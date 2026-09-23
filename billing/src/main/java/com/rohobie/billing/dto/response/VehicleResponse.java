package com.rohobie.billing.dto.response;

public record VehicleResponse(
        Long id,
        String registrationNumber,
        String type,
        Long vendorId,
        String vendorName
) {
}