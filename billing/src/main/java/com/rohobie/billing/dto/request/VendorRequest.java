package com.rohobie.billing.dto.request;

import jakarta.validation.constraints.NotBlank;

public record VendorRequest(
        @NotBlank(message = "Vendor name is required")
        String name,
        String contactEmail
) {
}