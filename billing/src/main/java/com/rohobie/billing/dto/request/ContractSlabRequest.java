package com.rohobie.billing.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ContractSlabRequest(
        @Min(value = 0, message = "fromKm must be greater than or equal to 0")
        int fromKm,
        Integer toKm,
        @NotNull(message = "ratePerKmPaisa is required")
        @Min(value = 0, message = "ratePerKmPaisa must be non-negative")
        long ratePerKmPaisa
) {
}