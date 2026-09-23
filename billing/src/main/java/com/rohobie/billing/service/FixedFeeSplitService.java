package com.rohobie.billing.service;

import com.rohobie.billing.domain.Trip;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * FixedFeeSplitService
 *
 * Distributes a fixed monthly contract fee across all trips for a vehicle
 * using the Largest Remainder Method to ensure the per-trip amounts sum
 * to exactly the contract total in paisa.
 *
 * Assumption: dead-leg trips are included in the distribution denominator.
 * Design decision: Largest Remainder chosen over round-robin for determinism
 * across idempotent re-runs.
 */
@Slf4j
@Service
public class FixedFeeSplitService {

    /**
     * Internal container holding intermediate calculation state for each trip.
     */
    @Getter
    private static class TripShareCalculation {
        private final Trip trip;
        private final long floorShare;
        private final BigDecimal fractionalRemainder;
        private long finalShare;

        TripShareCalculation(Trip trip, long floorShare, BigDecimal fractionalRemainder) {
            this.trip = trip;
            this.floorShare = floorShare;
            this.fractionalRemainder = fractionalRemainder;
            this.finalShare = floorShare;
        }

        void incrementShare() {
            this.finalShare++;
        }
    }

    /**
     * Splits a fixed contract amount in paisa proportionally across a list of trips
     * using the Largest Remainder Method.
     *
     * @param contractAmountPaisa the total monthly fixed fee in paisa
     * @param trips               the list of trips belonging to the vehicle for the billing period
     * @return a map of trip ID to its allocated share in paisa
     */
    public Map<Long, Long> split(long contractAmountPaisa, List<Trip> trips) {
        if (trips == null || trips.isEmpty()) {
            log.warn("Attempted to split fixed fee of {} paisa across null or empty trip list", contractAmountPaisa);
            return Collections.emptyMap();
        }

        if (contractAmountPaisa == 0L) {
            log.debug("Contract amount is 0 paisa; all {} trips receive 0 paisa share", trips.size());
            return trips.stream()
                    .collect(Collectors.toMap(Trip::getId, trip -> 0L, (a, b) -> a, LinkedHashMap::new));
        }

        if (trips.size() == 1) {
            Trip singleTrip = trips.get(0);
            log.debug("Single trip present (ID: {}); allocating full contract amount {} paisa",
                    singleTrip.getId(), contractAmountPaisa);
            return Map.of(singleTrip.getId(), contractAmountPaisa);
        }

        // Sum distanceKm across all trips, explicitly including dead-leg runs
        long totalDutyKm = 0L;
        for (Trip trip : trips) {
            totalDutyKm += trip.getDistanceKm();
        }

        // Handle edge case where all trips have 0 km distance
        if (totalDutyKm == 0L) {
            log.debug("Total duty km across all {} trips is 0; splitting equally across trips", trips.size());
            return splitEqually(contractAmountPaisa, trips);
        }

        // Largest Remainder Method:
        // 1. Calculate exactShare and floorShare for each trip using BigDecimal (scale 10)
        BigDecimal totalKmBd = BigDecimal.valueOf(totalDutyKm);
        BigDecimal contractAmountBd = BigDecimal.valueOf(contractAmountPaisa);

        List<TripShareCalculation> calculations = trips.stream().map(trip -> {
            BigDecimal tripKmBd = BigDecimal.valueOf(trip.getDistanceKm());
            // exactShare = (trip.distanceKm / totalDutyKm) * contractAmountPaisa
            BigDecimal exactShare = tripKmBd.divide(totalKmBd, 10, RoundingMode.HALF_UP)
                    .multiply(contractAmountBd);
            long floorShare = exactShare.setScale(0, RoundingMode.FLOOR).longValue();
            BigDecimal fractionalRemainder = exactShare.subtract(BigDecimal.valueOf(floorShare));
            return new TripShareCalculation(trip, floorShare, fractionalRemainder);
        }).collect(Collectors.toList());

        // 2. Compute the rounding deficit (remainder in paisa)
        long sumOfFloorShares = calculations.stream().mapToLong(c -> c.floorShare).sum();
        long remainderPaisa = contractAmountPaisa - sumOfFloorShares;

        // 3. Rank trips descending by fractional remainder; use trip ID ASC as tie-breaker for determinism
        calculations.sort(Comparator
                .comparing(TripShareCalculation::getFractionalRemainder, Comparator.reverseOrder())
                .thenComparing(c -> c.getTrip().getId(), Comparator.nullsLast(Comparator.naturalOrder())));

        // 4. Distribute 1 paisa to the top 'remainderPaisa' trips
        for (int i = 0; i < remainderPaisa && i < calculations.size(); i++) {
            calculations.get(i).incrementShare();
        }

        // 5. Build result map and assert exact sum equality
        Map<Long, Long> resultMap = new LinkedHashMap<>();
        long totalDistributedPaisa = 0L;
        for (TripShareCalculation calculation : calculations) {
            resultMap.put(calculation.getTrip().getId(), calculation.getFinalShare());
            totalDistributedPaisa += calculation.getFinalShare();
        }

        if (totalDistributedPaisa != contractAmountPaisa) {
            throw new IllegalStateException(String.format(
                    "Assertion failed: sum of distributed shares (%d paisa) does not match contract total (%d paisa)",
                    totalDistributedPaisa, contractAmountPaisa));
        }

        log.debug("Successfully split {} paisa across {} trips; sum matches exactly",
                contractAmountPaisa, trips.size());
        return resultMap;
    }

    /**
     * Splits contract fee equally across trips when all trips have 0 km,
     * assigning remainder paisa to the initial trips deterministically.
     */
    private Map<Long, Long> splitEqually(long contractAmountPaisa, List<Trip> trips) {
        int n = trips.size();
        long baseShare = contractAmountPaisa / n;
        long remainder = contractAmountPaisa % n;

        Map<Long, Long> resultMap = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            long share = baseShare + (i < remainder ? 1L : 0L);
            resultMap.put(trips.get(i).getId(), share);
        }
        return resultMap;
    }
}
