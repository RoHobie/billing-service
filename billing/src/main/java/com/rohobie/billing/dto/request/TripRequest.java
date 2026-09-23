package com.rohobie.billing.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record TripRequest(
        @NotNull(message = "Vehicle ID is required")
        Long vehicleId,
        @NotNull(message = "Start time is required")
        LocalDateTime startTime,
        @NotNull(message = "End time is required")
        LocalDateTime endTime,
        @Min(value = 0, message = "distanceKm cannot be negative")
        int distanceKm,
        boolean isDeadLeg,
        boolean hasNightCharge,
        @Min(value = 0, message = "waitingMinutes cannot be negative")
        int waitingMinutes,
        @Min(value = 0, message = "tollAmountPaisa cannot be negative")
        long tollAmountPaisa
) {
}