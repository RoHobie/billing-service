package com.rohobie.billing.dto.response;

public record ContractSlabResponse(
        Long id,
        Long contractId,
        int fromKm,
        Integer toKm,
        long ratePerKmPaisa
) {
}