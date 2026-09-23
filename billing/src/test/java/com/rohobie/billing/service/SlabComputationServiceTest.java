package com.rohobie.billing.service;

import com.rohobie.billing.domain.Contract;
import com.rohobie.billing.domain.ContractSlab;
import com.rohobie.billing.domain.ContractType;
import com.rohobie.billing.domain.Trip;
import com.rohobie.billing.domain.Vehicle;
import com.rohobie.billing.domain.Vendor;
import com.rohobie.billing.repository.ContractSlabRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SlabComputationServiceTest {

    @Mock
    private ContractSlabRepository contractSlabRepository;

    @InjectMocks
    private SlabComputationService slabComputationService;

    @Test
    @DisplayName("150 km trip under slabs 0-100 @ ₹50 and 101-200 @ ₹45 returns exactly 725,000 paisa (₹7,250)")
    void compute_perKmContract_150km_returnsExactPaisa() {
        Vendor vendor = new Vendor(1L, "Test Vendor", "test@vendor.com");
        Vehicle vehicle = new Vehicle(1L, "KA-01-AB-1234", "Sedan", vendor);
        Contract contract = new Contract(1L, vehicle, vendor, ContractType.PER_KM,
                0L, 0, LocalDate.of(2026, 1, 1), 0L, 0L);

        // ₹50 = 5000 paisa, ₹45 = 4500 paisa
        ContractSlab slab1 = new ContractSlab(1L, contract, 0, 100, 5000L);
        ContractSlab slab2 = new ContractSlab(2L, contract, 101, 200, 4500L);
        when(contractSlabRepository.findByContractIdOrderByFromKmAsc(1L))
                .thenReturn(List.of(slab1, slab2));

        Trip trip = new Trip(1L, vehicle, LocalDateTime.now(), LocalDateTime.now().plusHours(3),
                150, false, false, 0, 0L);

        long fare = slabComputationService.compute(trip, contract);
        // (100 * 5000) + (50 * 4500) = 500000 + 225000 = 725000 paisa (₹7,250)
        assertThat(fare).isEqualTo(725000L);
    }

    @Test
    @DisplayName("PER_TRIP contract returns flat base amount regardless of distance")
    void compute_perTripContract_returnsFlatBaseAmount() {
        Vendor vendor = new Vendor(1L, "Test Vendor", "test@vendor.com");
        Vehicle vehicle = new Vehicle(1L, "KA-01-AB-1234", "Sedan", vendor);
        Contract contract = new Contract(1L, vehicle, vendor, ContractType.PER_TRIP,
                50000L, 0, LocalDate.of(2026, 1, 1), 0L, 0L);

        Trip trip = new Trip(1L, vehicle, LocalDateTime.now(), LocalDateTime.now().plusHours(1),
                120, false, false, 0, 0L);

        long fare = slabComputationService.compute(trip, contract);
        assertThat(fare).isEqualTo(50000L);
    }

    @Test
    @DisplayName("FIXED_MONTHLY contract returns 0 paisa base fare from slab service")
    void compute_fixedMonthlyContract_returnsZeroPaisa() {
        Vendor vendor = new Vendor(1L, "Test Vendor", "test@vendor.com");
        Vehicle vehicle = new Vehicle(1L, "KA-01-AB-1234", "Sedan", vendor);
        Contract contract = new Contract(1L, vehicle, vendor, ContractType.FIXED_MONTHLY,
                3000000L, 0, LocalDate.of(2026, 1, 1), 0L, 0L);

        Trip trip = new Trip(1L, vehicle, LocalDateTime.now(), LocalDateTime.now().plusHours(1),
                80, false, false, 0, 0L);

        long fare = slabComputationService.compute(trip, contract);
        assertThat(fare).isEqualTo(0L);
    }

    @Test
    @DisplayName("Free km reduce billable distance prior to slab walk")
    void compute_perKmWithFreeKm_reducesDistance() {
        Vendor vendor = new Vendor(1L, "Test Vendor", "test@vendor.com");
        Vehicle vehicle = new Vehicle(1L, "KA-01-AB-1234", "Sedan", vendor);
        // 20 free km
        Contract contract = new Contract(1L, vehicle, vendor, ContractType.PER_KM,
                0L, 20, LocalDate.of(2026, 1, 1), 0L, 0L);

        ContractSlab slab1 = new ContractSlab(1L, contract, 0, 100, 1000L);
        when(contractSlabRepository.findByContractIdOrderByFromKmAsc(1L))
                .thenReturn(List.of(slab1));

        // 50 km trip - 20 free km = 30 billable km
        Trip trip = new Trip(1L, vehicle, LocalDateTime.now(), LocalDateTime.now().plusHours(1),
                50, false, false, 0, 0L);

        long fare = slabComputationService.compute(trip, contract);
        assertThat(fare).isEqualTo(30 * 1000L);
    }
}