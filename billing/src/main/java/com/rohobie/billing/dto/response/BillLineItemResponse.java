package com.rohobie.billing.dto.response;

public record BillLineItemResponse(
        Long id,
        Long tripId,
        long basePaisa,
        long extraChargesPaisa,
        long fixedFeeSharePaisa,
        long totalPaisa,
        String computationNote,
        Integer distanceKm,
        String startTime
) {
    public BillLineItemResponse(Long id, Long tripId, long basePaisa, long extraChargesPaisa, long fixedFeeSharePaisa, long totalPaisa, String computationNote) {
        this(id, tripId, basePaisa, extraChargesPaisa, fixedFeeSharePaisa, totalPaisa, computationNote, null, null);
    }
}
