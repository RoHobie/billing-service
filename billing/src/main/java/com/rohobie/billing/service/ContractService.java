package com.rohobie.billing.service;

import com.rohobie.billing.domain.Contract;
import com.rohobie.billing.domain.ContractSlab;
import com.rohobie.billing.domain.Vehicle;
import com.rohobie.billing.domain.Vendor;
import com.rohobie.billing.dto.request.ContractRequest;
import com.rohobie.billing.dto.request.ContractSlabRequest;
import com.rohobie.billing.dto.response.ContractResponse;
import com.rohobie.billing.dto.response.ContractSlabResponse;
import com.rohobie.billing.exception.ResourceNotFoundException;
import com.rohobie.billing.repository.ContractRepository;
import com.rohobie.billing.repository.ContractSlabRepository;
import com.rohobie.billing.repository.VehicleRepository;
import com.rohobie.billing.repository.VendorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * ContractService
 *
 * Manages contracts and rate slabs for fleet vehicles.
 * Design decision: Links contracts to vehicles and vendors, supporting versioning
 * via effectiveFrom dates and tiered pricing via slabs.
 */
@Service
public class ContractService {

    private static final Logger logger = LoggerFactory.getLogger(ContractService.class);

    private final ContractRepository contractRepository;
    private final ContractSlabRepository contractSlabRepository;
    private final VehicleRepository vehicleRepository;
    private final VendorRepository vendorRepository;

    public ContractService(ContractRepository contractRepository,
                           ContractSlabRepository contractSlabRepository,
                           VehicleRepository vehicleRepository,
                           VendorRepository vendorRepository) {
        this.contractRepository = contractRepository;
        this.contractSlabRepository = contractSlabRepository;
        this.vehicleRepository = vehicleRepository;
        this.vendorRepository = vendorRepository;
    }

    /** Creates a new contract for a vehicle and vendor. */
    @Transactional
    public ContractResponse createContract(ContractRequest request) {
        logger.info("Creating contract for vehicle ID: {} and vendor ID: {}", request.vehicleId(), request.vendorId());
        Vehicle vehicle = vehicleRepository.findById(request.vehicleId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle with id " + request.vehicleId() + " not found"));
        Vendor vendor = vendorRepository.findById(request.vendorId())
                .orElseThrow(() -> new ResourceNotFoundException("Vendor with id " + request.vendorId() + " not found"));

        Contract contract = new Contract(
                null,
                vehicle,
                vendor,
                request.contractType(),
                request.baseAmountPaisa(),
                request.freeKm(),
                request.effectiveFrom(),
                request.nightChargePaisa(),
                request.waitingRatePerMinutePaisa()
        );
        Contract saved = contractRepository.save(contract);
        return mapToResponse(saved);
    }

    /** Retrieves contract by ID. */
    @Transactional(readOnly = true)
    public ContractResponse getContractById(Long id) {
        Contract contract = contractRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contract with id " + id + " not found"));
        return mapToResponse(contract);
    }

    /** Adds a tiered slab to an existing contract. */
    @Transactional
    public ContractSlabResponse addSlab(Long contractId, ContractSlabRequest request) {
        logger.info("Adding slab fromKm: {}, toKm: {}, rate: {} to contract ID: {}",
                request.fromKm(), request.toKm(), request.ratePerKmPaisa(), contractId);
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Contract with id " + contractId + " not found"));

        ContractSlab slab = new ContractSlab(contract, request.fromKm(), request.toKm(), request.ratePerKmPaisa());
        ContractSlab saved = contractSlabRepository.save(slab);
        return mapToSlabResponse(saved);
    }

    /** Retrieves all slabs for a contract ordered by fromKm ascending. */
    @Transactional(readOnly = true)
    public List<ContractSlabResponse> getSlabsForContract(Long contractId) {
        if (!contractRepository.existsById(contractId)) {
            throw new ResourceNotFoundException("Contract with id " + contractId + " not found");
        }
        return contractSlabRepository.findByContractIdOrderByFromKmAsc(contractId)
                .stream()
                .map(this::mapToSlabResponse)
                .toList();
    }

    private ContractResponse mapToResponse(Contract contract) {
        return new ContractResponse(
                contract.getId(),
                contract.getVehicle().getId(),
                contract.getVehicle().getRegistrationNumber(),
                contract.getVendor().getId(),
                contract.getVendor().getName(),
                contract.getContractType(),
                contract.getBaseAmountPaisa(),
                contract.getFreeKm(),
                contract.getEffectiveFrom(),
                contract.getNightChargePaisa(),
                contract.getWaitingRatePerMinutePaisa()
        );
    }

    private ContractSlabResponse mapToSlabResponse(ContractSlab slab) {
        return new ContractSlabResponse(
                slab.getId(),
                slab.getContract().getId(),
                slab.getFromKm(),
                slab.getToKm(),
                slab.getRatePerKmPaisa()
        );
    }
}