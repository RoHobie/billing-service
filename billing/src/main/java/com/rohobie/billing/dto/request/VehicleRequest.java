package com.rohobie.billing.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record VehicleRequest(
        @NotBlank(message = "Registration number is required")
        String registrationNumber,
        String type,
        @NotNull(message = "Vendor ID is required")
        Long vendorId
) {
}