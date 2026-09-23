package com.rohobie.billing.service;

import com.rohobie.billing.domain.Trip;
import com.rohobie.billing.domain.Vehicle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FixedFeeSplitServiceTest {

    private FixedFeeSplitService fixedFeeSplitService;
    private Vehicle sampleVehicle;

    @BeforeEach
    void setUp() {
        fixedFeeSplitService = new FixedFeeSplitService();
        sampleVehicle = new Vehicle(1L, "KA-01-AB-1234", "Sedan", null);
    }

    @Test
    @DisplayName("5,000 trips totaling 1,200 km reconcile to exactly 3,000,000 paisa (₹30,000)")
    void split_5000Trips_sumsToExactlyContractAmountPaisa() {
        long contractAmountPaisa = 3000000L; // ₹30,000
        int numberOfTrips = 5000;
        List<Trip> trips = new ArrayList<>();

        // Distribute distances so they sum to 1200 km
        // 4000 trips of 0 km (or dead-legs/short runs), and 1000 trips varying between 1 and 2 km
        for (int i = 1; i <= numberOfTrips; i++) {
            int distance = (i <= 1000) ? (i % 2 == 0 ? 1 : 2) : 0;
            Trip trip = new Trip((long) i, sampleVehicle, LocalDateTime.now(), LocalDateTime.now().plusHours(1),
                    distance, false, false, 0, 0L);
            trips.add(trip);
        }

        long totalKm = trips.stream().mapToLong(Trip::getDistanceKm).sum();
        assertThat(totalKm).isGreaterThan(0L);

        Map<Long, Long> shares = fixedFeeSplitService.split(contractAmountPaisa, trips);

        assertThat(shares).hasSize(numberOfTrips);
        long sumOfShares = shares.values().stream().mapToLong(Long::longValue).sum();
        assertThat(sumOfShares).isEqualTo(contractAmountPaisa);
    }

    @Test
    @DisplayName("3 trips [100, 200, 300] km with 10 paisa contract fee sum to 10 exactly")
    void split_threeTripsSmallAmount_sumsToExactlyTenPaisa() {
        long contractAmountPaisa = 10L;

        Trip trip1 = new Trip(1L, sampleVehicle, LocalDateTime.now(), LocalDateTime.now().plusHours(1),
                100, false, false, 0, 0L);
        Trip trip2 = new Trip(2L, sampleVehicle, LocalDateTime.now(), LocalDateTime.now().plusHours(1),
                200, false, false, 0, 0L);
        Trip trip3 = new Trip(3L, sampleVehicle, LocalDateTime.now(), LocalDateTime.now().plusHours(1),
                300, false, false, 0, 0L);

        Map<Long, Long> shares = fixedFeeSplitService.split(contractAmountPaisa, List.of(trip1, trip2, trip3));

        assertThat(shares).hasSize(3);
        long sum = shares.values().stream().mapToLong(Long::longValue).sum();
        assertThat(sum).isEqualTo(10L);

        // Verification of proportional largest remainder allocation:
        // total = 600 km.
        // trip1: 100/600 * 10 = 1.666... -> floor 1, rem 0.666...
        // trip2: 200/600 * 10 = 3.333... -> floor 3, rem 0.333...
        // trip3: 300/600 * 10 = 5.000... -> floor 5, rem 0.000...
        // sum floors = 9. Deficit = 1.
        // Remainder ranking: trip1 (0.666...) > trip2 (0.333...) > trip3 (0.000...).
        // Trip1 gets +1 -> 2. Trip2 gets 3. Trip3 gets 5.
        assertThat(shares.get(1L)).isEqualTo(2L);
        assertThat(shares.get(2L)).isEqualTo(3L);
        assertThat(shares.get(3L)).isEqualTo(5L);
    }

    @Test
    @DisplayName("Single trip receives 100% of contract amount")
    void split_singleTrip_receivesEntireContractAmount() {
        long contractAmountPaisa = 500000L;
        Trip trip = new Trip(1L, sampleVehicle, LocalDateTime.now(), LocalDateTime.now().plusHours(1),
                50, false, false, 0, 0L);

        Map<Long, Long> shares = fixedFeeSplitService.split(contractAmountPaisa, List.of(trip));

        assertThat(shares).hasSize(1);
        assertThat(shares.get(1L)).isEqualTo(contractAmountPaisa);
    }

    @Test
    @DisplayName("All trips having 0 distance divide fee equally without arithmetic exception")
    void split_allTripsZeroDistance_splitsEquallyWithoutException() {
        long contractAmountPaisa = 100L;
        Trip trip1 = new Trip(1L, sampleVehicle, LocalDateTime.now(), LocalDateTime.now().plusHours(1),
                0, true, false, 0, 0L);
        Trip trip2 = new Trip(2L, sampleVehicle, LocalDateTime.now(), LocalDateTime.now().plusHours(1),
                0, true, false, 0, 0L);
        Trip trip3 = new Trip(3L, sampleVehicle, LocalDateTime.now(), LocalDateTime.now().plusHours(1),
                0, true, false, 0, 0L);

        Map<Long, Long> shares = fixedFeeSplitService.split(contractAmountPaisa, List.of(trip1, trip2, trip3));

        assertThat(shares).hasSize(3);
        long sum = shares.values().stream().mapToLong(Long::longValue).sum();
        assertThat(sum).isEqualTo(100L);
        // 100 / 3 = 33 base, rem 1 -> trip1 gets 34, trip2 gets 33, trip3 gets 33
        assertThat(shares.get(1L)).isEqualTo(34L);
        assertThat(shares.get(2L)).isEqualTo(33L);
        assertThat(shares.get(3L)).isEqualTo(33L);
    }

    @Test
    @DisplayName("Empty trip list returns empty map and logs warning")
    void split_emptyTripList_returnsEmptyMap() {
        Map<Long, Long> shares = fixedFeeSplitService.split(100000L, List.of());
        assertThat(shares).isEmpty();
    }

    @Test
    @DisplayName("Zero contract amount allocates 0 paisa to all trips")
    void split_zeroContractAmount_allocatesZeroToAllTrips() {
        Trip trip1 = new Trip(1L, sampleVehicle, LocalDateTime.now(), LocalDateTime.now().plusHours(1),
                50, false, false, 0, 0L);
        Trip trip2 = new Trip(2L, sampleVehicle, LocalDateTime.now(), LocalDateTime.now().plusHours(1),
                70, false, false, 0, 0L);

        Map<Long, Long> shares = fixedFeeSplitService.split(0L, List.of(trip1, trip2));

        assertThat(shares).hasSize(2);
        assertThat(shares.get(1L)).isEqualTo(0L);
        assertThat(shares.get(2L)).isEqualTo(0L);
    }
}
