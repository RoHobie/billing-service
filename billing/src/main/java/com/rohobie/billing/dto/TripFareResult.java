package com.rohobie.billing.dto;

/**
 * Result of trip fare computation.
 *
 * @param basePaisa base fare in paisa
 * @param extraChargesPaisa extra charges in paisa
 * @param computationNote human-readable audit trail of fare calculation
 */
public record TripFareResult(
        long basePaisa,
        long extraChargesPaisa,
        String computationNote
) {
    public long totalPaisa() {
        return basePaisa + extraChargesPaisa;
    }
}