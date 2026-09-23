package com.rohobie.billing.dto.response;

import com.rohobie.billing.domain.BillingRunStatus;

import java.time.LocalDateTime;
import java.util.List;

public record BillingRunResponse(
        Long runId,
        Long vehicleId,
        String billingMonth,
        BillingRunStatus status,
        LocalDateTime runAt,
        int lineItemCount,
        long grandTotalPaisa,
        List<BillLineItemResponse> lineItems
) {
}
