package com.rohobie.billing.controller;

import com.rohobie.billing.dto.response.ApiResponse;
import com.rohobie.billing.dto.response.SystemStatsResponse;
import com.rohobie.billing.service.MonitoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * MonitoringController
 *
 * REST Controller delivering system monitoring and fleet operational telemetry.
 * Serves live business health and runtime metrics.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/monitoring")
public class MonitoringController {

    private final MonitoringService monitoringService;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<SystemStatsResponse>> getSystemStats() {
        SystemStatsResponse stats = monitoringService.getSystemStats();
        return ResponseEntity.ok(ApiResponse.of(stats));
    }
}
