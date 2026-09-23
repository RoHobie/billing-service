package com.rohobie.billing.service;

import com.rohobie.billing.domain.Contract;
import com.rohobie.billing.exception.ResourceNotFoundException;
import com.rohobie.billing.repository.ContractRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * ContractLookupService
 *
 * Resolves the active contract version for a given vehicle on a specific trip date.
 * Design decision: Uses contract.effectiveFrom with descending order to resolve
 * mid-month rate changes deterministically without requiring a separate versioning join table.
 */
@Service
public class ContractLookupService {

    private static final Logger logger = LoggerFactory.getLogger(ContractLookupService.class);

    private final ContractRepository contractRepository;

    public ContractLookupService(ContractRepository contractRepository) {
        this.contractRepository = contractRepository;
    }

    /**
     * Finds the active contract for a vehicle on a given trip date.
     * Selects the contract with the latest effectiveFrom on or before tripDate.
     *
     * @param vehicleId the vehicle ID
     * @param tripDate the date of the trip
     * @return the active Contract
     * @throws ResourceNotFoundException if no contract is active for the vehicle on that date
     */
    @Transactional(readOnly = true)
    public Contract findActiveContract(Long vehicleId, LocalDate tripDate) {
        logger.debug("Looking up active contract for vehicle ID: {} on date: {}", vehicleId, tripDate);
        List<Contract> contracts = contractRepository.findActiveContractsOnDate(vehicleId, tripDate);
        if (contracts.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No active contract found for vehicle " + vehicleId + " on date " + tripDate);
        }
        return contracts.get(0);
    }
}