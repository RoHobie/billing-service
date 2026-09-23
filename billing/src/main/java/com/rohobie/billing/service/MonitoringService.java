package com.rohobie.billing.service;

import com.rohobie.billing.dto.response.SystemStatsResponse;
import com.rohobie.billing.repository.BillLineItemRepository;
import com.rohobie.billing.repository.BillingRunRepository;
import com.rohobie.billing.repository.FraudFlagRepository;
import com.rohobie.billing.repository.VehicleRepository;
import com.rohobie.billing.repository.VendorRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.management.ManagementFactory;

/**
 * MonitoringService
 *
 * Aggregates operational telemetry and system health indicators for real-time monitoring.
 * Provides live business performance metrics including active fleet size, billed revenue,
 * anomaly volumes, and JVM memory utilization.
 */
@Slf4j
@Service
public class MonitoringService {

    private final VehicleRepository vehicleRepository;
    private final VendorRepository vendorRepository;
    private final BillingRunRepository billingRunRepository;
    private final BillLineItemRepository billLineItemRepository;
    private final FraudFlagRepository fraudFlagRepository;

    public MonitoringService(VehicleRepository vehicleRepository,
                             VendorRepository vendorRepository,
                             BillingRunRepository billingRunRepository,
                             BillLineItemRepository billLineItemRepository,
                             FraudFlagRepository fraudFlagRepository) {
        this.vehicleRepository = vehicleRepository;
        this.vendorRepository = vendorRepository;
        this.billingRunRepository = billingRunRepository;
        this.billLineItemRepository = billLineItemRepository;
        this.fraudFlagRepository = fraudFlagRepository;
    }

    /**
     * Gathers operational and system telemetry indicators.
     *
     * @return SystemStatsResponse containing aggregate business and system runtime metrics
     */
    @Transactional(readOnly = true)
    public SystemStatsResponse getSystemStats() {
        log.debug("Compiling system telemetry metrics");

        long totalVehicles = vehicleRepository.count();
        long totalVendors = vendorRepository.count();
        long totalBillingRuns = billingRunRepository.count();
        long totalRevenuePaisa = billLineItemRepository.sumTotalPaisa();
        long totalFraudFlags = fraudFlagRepository.count();

        long uptimeSeconds = ManagementFactory.getRuntimeMXBean().getUptime() / 1000L;
        Runtime runtime = Runtime.getRuntime();
        long jvmMemoryUsedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024L * 1024L);
        long jvmMemoryMaxMb = runtime.maxMemory() / (1024L * 1024L);

        return new SystemStatsResponse(
                totalVehicles,
                totalVendors,
                totalBillingRuns,
                totalRevenuePaisa,
                totalFraudFlags,
                uptimeSeconds,
                jvmMemoryUsedMb,
                jvmMemoryMaxMb,
                "OPERATIONAL",
                "ACTIVE"
        );
    }
}
