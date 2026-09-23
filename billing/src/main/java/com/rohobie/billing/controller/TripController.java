package com.rohobie.billing.controller;

import com.rohobie.billing.dto.request.TripRequest;
import com.rohobie.billing.dto.response.ApiResponse;
import com.rohobie.billing.dto.response.TripResponse;
import com.rohobie.billing.service.TripService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trips")
public class TripController {

    private final TripService tripService;

    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TripResponse>> createTrip(@Valid @RequestBody TripRequest request) {
        TripResponse response = tripService.createTrip(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TripResponse>> getTripById(@PathVariable Long id) {
        TripResponse response = tripService.getTripById(id);
        return ResponseEntity.ok(ApiResponse.of(response));
    }
}