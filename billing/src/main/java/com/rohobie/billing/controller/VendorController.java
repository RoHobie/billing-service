package com.rohobie.billing.controller;

import com.rohobie.billing.dto.request.VendorRequest;
import com.rohobie.billing.dto.response.ApiResponse;
import com.rohobie.billing.dto.response.VehicleResponse;
import com.rohobie.billing.dto.response.VendorResponse;
import com.rohobie.billing.service.VehicleService;
import com.rohobie.billing.service.VendorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/vendors")
public class VendorController {

    private final VendorService vendorService;
    private final VehicleService vehicleService;

    @PostMapping
    public ResponseEntity<ApiResponse<VendorResponse>> createVendor(@Valid @RequestBody VendorRequest request) {
        VendorResponse response = vendorService.createVendor(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<VendorResponse>>> getAllVendors() {
        List<VendorResponse> vendors = vendorService.getAllVendors();
        return ResponseEntity.ok(ApiResponse.of(vendors));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VendorResponse>> getVendorById(@PathVariable Long id) {
        VendorResponse response = vendorService.getVendorById(id);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @GetMapping("/{id}/vehicles")
    public ResponseEntity<ApiResponse<List<VehicleResponse>>> getVehiclesByVendor(@PathVariable Long id) {
        List<VehicleResponse> vehicles = vehicleService.getVehiclesByVendorId(id);
        return ResponseEntity.ok(ApiResponse.of(vehicles));
    }
}