package com.rohobie.billing.controller;

import com.rohobie.billing.dto.request.BillingRunRequest;
import com.rohobie.billing.dto.response.ApiResponse;
import com.rohobie.billing.dto.response.BillingRunResponse;
import com.rohobie.billing.dto.response.BillingRunSummary;
import com.rohobie.billing.dto.response.FraudFlagResponse;
import com.rohobie.billing.service.BillingRunService;
import com.rohobie.billing.service.FraudDetectionService;
import jakarta.validation.Valid;
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
@RestController
@RequestMapping("/api/billing")
public class BillingRunController {

    private final BillingRunService billingRunService;
    private final FraudDetectionService fraudDetectionService;
    private final com.rohobie.billing.service.InvoicePdfService invoicePdfService;

    public BillingRunController(BillingRunService billingRunService,
                                FraudDetectionService fraudDetectionService,
                                com.rohobie.billing.service.InvoicePdfService invoicePdfService) {
        this.billingRunService = billingRunService;
        this.fraudDetectionService = fraudDetectionService;
        this.invoicePdfService = invoicePdfService;
    }

    @PostMapping("/run")
    public ResponseEntity<ApiResponse<BillingRunSummary>> runBilling(@Valid @RequestBody BillingRunRequest request) {
        BillingRunSummary summary = billingRunService.runBilling(request.vehicleId(), request.billingMonth());
        return ResponseEntity.ok(ApiResponse.of(summary));
    }

    @GetMapping("/run/{runId}")
    public ResponseEntity<ApiResponse<BillingRunResponse>> getBillingRun(@PathVariable Long runId) {
        BillingRunResponse response = billingRunService.getBillingRun(runId);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @GetMapping("/run/{runId}/summary")
    public ResponseEntity<ApiResponse<BillingRunSummary>> getBillingRunSummary(@PathVariable Long runId) {
        BillingRunSummary summary = billingRunService.getBillingRunSummary(runId);
        return ResponseEntity.ok(ApiResponse.of(summary));
    }

    @GetMapping("/run/{runId}/flags")
    public ResponseEntity<ApiResponse<java.util.List<com.rohobie.billing.dto.response.FraudFlagResponse>>> getBillingRunFlags(@PathVariable Long runId) {
        java.util.List<com.rohobie.billing.dto.response.FraudFlagResponse> flags = fraudDetectionService.getFlagsForRun(runId);
        return ResponseEntity.ok(ApiResponse.of(flags));
    }

    @GetMapping("/run/{runId}/invoice/pdf")
    public ResponseEntity<byte[]> downloadInvoicePdf(@PathVariable Long runId) {
        byte[] pdfBytes = invoicePdfService.generateInvoicePdf(runId);
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"invoice-" + runId + ".pdf\"")
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
