package com.rohobie.billing.dto.response;

public record SystemStatsResponse(
        long totalVehicles,
        long totalVendors,
        long totalBillingRuns,
        long totalRevenuePaisa,
        long totalFraudFlags,
        long uptimeSeconds,
        long jvmMemoryUsedMb,
        long jvmMemoryMaxMb,
        String systemHealth,
        String cacheStatus
) {
}
