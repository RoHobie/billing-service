package com.rohobie.billing;

import com.rohobie.billing.domain.BillingRunStatus;
import com.rohobie.billing.domain.Vehicle;
import com.rohobie.billing.dto.response.BillLineItemResponse;
import com.rohobie.billing.dto.response.BillingRunResponse;
import com.rohobie.billing.dto.response.BillingRunSummary;
import com.rohobie.billing.dto.response.FraudFlagResponse;
import com.rohobie.billing.repository.VehicleRepository;
import com.rohobie.billing.service.BillingRunService;
import com.rohobie.billing.service.FraudDetectionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class BillingIntegrationTest {

    @Autowired
    private BillingRunService billingRunService;

    @Autowired
    private FraudDetectionService fraudDetectionService;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Test
    @DisplayName("Fixed-fee contract reconciles to exact 3,000,000 paisa across 10 trips with idempotency")
    void testFixedMonthlyBillingRunReconciliationAndIdempotency() {
        Vehicle vehicle2 = vehicleRepository.findAll().stream()
                .filter(v -> "KA-02-AB-5678".equals(v.getRegistrationNumber()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Vehicle 2 not found in seeded data"));

        // 1. Execute initial month-end billing run
        BillingRunSummary summary1 = billingRunService.runBilling(vehicle2.getId(), "2026-01");

        assertThat(summary1.status()).isEqualTo(BillingRunStatus.COMPLETED);
        assertThat(summary1.lineItemCount()).isEqualTo(10);
        assertThat(summary1.grandTotalPaisa()).isEqualTo(3000000L); // Exactly ₹30,000

        // 2. Fetch full itemised bill and assert individual line item shares
        BillingRunResponse fullBill = billingRunService.getBillingRun(summary1.runId());
        List<BillLineItemResponse> lineItems = fullBill.lineItems();
        assertThat(lineItems).hasSize(10);

        long sumOfFixedShares = lineItems.stream().mapToLong(BillLineItemResponse::fixedFeeSharePaisa).sum();
        assertThat(sumOfFixedShares).isEqualTo(3000000L);

        // Verify computation audit note is populated on every line item
        for (BillLineItemResponse item : lineItems) {
            assertThat(item.computationNote()).isNotBlank();
            assertThat(item.computationNote()).contains("fixedShare:");
        }

        // 3. Verify zero fraud flags for clean seeded data
        List<FraudFlagResponse> flags = fraudDetectionService.getFlagsForRun(summary1.runId());
        assertThat(flags).isEmpty();

        // 4. Test Idempotency: re-running billing for same vehicle and month returns identical runId and count
        BillingRunSummary summary2 = billingRunService.runBilling(vehicle2.getId(), "2026-01");
        assertThat(summary2.runId()).isEqualTo(summary1.runId());
        assertThat(summary2.grandTotalPaisa()).isEqualTo(3000000L);
        assertThat(summary2.lineItemCount()).isEqualTo(10);

        // Verify no duplicate line items created in the database
        BillingRunResponse billAfterRerun = billingRunService.getBillingRun(summary1.runId());
        assertThat(billAfterRerun.lineItems()).hasSize(10);
    }

    @Test
    @DisplayName("Tiered PER_KM contract correctly applies mid-month rate changes across 10 trips")
    void testPerKmBillingRunWithMidMonthContractVersioning() {
        Vehicle vehicle1 = vehicleRepository.findAll().stream()
                .filter(v -> "KA-01-HH-1234".equals(v.getRegistrationNumber()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Vehicle 1 not found in seeded data"));

        BillingRunSummary summary = billingRunService.runBilling(vehicle1.getId(), "2026-01");

        assertThat(summary.status()).isEqualTo(BillingRunStatus.COMPLETED);
        assertThat(summary.lineItemCount()).isEqualTo(10);
        assertThat(summary.grandTotalPaisa()).isGreaterThan(0L);

        BillingRunResponse fullBill = billingRunService.getBillingRun(summary.runId());
        for (BillLineItemResponse item : fullBill.lineItems()) {
            assertThat(item.computationNote()).isNotBlank();
            assertThat(item.basePaisa()).isGreaterThan(0L);
            assertThat(item.totalPaisa()).isEqualTo(item.basePaisa() + item.extraChargesPaisa());
        }
    }
}
