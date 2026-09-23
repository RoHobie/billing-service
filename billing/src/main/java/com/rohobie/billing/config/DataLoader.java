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
 * Development data seeding component that populates demonstration vendors, vehicles,
 * contracts with mid-month rate revisions, slabs, and January 2026 trips.
 *
 * Note: Dev-only scaffolding; replaced by Flyway/Liquibase migrations in production.
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

    @Override
    @Transactional
    public void run(String... args) {
        if (vendorRepository.count() > 0) {
            log.info("Database already seeded; skipping DataLoader initialization");
            return;
        }

        log.info("Seeding demonstration fleet billing dataset...");

        // 1. Vendor
        Vendor vendor = vendorRepository.save(new Vendor(null, "FastFleet Rentals", "info@fastfleet.com"));

        // 2. Vehicles
        Vehicle vehicle1 = vehicleRepository.save(new Vehicle(null, "KA-01-HH-1234", "Sedan", vendor));
        Vehicle vehicle2 = vehicleRepository.save(new Vehicle(null, "KA-02-AB-5678", "SUV", vendor));

        // 3. Contract 1: PER_KM for Vehicle 1 (Effective Jan 1, 2026)
        Contract v1ContractJan1 = contractRepository.save(new Contract(
                null, vehicle1, vendor, ContractType.PER_KM, 0L, 0,
                LocalDate.of(2026, 1, 1), 500L, 200L
        ));

        // Slabs for Contract 1: 0-100 @ 1200p (₹12), 101-300 @ 1000p (₹10), 301+ @ 800p (₹8)
        contractSlabRepository.save(new ContractSlab(null, v1ContractJan1, 0, 100, 1200L));
        contractSlabRepository.save(new ContractSlab(null, v1ContractJan1, 101, 300, 1000L));
        contractSlabRepository.save(new ContractSlab(null, v1ContractJan1, 301, null, 800L));

        // 4. Contract 2: FIXED_MONTHLY for Vehicle 2 (Effective Jan 1, 2026) - ₹30,000 = 3,000,000 paisa
        contractRepository.save(new Contract(
                null, vehicle2, vendor, ContractType.FIXED_MONTHLY, 3000000L, 0,
                LocalDate.of(2026, 1, 1), 0L, 0L
        ));

        // 5. Contract 3: Updated PER_KM for Vehicle 1 (Effective Jan 15, 2026 - Higher Rates)
        Contract v1ContractJan15 = contractRepository.save(new Contract(
                null, vehicle1, vendor, ContractType.PER_KM, 0L, 0,
                LocalDate.of(2026, 1, 15), 600L, 250L
        ));

        // Slabs for Contract 3: 0-100 @ 1400p (₹14), 101-300 @ 1200p (₹12), 301+ @ 1000p (₹10)
        contractSlabRepository.save(new ContractSlab(null, v1ContractJan15, 0, 100, 1400L));
        contractSlabRepository.save(new ContractSlab(null, v1ContractJan15, 101, 300, 1200L));
        contractSlabRepository.save(new ContractSlab(null, v1ContractJan15, 301, null, 1000L));

        // 6. Trips for Vehicle 1 (10 trips across January 2026, split across Jan 1 and Jan 15 contracts)
        // Trips before Jan 15 (use Jan 1 rates):
        tripRepository.save(new Trip(null, vehicle1, LocalDateTime.of(2026, 1, 2, 8, 0), LocalDateTime.of(2026, 1, 2, 10, 0), 80, false, false, 0, 0L));
        tripRepository.save(new Trip(null, vehicle1, LocalDateTime.of(2026, 1, 4, 9, 0), LocalDateTime.of(2026, 1, 4, 12, 0), 150, false, false, 10, 5000L));
        tripRepository.save(new Trip(null, vehicle1, LocalDateTime.of(2026, 1, 7, 22, 0), LocalDateTime.of(2026, 1, 7, 23, 30), 60, false, true, 0, 0L));
        tripRepository.save(new Trip(null, vehicle1, LocalDateTime.of(2026, 1, 10, 14, 0), LocalDateTime.of(2026, 1, 10, 16, 0), 90, false, false, 5, 2000L));
        tripRepository.save(new Trip(null, vehicle1, LocalDateTime.of(2026, 1, 14, 11, 0), LocalDateTime.of(2026, 1, 14, 12, 0), 40, false, false, 0, 0L));

        // Trips on or after Jan 15 (use Jan 15 rates):
        tripRepository.save(new Trip(null, vehicle1, LocalDateTime.of(2026, 1, 16, 8, 0), LocalDateTime.of(2026, 1, 16, 11, 0), 120, false, false, 0, 0L));
        tripRepository.save(new Trip(null, vehicle1, LocalDateTime.of(2026, 1, 18, 10, 0), LocalDateTime.of(2026, 1, 18, 14, 0), 220, false, false, 15, 8000L));
        tripRepository.save(new Trip(null, vehicle1, LocalDateTime.of(2026, 1, 22, 23, 0), LocalDateTime.of(2026, 1, 23, 1, 0), 75, false, true, 0, 0L));
        tripRepository.save(new Trip(null, vehicle1, LocalDateTime.of(2026, 1, 25, 9, 0), LocalDateTime.of(2026, 1, 25, 10, 30), 50, false, false, 0, 0L));
        tripRepository.save(new Trip(null, vehicle1, LocalDateTime.of(2026, 1, 28, 15, 0), LocalDateTime.of(2026, 1, 28, 20, 0), 350, false, false, 20, 12000L));

        // 7. Trips for Vehicle 2 (10 trips across January 2026 under FIXED_MONTHLY contract)
        tripRepository.save(new Trip(null, vehicle2, LocalDateTime.of(2026, 1, 3, 9, 0), LocalDateTime.of(2026, 1, 3, 11, 0), 50, false, false, 0, 0L));
        tripRepository.save(new Trip(null, vehicle2, LocalDateTime.of(2026, 1, 5, 8, 0), LocalDateTime.of(2026, 1, 5, 11, 0), 120, false, false, 0, 0L));
        tripRepository.save(new Trip(null, vehicle2, LocalDateTime.of(2026, 1, 8, 10, 0), LocalDateTime.of(2026, 1, 8, 11, 0), 30, false, false, 0, 0L));
        tripRepository.save(new Trip(null, vehicle2, LocalDateTime.of(2026, 1, 11, 13, 0), LocalDateTime.of(2026, 1, 11, 16, 0), 180, false, false, 0, 0L));
        // Dead-leg trip: empty repositioning run
        tripRepository.save(new Trip(null, vehicle2, LocalDateTime.of(2026, 1, 13, 7, 0), LocalDateTime.of(2026, 1, 13, 8, 0), 45, true, false, 0, 0L));
        tripRepository.save(new Trip(null, vehicle2, LocalDateTime.of(2026, 1, 17, 9, 0), LocalDateTime.of(2026, 1, 17, 12, 0), 110, false, false, 0, 0L));
        tripRepository.save(new Trip(null, vehicle2, LocalDateTime.of(2026, 1, 20, 14, 0), LocalDateTime.of(2026, 1, 20, 17, 0), 95, false, false, 0, 0L));
        tripRepository.save(new Trip(null, vehicle2, LocalDateTime.of(2026, 1, 23, 8, 0), LocalDateTime.of(2026, 1, 23, 13, 0), 250, false, false, 0, 0L));
        tripRepository.save(new Trip(null, vehicle2, LocalDateTime.of(2026, 1, 27, 10, 0), LocalDateTime.of(2026, 1, 27, 12, 0), 80, false, false, 0, 0L));
        tripRepository.save(new Trip(null, vehicle2, LocalDateTime.of(2026, 1, 30, 15, 0), LocalDateTime.of(2026, 1, 30, 18, 0), 140, false, false, 0, 0L));

        log.info("Demonstration dataset seeded successfully: 1 vendor, 2 vehicles, 3 contracts, 20 trips");
    }
}
