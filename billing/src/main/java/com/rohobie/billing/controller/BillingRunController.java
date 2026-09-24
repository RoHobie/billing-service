package com.rohobie.billing.controller;

import com.rohobie.billing.dto.request.BillingRunRequest;
import com.rohobie.billing.dto.response.ApiResponse;
import com.rohobie.billing.dto.response.BillLineItemResponse;
import com.rohobie.billing.dto.response.BillingRunResponse;
import com.rohobie.billing.dto.response.BillingRunSummary;
import com.rohobie.billing.dto.response.FraudFlagResponse;
import com.rohobie.billing.service.BillingRunService;
import com.rohobie.billing.service.FraudDetectionService;
import com.rohobie.billing.service.InvoicePdfService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * BillingRunController
 *
 * REST Controller exposing billing run initiation and inspection endpoints.
 * Enforces separation of concerns by delegating all logic to the service layer.
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/billing")
public class BillingRunController {

    private final BillingRunService billingRunService;
    private final FraudDetectionService fraudDetectionService;
    private final InvoicePdfService invoicePdfService;

    @PostMapping("/run")
    public ResponseEntity<ApiResponse<BillingRunSummary>> runBilling(@Valid @RequestBody BillingRunRequest request) {
        try {
            BillingRunSummary summary = billingRunService.runBilling(request.vehicleId(), request.billingMonth());
            return ResponseEntity.ok(ApiResponse.of(summary));
        } catch (DataIntegrityViolationException ex) {
            log.info("Concurrent duplicate billing run detected for vehicle {} and month {}; returning existing summary",
                    request.vehicleId(), request.billingMonth());
            BillingRunSummary summary = billingRunService.getBillingRunSummaryByVehicleAndMonth(
                    request.vehicleId(), request.billingMonth());
            return ResponseEntity.ok(ApiResponse.of(summary));
        }
    }

    @GetMapping("/runs")
    public ResponseEntity<ApiResponse<List<BillingRunSummary>>> getAllBillingRuns(
            @org.springframework.web.bind.annotation.RequestParam(required = false) Long vehicleId) {
        List<BillingRunSummary> runs = (vehicleId != null)
                ? billingRunService.getBillingRunsByVehicle(vehicleId)
                : billingRunService.getAllBillingRuns();
        return ResponseEntity.ok(ApiResponse.of(runs));
    }

    @GetMapping("/run/{runId}")
    public ResponseEntity<ApiResponse<BillingRunResponse>> getBillingRun(@PathVariable Long runId) {
        BillingRunResponse response = billingRunService.getBillingRun(runId);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @GetMapping("/run/{runId}/items")
    public ResponseEntity<ApiResponse<Page<BillLineItemResponse>>> getBillingRunItems(
            @PathVariable Long runId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<BillLineItemResponse> page = billingRunService.getBillingRunItems(runId, pageable);
        return ResponseEntity.ok(ApiResponse.of(page));
    }

    @GetMapping("/run/{runId}/summary")
    public ResponseEntity<ApiResponse<BillingRunSummary>> getBillingRunSummary(@PathVariable Long runId) {
        BillingRunSummary summary = billingRunService.getBillingRunSummary(runId);
        return ResponseEntity.ok(ApiResponse.of(summary));
    }

    @GetMapping("/run/{runId}/flags")
    public ResponseEntity<ApiResponse<List<FraudFlagResponse>>> getBillingRunFlags(@PathVariable Long runId) {
        List<FraudFlagResponse> flags = fraudDetectionService.getFlagsForRun(runId);
        return ResponseEntity.ok(ApiResponse.of(flags));
    }

    @GetMapping("/run/{runId}/invoice/pdf")
    public ResponseEntity<byte[]> downloadInvoicePdf(@PathVariable Long runId) {
        byte[] pdfBytes = invoicePdfService.generateInvoicePdf(runId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"invoice-" + runId + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
