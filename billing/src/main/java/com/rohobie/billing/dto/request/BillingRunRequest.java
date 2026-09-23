package com.rohobie.billing.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record BillingRunRequest(
        @NotNull(message = "Vehicle ID is required")
        Long vehicleId,

        @NotNull(message = "Billing month is required")
        @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "Billing month must be in YYYY-MM format")
        String billingMonth
) {
}
