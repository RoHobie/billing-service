package com.rohobie.billing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rohobie.billing.domain.Contract;
import com.rohobie.billing.domain.ContractSlab;
import com.rohobie.billing.domain.ContractType;
import com.rohobie.billing.domain.Trip;
import com.rohobie.billing.domain.Vehicle;
import com.rohobie.billing.domain.Vendor;
import com.rohobie.billing.dto.request.ContractRequest;
import com.rohobie.billing.dto.request.ContractSlabRequest;
import com.rohobie.billing.dto.request.TripRequest;
import com.rohobie.billing.dto.request.VehicleRequest;
import com.rohobie.billing.dto.request.VendorRequest;
import com.rohobie.billing.repository.ContractRepository;
import com.rohobie.billing.repository.ContractSlabRepository;
import com.rohobie.billing.repository.TripRepository;
import com.rohobie.billing.repository.VehicleRepository;
import com.rohobie.billing.repository.VendorRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class EntityCrudTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private ContractSlabRepository contractSlabRepository;

    @Autowired
    private TripRepository tripRepository;

    @Test
    @DisplayName("Direct entity repository save and retrieve asserts field equality")
    void testDirectEntityPersistence() {
        Vendor vendor = vendorRepository.save(new Vendor("Direct Vendor", "direct@vendor.com"));
        assertThat(vendor.getId()).isNotNull();

        Vendor retrievedVendor = vendorRepository.findById(vendor.getId()).orElseThrow();
        assertThat(retrievedVendor.getName()).isEqualTo("Direct Vendor");
        assertThat(retrievedVendor.getContactEmail()).isEqualTo("direct@vendor.com");

        Vehicle vehicle = vehicleRepository.save(new Vehicle("KA-05-ZZ-9999", "Sedan", retrievedVendor));
        assertThat(vehicle.getId()).isNotNull();
        assertThat(vehicle.getRegistrationNumber()).isEqualTo("KA-05-ZZ-9999");
        assertThat(vehicle.getVendor().getName()).isEqualTo("Direct Vendor");

        Contract contract = contractRepository.save(new Contract(
                null, vehicle, retrievedVendor, ContractType.FIXED_MONTHLY,
                3000000L, 100, LocalDate.of(2026, 1, 1), 500L, 200L
        ));
        assertThat(contract.getId()).isNotNull();
        assertThat(contract.getBaseAmountPaisa()).isEqualTo(3000000L);
        assertThat(contract.getEffectiveFrom()).isEqualTo(LocalDate.of(2026, 1, 1));

        ContractSlab slab = contractSlabRepository.save(new ContractSlab(contract, 0, 100, 1200L));
        assertThat(slab.getId()).isNotNull();
        assertThat(slab.getFromKm()).isEqualTo(0);
        assertThat(slab.getToKm()).isEqualTo(100);
        assertThat(slab.getRatePerKmPaisa()).isEqualTo(1200L);

        Trip trip = tripRepository.save(new Trip(
                vehicle,
                LocalDateTime.of(2026, 1, 10, 10, 0),
                LocalDateTime.of(2026, 1, 10, 12, 0),
                50, false, false, 0, 15000L
        ));
        assertThat(trip.getId()).isNotNull();
        assertThat(trip.getDistanceKm()).isEqualTo(50);
        assertThat(trip.getTollAmountPaisa()).isEqualTo(15000L);
    }

    @Test
    @DisplayName("REST API CRUD workflow: vendor, vehicle, contract, slab, trip, and 404 handling")
    void testRestApiEndpoints() throws Exception {
        // 1. POST /api/vendors -> 201
        VendorRequest vendorRequest = new VendorRequest("FastFleet Rentals", "info@fastfleet.com");
        MvcResult vendorResult = mockMvc.perform(post("/api/vendors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vendorRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.name").value("FastFleet Rentals"))
                .andReturn();

        JsonNode vendorNode = objectMapper.readTree(vendorResult.getResponse().getContentAsString());
        long vendorId = vendorNode.path("data").path("id").asLong();

        // GET /api/vendors/{id} -> 200
        mockMvc.perform(get("/api/vendors/" + vendorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("FastFleet Rentals"));

        // 2. POST /api/vehicles with valid vendorId -> 201, returns vendor name
        VehicleRequest vehicleRequest = new VehicleRequest("KA-01-CRUD-1234", "SUV", vendorId);
        MvcResult vehicleResult = mockMvc.perform(post("/api/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vehicleRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.registrationNumber").value("KA-01-CRUD-1234"))
                .andExpect(jsonPath("$.data.vendorName").value("FastFleet Rentals"))
                .andReturn();

        long vehicleId = objectMapper.readTree(vehicleResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // GET /api/vehicles/{id} -> 200
        mockMvc.perform(get("/api/vehicles/" + vehicleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.registrationNumber").value("KA-01-CRUD-1234"));

        // 3. POST /api/contracts with FIXED_MONTHLY and effectiveFrom: 2026-01-01 -> 201
        ContractRequest contractRequest = new ContractRequest(
                vehicleId, vendorId, ContractType.FIXED_MONTHLY,
                3000000L, 0, LocalDate.of(2026, 1, 1), 0L, 0L
        );
        MvcResult contractResult = mockMvc.perform(post("/api/contracts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(contractRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.contractType").value("FIXED_MONTHLY"))
                .andExpect(jsonPath("$.data.baseAmountPaisa").value(3000000L))
                .andReturn();

        long contractId = objectMapper.readTree(contractResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // 4. POST /api/contracts/{id}/slabs -> 201, slab persists with correct FK
        ContractSlabRequest slabRequest = new ContractSlabRequest(0, 100, 1200L);
        mockMvc.perform(post("/api/contracts/" + contractId + "/slabs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(slabRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.fromKm").value(0))
                .andExpect(jsonPath("$.data.toKm").value(100))
                .andExpect(jsonPath("$.data.ratePerKmPaisa").value(1200L));

        // GET /api/contracts/{id}/slabs -> 200
        mockMvc.perform(get("/api/contracts/" + contractId + "/slabs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].ratePerKmPaisa").value(1200L));

        // 5. POST /api/trips -> 201 with vehicle FK
        TripRequest tripRequest = new TripRequest(
                vehicleId,
                LocalDateTime.of(2026, 1, 5, 9, 0),
                LocalDateTime.of(2026, 1, 5, 11, 0),
                45,
                false,
                false,
                10,
                5000L
        );
        MvcResult tripResult = mockMvc.perform(post("/api/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tripRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.distanceKm").value(45))
                .andExpect(jsonPath("$.data.vehicleRegistrationNumber").value("KA-01-CRUD-1234"))
                .andReturn();

        long tripId = objectMapper.readTree(tripResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // GET /api/trips/{id} -> 200
        mockMvc.perform(get("/api/trips/" + tripId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.distanceKm").value(45));

        // 6. Unknown id -> 404 with error body
        mockMvc.perform(get("/api/vendors/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Vendor with id 99999 not found"));

        mockMvc.perform(get("/api/contracts/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Contract with id 99999 not found"));
    }
}