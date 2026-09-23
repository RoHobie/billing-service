package com.rohobie.billing.service;

import com.rohobie.billing.dto.response.SystemStatsResponse;
import com.rohobie.billing.repository.BillLineItemRepository;
import com.rohobie.billing.repository.BillingRunRepository;
import com.rohobie.billing.repository.FraudFlagRepository;
import com.rohobie.billing.repository.VehicleRepository;
import com.rohobie.billing.repository.VendorRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonitoringServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private VendorRepository vendorRepository;

    @Mock
    private BillingRunRepository billingRunRepository;

    @Mock
    private BillLineItemRepository billLineItemRepository;

    @Mock
    private FraudFlagRepository fraudFlagRepository;

    @InjectMocks
    private MonitoringService monitoringService;

    @Test
    @DisplayName("Aggregates telemetry and JVM statistics correctly")
    void getSystemStats_returnsValidMetrics() {
        when(vehicleRepository.count()).thenReturn(5L);
        when(vendorRepository.count()).thenReturn(2L);
        when(billingRunRepository.count()).thenReturn(10L);
        when(billLineItemRepository.sumTotalPaisa()).thenReturn(1500000L);
        when(fraudFlagRepository.count()).thenReturn(3L);

        SystemStatsResponse stats = monitoringService.getSystemStats();

        assertThat(stats).isNotNull();
        assertThat(stats.totalVehicles()).isEqualTo(5L);
        assertThat(stats.totalVendors()).isEqualTo(2L);
        assertThat(stats.totalBillingRuns()).isEqualTo(10L);
        assertThat(stats.totalRevenuePaisa()).isEqualTo(1500000L);
        assertThat(stats.totalFraudFlags()).isEqualTo(3L);
        assertThat(stats.systemHealth()).isEqualTo("OPERATIONAL");
        assertThat(stats.cacheStatus()).isEqualTo("ACTIVE");
        assertThat(stats.jvmMemoryUsedMb()).isGreaterThanOrEqualTo(0L);
        assertThat(stats.jvmMemoryMaxMb()).isGreaterThan(0L);
        assertThat(stats.uptimeSeconds()).isGreaterThanOrEqualTo(0L);
    }
}
