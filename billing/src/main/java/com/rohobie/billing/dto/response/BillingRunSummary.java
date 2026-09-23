package com.rohobie.billing.dto.response;

import com.rohobie.billing.domain.BillingRunStatus;

public record BillingRunSummary(
        Long runId,
        Long vehicleId,
        String billingMonth,
        BillingRunStatus status,
        int lineItemCount,
        long grandTotalPaisa
) {
}
