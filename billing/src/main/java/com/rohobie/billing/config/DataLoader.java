package com.rohobie.billing.config;

import com.rohobie.billing.domain.Contract;
import com.rohobie.billing.domain.ContractSlab;
import com.rohobie.billing.domain.ContractType;
import com.rohobie.billing.domain.Trip;
import com.rohobie.billing.domain.Vehicle;
import com.rohobie.billing.domain.Vendor;
import com.rohobie.billing.repository.ContractRepository;
import com.rohobie.billing.repository.ContractSlabRepository;
import com.rohobie.billing.repository.TripRepository;
import com.rohobie.billing.repository.VehicleRepository;
import com.rohobie.billing.repository.VendorRepository;
import com.rohobie.billing.service.BillingRunService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DataLoader
 *
 * Populates demonstration vendors, vehicles, contracts with mid-month rate revisions,
 * slabs, and trips across multiple billing periods.
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class DataLoader implements CommandLineRunner {

    private final VendorRepository vendorRepository;
    private final VehicleRepository vehicleRepository;
    private final ContractRepository contractRepository;
    private final ContractSlabRepository contractSlabRepository;
    private final TripRepository tripRepository;
    private final BillingRunService billingRunService;

    @Override
    @Transactional
    public void run(String... args) {
        if (vendorRepository.count() > 0) {
            log.info("Database already seeded; skipping DataLoader initialization");
            return;
        }

        log.info("Seeding expanded demonstration fleet billing dataset...");

        // 1. Vendors
        Vendor vendor1 = vendorRepository.save(new Vendor(null, "FastFleet Rentals", "info@fastfleet.com"));
        Vendor vendor2 = vendorRepository.save(new Vendor(null, "Metro Mobility Logistics", "support@metromobility.com"));
        Vendor vendor3 = vendorRepository.save(new Vendor(null, "Apex Commercial Fleets", "billing@apexfleet.in"));
        Vendor vendor4 = vendorRepository.save(new Vendor(null, "CityLine Transit Partners", "partner@citylinetransit.com"));

        // 2. Vehicles
        Vehicle vehicle1 = vehicleRepository.save(new Vehicle(null, "KA-01-HH-1234", "Sedan", vendor1));
        Vehicle vehicle2 = vehicleRepository.save(new Vehicle(null, "KA-02-AB-5678", "SUV", vendor1));
        Vehicle vehicle3 = vehicleRepository.save(new Vehicle(null, "KA-03-CD-2468", "EV Cargo Van", vendor2));
        Vehicle vehicle4 = vehicleRepository.save(new Vehicle(null, "DL-01-EF-1357", "Executive Sedan", vendor2));
        Vehicle vehicle5 = vehicleRepository.save(new Vehicle(null, "MH-02-JK-9876", "Luxury SUV", vendor3));
        Vehicle vehicle6 = vehicleRepository.save(new Vehicle(null, "TN-09-LM-5432", "Urban Hatchback", vendor4));
        Vehicle vehicle7 = vehicleRepository.save(new Vehicle(null, "KA-05-MN-8765", "Passenger Van", vendor3));

        // 3. Contracts for Vehicle 1 (Preserved for integration test compatibility)
        Contract v1ContractJan1 = contractRepository.save(new Contract(
                null, vehicle1, vendor1, ContractType.PER_KM, 0L, 0,
                LocalDate.of(2026, 1, 1), 500L, 200L
        ));
        contractSlabRepository.save(new ContractSlab(null, v1ContractJan1, 0, 100, 1200L));
        contractSlabRepository.save(new ContractSlab(null, v1ContractJan1, 101, 300, 1000L));
        contractSlabRepository.save(new ContractSlab(null, v1ContractJan1, 301, null, 800L));

        Contract v1ContractJan15 = contractRepository.save(new Contract(
                null, vehicle1, vendor1, ContractType.PER_KM, 0L, 0,
                LocalDate.of(2026, 1, 15), 600L, 250L
        ));
        contractSlabRepository.save(new ContractSlab(null, v1ContractJan15, 0, 100, 1400L));
        contractSlabRepository.save(new ContractSlab(null, v1ContractJan15, 101, 300, 1200L));
        contractSlabRepository.save(new ContractSlab(null, v1ContractJan15, 301, null, 1000L));

        // 4. Contract for Vehicle 2 (FIXED_MONTHLY ₹30,000 = 3,000,000 paisa)
        contractRepository.save(new Contract(
                null, vehicle2, vendor1, ContractType.FIXED_MONTHLY, 3000000L, 0,
                LocalDate.of(2026, 1, 1), 0L, 0L
        ));

        // 5. Contract for Vehicle 3 (FIXED_MONTHLY ₹45,000 = 4,500,000 paisa)
        contractRepository.save(new Contract(
                null, vehicle3, vendor2, ContractType.FIXED_MONTHLY, 4500000L, 0,
                LocalDate.of(2026, 1, 1), 0L, 0L
        ));

        // 6. Contract for Vehicle 4 (PER_KM with tiered slabs)
        Contract v4Contract = contractRepository.save(new Contract(
                null, vehicle4, vendor2, ContractType.PER_KM, 0L, 0,
                LocalDate.of(2026, 1, 1), 500L, 150L
        ));
        contractSlabRepository.save(new ContractSlab(null, v4Contract, 0, 50, 1500L));
        contractSlabRepository.save(new ContractSlab(null, v4Contract, 51, 200, 1200L));
        contractSlabRepository.save(new ContractSlab(null, v4Contract, 201, null, 900L));

        // 7. Contract for Vehicle 5 (FIXED_MONTHLY ₹60,000 = 6,000,000 paisa)
        contractRepository.save(new Contract(
                null, vehicle5, vendor3, ContractType.FIXED_MONTHLY, 6000000L, 0,
                LocalDate.of(2026, 1, 1), 0L, 0L
        ));

        // 8. Contract for Vehicle 6 (PER_KM)
        Contract v6Contract = contractRepository.save(new Contract(
                null, vehicle6, vendor4, ContractType.PER_KM, 0L, 0,
                LocalDate.of(2026, 1, 1), 400L, 100L
        ));
        contractSlabRepository.save(new ContractSlab(null, v6Contract, 0, 100, 1100L));
        contractSlabRepository.save(new ContractSlab(null, v6Contract, 101, null, 850L));

        // 9. Contract for Vehicle 7 (PER_KM)
        Contract v7Contract = contractRepository.save(new Contract(
                null, vehicle7, vendor3, ContractType.PER_KM, 0L, 0,
                LocalDate.of(2026, 1, 1), 600L, 200L
        ));
        contractSlabRepository.save(new ContractSlab(null, v7Contract, 0, 80, 1300L));
        contractSlabRepository.save(new ContractSlab(null, v7Contract, 81, 250, 1050L));
        contractSlabRepository.save(new ContractSlab(null, v7Contract, 251, null, 800L));

        // 10. Trips for Vehicle 1 (Jan 2026 - Exact 10 trips preserved for test compatibility)
        tripRepository.save(new Trip(null, vehicle1, LocalDateTime.of(2026, 1, 2, 8, 0), LocalDateTime.of(2026, 1, 2, 10, 0), 80, false, false, 0, 0L));
        tripRepository.save(new Trip(null, vehicle1, LocalDateTime.of(2026, 1, 4, 9, 0), LocalDateTime.of(2026, 1, 4, 12, 0), 150, false, false, 10, 5000L));
        tripRepository.save(new Trip(null, vehicle1, LocalDateTime.of(2026, 1, 7, 22, 0), LocalDateTime.of(2026, 1, 7, 23, 30), 60, false, true, 0, 0L));
        tripRepository.save(new Trip(null, vehicle1, LocalDateTime.of(2026, 1, 10, 14, 0), LocalDateTime.of(2026, 1, 10, 16, 0), 90, false, false, 5, 2000L));
        tripRepository.save(new Trip(null, vehicle1, LocalDateTime.of(2026, 1, 14, 11, 0), LocalDateTime.of(2026, 1, 14, 12, 0), 40, false, false, 0, 0L));
        tripRepository.save(new Trip(null, vehicle1, LocalDateTime.of(2026, 1, 16, 8, 0), LocalDateTime.of(2026, 1, 16, 11, 0), 120, false, false, 0, 0L));
        tripRepository.save(new Trip(null, vehicle1, LocalDateTime.of(2026, 1, 18, 10, 0), LocalDateTime.of(2026, 1, 18, 14, 0), 220, false, false, 15, 8000L));
        tripRepository.save(new Trip(null, vehicle1, LocalDateTime.of(2026, 1, 22, 23, 0), LocalDateTime.of(2026, 1, 23, 1, 0), 75, false, true, 0, 0L));
        tripRepository.save(new Trip(null, vehicle1, LocalDateTime.of(2026, 1, 25, 9, 0), LocalDateTime.of(2026, 1, 25, 10, 30), 50, false, false, 0, 0L));
        tripRepository.save(new Trip(null, vehicle1, LocalDateTime.of(2026, 1, 28, 15, 0), LocalDateTime.of(2026, 1, 28, 20, 0), 350, false, false, 20, 12000L));

        // 11. Trips for Vehicle 2 (Jan 2026 - Exact 10 trips preserved for test compatibility)
        tripRepository.save(new Trip(null, vehicle2, LocalDateTime.of(2026, 1, 3, 9, 0), LocalDateTime.of(2026, 1, 3, 11, 0), 50, false, false, 0, 0L));
        tripRepository.save(new Trip(null, vehicle2, LocalDateTime.of(2026, 1, 5, 8, 0), LocalDateTime.of(2026, 1, 5, 11, 0), 120, false, false, 0, 0L));
        tripRepository.save(new Trip(null, vehicle2, LocalDateTime.of(2026, 1, 8, 10, 0), LocalDateTime.of(2026, 1, 8, 11, 0), 30, false, false, 0, 0L));
        tripRepository.save(new Trip(null, vehicle2, LocalDateTime.of(2026, 1, 11, 13, 0), LocalDateTime.of(2026, 1, 11, 16, 0), 180, false, false, 0, 0L));
        tripRepository.save(new Trip(null, vehicle2, LocalDateTime.of(2026, 1, 13, 7, 0), LocalDateTime.of(2026, 1, 13, 8, 0), 45, true, false, 0, 0L));
        tripRepository.save(new Trip(null, vehicle2, LocalDateTime.of(2026, 1, 17, 9, 0), LocalDateTime.of(2026, 1, 17, 12, 0), 110, false, false, 0, 0L));
        tripRepository.save(new Trip(null, vehicle2, LocalDateTime.of(2026, 1, 20, 14, 0), LocalDateTime.of(2026, 1, 20, 17, 0), 95, false, false, 0, 0L));
        tripRepository.save(new Trip(null, vehicle2, LocalDateTime.of(2026, 1, 23, 8, 0), LocalDateTime.of(2026, 1, 23, 13, 0), 250, false, false, 0, 0L));
        tripRepository.save(new Trip(null, vehicle2, LocalDateTime.of(2026, 1, 27, 10, 0), LocalDateTime.of(2026, 1, 27, 12, 0), 80, false, false, 0, 0L));
        tripRepository.save(new Trip(null, vehicle2, LocalDateTime.of(2026, 1, 30, 15, 0), LocalDateTime.of(2026, 1, 30, 18, 0), 140, false, false, 0, 0L));

        // 12. Additional Trips for Vehicle 3 (Jan 2026)
        tripRepository.save(new Trip(null, vehicle3, LocalDateTime.of(2026, 1, 2, 9, 0), LocalDateTime.of(2026, 1, 2, 11, 0), 65, false, false, 0, 0L));
        tripRepository.save(new Trip(null, vehicle3, LocalDateTime.of(2026, 1, 5, 10, 0), LocalDateTime.of(2026, 1, 5, 12, 30), 85, false, false, 0, 0L));
        tripRepository.save(new Trip(null, vehicle3, LocalDateTime.of(2026, 1, 8, 8, 0), LocalDateTime.of(2026, 1, 8, 10, 0), 45, false, false, 0, 0L));
        tripRepository.save(new Trip(null, vehicle3, LocalDateTime.of(2026, 1, 12, 14, 0), LocalDateTime.of(2026, 1, 12, 17, 0), 130, false, false, 0, 0L));
        tripRepository.save(new Trip(null, vehicle3, LocalDateTime.of(2026, 1, 16, 11, 0), LocalDateTime.of(2026, 1, 16, 13, 0), 75, false, false, 0, 0L));
        tripRepository.save(new Trip(null, vehicle3, LocalDateTime.of(2026, 1, 19, 9, 0), LocalDateTime.of(2026, 1, 19, 12, 0), 110, false, false, 0, 0L));
        tripRepository.save(new Trip(null, vehicle3, LocalDateTime.of(2026, 1, 22, 15, 0), LocalDateTime.of(2026, 1, 22, 17, 0), 90, false, false, 0, 0L));
        tripRepository.save(new Trip(null, vehicle3, LocalDateTime.of(2026, 1, 26, 8, 0), LocalDateTime.of(2026, 1, 26, 11, 30), 140, false, false, 0, 0L));
        tripRepository.save(new Trip(null, vehicle3, LocalDateTime.of(2026, 1, 29, 10, 0), LocalDateTime.of(2026, 1, 29, 12, 0), 60, false, false, 0, 0L));

        // 13. Additional Trips for Vehicle 4 (Jan 2026 - Per KM with surcharges)
        tripRepository.save(new Trip(null, vehicle4, LocalDateTime.of(2026, 1, 3, 7, 0), LocalDateTime.of(2026, 1, 3, 9, 0), 45, false, false, 0, 0L));
        tripRepository.save(new Trip(null, vehicle4, LocalDateTime.of(2026, 1, 6, 10, 0), LocalDateTime.of(2026, 1, 6, 13, 0), 95, false, false, 15, 4500L));
        tripRepository.save(new Trip(null, vehicle4, LocalDateTime.of(2026, 1, 9, 22, 30), LocalDateTime.of(2026, 1, 10, 0, 30), 70, false, true, 0, 0L));
        tripRepository.save(new Trip(null, vehicle4, LocalDateTime.of(2026, 1, 13, 8, 0), LocalDateTime.of(2026, 1, 13, 11, 0), 120, false, false, 20, 6000L));
        tripRepository.save(new Trip(null, vehicle4, LocalDateTime.of(2026, 1, 17, 14, 0), LocalDateTime.of(2026, 1, 17, 16, 0), 60, false, false, 0, 0L));
        tripRepository.save(new Trip(null, vehicle4, LocalDateTime.of(2026, 1, 21, 23, 0), LocalDateTime.of(2026, 1, 22, 1, 0), 85, false, true, 10, 3500L));
        tripRepository.save(new Trip(null, vehicle4, LocalDateTime.of(2026, 1, 25, 9, 0), LocalDateTime.of(2026, 1, 25, 14, 0), 210, false, false, 25, 9000L));
        tripRepository.save(new Trip(null, vehicle4, LocalDateTime.of(2026, 1, 28, 16, 0), LocalDateTime.of(2026, 1, 28, 18, 0), 55, false, false, 0, 0L));

        // 14. Additional Trips for Vehicle 7 (Jan 2026 - includes audit flag trip > 500km)
        tripRepository.save(new Trip(null, vehicle7, LocalDateTime.of(2026, 1, 4, 8, 0), LocalDateTime.of(2026, 1, 4, 11, 0), 75, false, false, 0, 0L));
        tripRepository.save(new Trip(null, vehicle7, LocalDateTime.of(2026, 1, 10, 9, 0), LocalDateTime.of(2026, 1, 10, 13, 0), 160, false, false, 15, 5000L));
        tripRepository.save(new Trip(null, vehicle7, LocalDateTime.of(2026, 1, 15, 23, 0), LocalDateTime.of(2026, 1, 16, 1, 0), 90, false, true, 0, 0L));
        tripRepository.save(new Trip(null, vehicle7, LocalDateTime.of(2026, 1, 20, 5, 0), LocalDateTime.of(2026, 1, 20, 22, 0), 520, false, false, 30, 15000L)); // Impossible distance audit flag
        tripRepository.save(new Trip(null, vehicle7, LocalDateTime.of(2026, 1, 27, 10, 0), LocalDateTime.of(2026, 1, 27, 13, 0), 110, false, false, 0, 0L));

        // 15. February 2026 Trips
        tripRepository.save(new Trip(null, vehicle1, LocalDateTime.of(2026, 2, 2, 9, 0), LocalDateTime.of(2026, 2, 2, 11, 0), 70, false, false, 0, 0L));
        tripRepository.save(new Trip(null, vehicle1, LocalDateTime.of(2026, 2, 5, 10, 0), LocalDateTime.of(2026, 2, 5, 13, 0), 130, false, false, 10, 4000L));
        tripRepository.save(new Trip(null, vehicle1, LocalDateTime.of(2026, 2, 8, 22, 0), LocalDateTime.of(2026, 2, 8, 23, 30), 55, false, true, 0, 0L));
        tripRepository.save(new Trip(null, vehicle2, LocalDateTime.of(2026, 2, 3, 8, 0), LocalDateTime.of(2026, 2, 3, 10, 0), 60, false, false, 0, 0L));
        tripRepository.save(new Trip(null, vehicle2, LocalDateTime.of(2026, 2, 7, 9, 0), LocalDateTime.of(2026, 2, 7, 12, 0), 105, false, false, 0, 0L));
        tripRepository.save(new Trip(null, vehicle4, LocalDateTime.of(2026, 2, 4, 8, 0), LocalDateTime.of(2026, 2, 4, 11, 0), 90, false, false, 10, 3000L));

        // 16. Pre-execute demonstration billing runs for Vehicle 4 and Vehicle 7
        billingRunService.runBilling(vehicle4.getId(), "2026-01");
        billingRunService.runBilling(vehicle7.getId(), "2026-01");

        log.info("Demonstration dataset seeded successfully: 4 vendors, 7 vehicles, 7 contracts, 50+ trips, 2 active runs");
    }
}
