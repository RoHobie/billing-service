package com.rohobie.billing.service;

import com.rohobie.billing.domain.Contract;
import com.rohobie.billing.domain.ContractType;
import com.rohobie.billing.domain.Trip;
import com.rohobie.billing.domain.Vehicle;
import com.rohobie.billing.domain.Vendor;
import com.rohobie.billing.dto.TripFareResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TripBillingServiceTest {

    @Mock
    private ContractLookupService contractLookupService;

    @Mock
    private SlabComputationService slabComputationService;

    @Mock
    private ExtraChargeService extraChargeService;

    @InjectMocks
    private TripBillingService tripBillingService;

    @Test
    @DisplayName("computeTripFare coordinates contract lookup, base slab, extra charges and builds note")
    void computeTripFare_standardTrip_returnsResultWithNote() {
        Vendor vendor = new Vendor(1L, "Vendor", "v@v.com");
        Vehicle vehicle = new Vehicle(1L, "KA-01-AB-1234", "Sedan", vendor);
        Contract contract = new Contract(1L, vehicle, vendor, ContractType.PER_KM,
                0L, 0, LocalDate.of(2026, 1, 1), 500L, 200L);

        LocalDateTime start = LocalDateTime.of(2026, 1, 10, 10, 0);
        LocalDateTime end = LocalDateTime.of(2026, 1, 10, 12, 0);
        Trip trip = new Trip(1L, vehicle, start, end, 100, false, true, 5, 2000L);

        when(contractLookupService.findActiveContract(1L, LocalDate.of(2026, 1, 10)))
                .thenReturn(contract);
        when(slabComputationService.compute(trip, contract)).thenReturn(500000L);
        when(slabComputationService.buildSlabBreakdownNote(trip, contract))
                .thenReturn("PER_KM: 100km@5000p = 500000p");
        when(extraChargeService.compute(trip, contract)).thenReturn(3500L);
        when(extraChargeService.buildExtraChargesNote(trip, contract))
                .thenReturn("night:500p | waiting:1000p | toll:2000p");

        TripFareResult result = tripBillingService.computeTripFare(trip);

        assertThat(result.basePaisa()).isEqualTo(500000L);
        assertThat(result.extraChargesPaisa()).isEqualTo(3500L);
        assertThat(result.totalPaisa()).isEqualTo(503500L);
        assertThat(result.computationNote())
                .isEqualTo("PER_KM: 100km@5000p = 500000p | night:500p | waiting:1000p | toll:2000p");
    }

    @Test
    @DisplayName("Dead-leg trip gets 0 base fare with DEAD_LEG note")
    void computeTripFare_deadLegTrip_returnsZeroBaseFare() {
        Vendor vendor = new Vendor(1L, "Vendor", "v@v.com");
        Vehicle vehicle = new Vehicle(1L, "KA-01-AB-1234", "Sedan", vendor);
        Contract contract = new Contract(1L, vehicle, vendor, ContractType.PER_KM,
                0L, 0, LocalDate.of(2026, 1, 1), 0L, 0L);

        LocalDateTime start = LocalDateTime.of(2026, 1, 10, 10, 0);
        LocalDateTime end = LocalDateTime.of(2026, 1, 10, 11, 0);
        // dead-leg = true
        Trip trip = new Trip(1L, vehicle, start, end, 50, true, false, 0, 0L);

        when(contractLookupService.findActiveContract(1L, LocalDate.of(2026, 1, 10)))
                .thenReturn(contract);
        when(extraChargeService.compute(trip, contract)).thenReturn(0L);
        when(extraChargeService.buildExtraChargesNote(trip, contract)).thenReturn("");

        TripFareResult result = tripBillingService.computeTripFare(trip);

        assertThat(result.basePaisa()).isEqualTo(0L);
        assertThat(result.extraChargesPaisa()).isEqualTo(0L);
        assertThat(result.totalPaisa()).isEqualTo(0L);
        assertThat(result.computationNote()).isEqualTo("DEAD_LEG: base = 0p");
    }
}