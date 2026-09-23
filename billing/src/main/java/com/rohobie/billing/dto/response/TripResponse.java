package com.rohobie.billing.dto.response;

import java.time.LocalDateTime;

public record TripResponse(
        Long id,
        Long vehicleId,
        String vehicleRegistrationNumber,
        LocalDateTime startTime,
        LocalDateTime endTime,
        int distanceKm,
        boolean isDeadLeg,
        boolean hasNightCharge,
        int waitingMinutes,
        long tollAmountPaisa
) {
}