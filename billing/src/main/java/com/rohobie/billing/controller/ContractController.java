package com.rohobie.billing.controller;

import com.rohobie.billing.dto.request.ContractRequest;
import com.rohobie.billing.dto.response.ApiResponse;
import com.rohobie.billing.dto.response.ContractResponse;
import com.rohobie.billing.service.ContractService;
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
@RequestMapping("/api/contracts")
public class ContractController {

    private final ContractService contractService;

    public ContractController(ContractService contractService) {
        this.contractService = contractService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ContractResponse>> createContract(@Valid @RequestBody ContractRequest request) {
        ContractResponse response = contractService.createContract(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ContractResponse>> getContractById(@PathVariable Long id) {
        ContractResponse response = contractService.getContractById(id);
        return ResponseEntity.ok(ApiResponse.of(response));
    }
}