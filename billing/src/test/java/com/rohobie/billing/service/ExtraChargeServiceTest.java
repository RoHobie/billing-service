package com.rohobie.billing.service;

import com.rohobie.billing.domain.Contract;
import com.rohobie.billing.domain.ContractType;
import com.rohobie.billing.domain.Trip;
import com.rohobie.billing.domain.Vehicle;
import com.rohobie.billing.domain.Vendor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ExtraChargeServiceTest {

    private final ExtraChargeService extraChargeService = new ExtraChargeService();

    @Test
    @DisplayName("Night charge + waiting (10 min @ ₹2/min) + toll ₹200 returns exact sum in paisa")
    void compute_nightWaitingToll_returnsExactSumPaisa() {
        Vendor vendor = new Vendor(1L, "Test Vendor", "test@vendor.com");
        Vehicle vehicle = new Vehicle(1L, "KA-01-AB-1234", "Sedan", vendor);

        // Night charge = ₹5 (500p), Waiting rate = ₹2/min (200p/min)
        Contract contract = new Contract(1L, vehicle, vendor, ContractType.PER_KM,
                0L, 0, LocalDate.of(2026, 1, 1), 500L, 200L);

        // Trip with night charge = true, waitingMinutes = 10, tollAmountPaisa = 20000L (₹200)
        Trip trip = new Trip(1L, vehicle, LocalDateTime.now(), LocalDateTime.now().plusHours(2),
                30, false, true, 10, 20000L);

        long extras = extraChargeService.compute(trip, contract);

        // 500p (night) + (10 * 200p = 2000p) + 20000p (toll) = 22500p
        assertThat(extras).isEqualTo(22500L);
    }

    @Test
    @DisplayName("Trip without extra charges returns 0 paisa")
    void compute_noExtras_returnsZero() {
        Vendor vendor = new Vendor(1L, "Test Vendor", "test@vendor.com");
        Vehicle vehicle = new Vehicle(1L, "KA-01-AB-1234", "Sedan", vendor);
        Contract contract = new Contract(1L, vehicle, vendor, ContractType.PER_KM,
                0L, 0, LocalDate.of(2026, 1, 1), 500L, 200L);

        Trip trip = new Trip(1L, vehicle, LocalDateTime.now(), LocalDateTime.now().plusHours(1),
                20, false, false, 0, 0L);

        long extras = extraChargeService.compute(trip, contract);
        assertThat(extras).isEqualTo(0L);
    }
}