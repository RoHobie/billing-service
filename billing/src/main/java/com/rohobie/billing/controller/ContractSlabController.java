package com.rohobie.billing.controller;

import com.rohobie.billing.dto.request.ContractSlabRequest;
import com.rohobie.billing.dto.response.ApiResponse;
import com.rohobie.billing.dto.response.ContractSlabResponse;
import com.rohobie.billing.service.ContractService;
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
@RequestMapping("/api/contracts/{contractId}/slabs")
public class ContractSlabController {

    private final ContractService contractService;

    @PostMapping
    public ResponseEntity<ApiResponse<ContractSlabResponse>> addSlab(
            @PathVariable Long contractId,
            @Valid @RequestBody ContractSlabRequest request) {
        ContractSlabResponse response = contractService.addSlab(contractId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ContractSlabResponse>>> getSlabs(@PathVariable Long contractId) {
        List<ContractSlabResponse> responses = contractService.getSlabsForContract(contractId);
        return ResponseEntity.ok(ApiResponse.of(responses));
    }
}