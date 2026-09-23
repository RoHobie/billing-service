package com.rohobie.billing.dto.response;

import com.rohobie.billing.domain.FraudFlagType;

public record FraudFlagResponse(
        Long id,
        Long billingRunId,
        Long tripId,
        FraudFlagType flagType,
        String description
) {
}
