package com.rohobie.billing.dto.response;

public record BillLineItemResponse(
        Long id,
        Long tripId,
        long basePaisa,
        long extraChargesPaisa,
        long fixedFeeSharePaisa,
        long totalPaisa,
        String computationNote
) {
}
