package com.rohobie.billing.service;

import com.rohobie.billing.domain.Contract;
import com.rohobie.billing.domain.ContractSlab;
import com.rohobie.billing.domain.ContractType;
import com.rohobie.billing.domain.Trip;
import com.rohobie.billing.repository.ContractSlabRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * SlabComputationService
 *
 * Walks tiered slab rates for PER_KM contracts, applies flat fees for PER_TRIP contracts,
 * and returns 0 paisa for FIXED_MONTHLY contracts (which are distributed at month-end).
 *
 * Assumption: Slabs apply cumulatively to total trip distance like tax slabs.
 * Assumption: Free km reduce the billable distance before walking the rate slabs.
 * Design decision: Strict long-precision integer arithmetic to avoid floating-point errors.
 */
@Service
public class SlabComputationService {

    private static final Logger logger = LoggerFactory.getLogger(SlabComputationService.class);

    private final ContractSlabRepository contractSlabRepository;

    public SlabComputationService(ContractSlabRepository contractSlabRepository) {
        this.contractSlabRepository = contractSlabRepository;
    }

    /**
     * Computes the base fare for a trip based on its active contract's slab rates.
     *
     * @param trip the trip to compute fare for
     * @param contract the active contract governing the vehicle
     * @return the base fare in paisa
     */
    @Transactional(readOnly = true)
    public long compute(Trip trip, Contract contract) {
        if (contract.getContractType() == ContractType.PER_TRIP) {
            logger.debug("PER_TRIP contract type; returning flat base amount: {} paisa", contract.getBaseAmountPaisa());
            return contract.getBaseAmountPaisa();
        }

        if (contract.getContractType() == ContractType.FIXED_MONTHLY) {
            logger.debug("FIXED_MONTHLY contract type; base fare is 0 paisa (split in month-end billing run)");
            return 0L;
        }

        // PER_KM computation
        // ASSUMPTION: Free km reduce the billable distance prior to slab evaluation
        int billableDistance = Math.max(0, trip.getDistanceKm() - contract.getFreeKm());
        if (billableDistance == 0) {
            return 0L;
        }

        List<ContractSlab> slabs = new ArrayList<>(contractSlabRepository.findByContractIdOrderByFromKmAsc(contract.getId()));
        // Sort by fromKm to ensure we walk slabs in the correct ascending order
        slabs.sort(Comparator.comparingInt(ContractSlab::getFromKm));

        long runningTotal = 0L;
        for (ContractSlab slab : slabs) {
            // ASSUMPTION: If slab.fromKm is 0, the 1st km is index 1; otherwise fromKm is 1-indexed (e.g. 101)
            int effectiveFrom = slab.getFromKm() == 0 ? 1 : slab.getFromKm();

            if (billableDistance < effectiveFrom) {
                // Trip did not reach this slab tier
                break;
            }

            // toKm is null for the last slab ("and above"), so we use Integer.MAX_VALUE as the ceiling
            int slabCeiling = slab.getToKm() != null ? slab.getToKm() : Integer.MAX_VALUE;
            int kmUpToSlabCeiling = Math.min(billableDistance, slabCeiling);
            int kmInThisSlab = kmUpToSlabCeiling - effectiveFrom + 1;

            if (kmInThisSlab > 0) {
                long slabCost = (long) kmInThisSlab * slab.getRatePerKmPaisa();
                logger.debug("Slab [{} - {}]: {} km @ {} paisa/km = {} paisa",
                        slab.getFromKm(), slab.getToKm(), kmInThisSlab, slab.getRatePerKmPaisa(), slabCost);
                runningTotal += slabCost;
            }
        }

        return runningTotal;
    }

    /**
     * Builds a detailed human-readable audit string describing the base fare calculation.
     *
     * @param trip the trip being audited
     * @param contract the contract applied
     * @return a formatted computation note snippet for base fare
     */
    @Transactional(readOnly = true)
    public String buildSlabBreakdownNote(Trip trip, Contract contract) {
        if (contract.getContractType() == ContractType.PER_TRIP) {
            return "PER_TRIP: base=" + contract.getBaseAmountPaisa() + "p";
        }

        if (contract.getContractType() == ContractType.FIXED_MONTHLY) {
            return "FIXED_MONTHLY: base=0p (split at month-end)";
        }

        int billableDistance = Math.max(0, trip.getDistanceKm() - contract.getFreeKm());
        if (billableDistance == 0) {
            return "PER_KM: 0 billable km (freeKm=" + contract.getFreeKm() + ") = 0p";
        }

        List<ContractSlab> slabs = new ArrayList<>(contractSlabRepository.findByContractIdOrderByFromKmAsc(contract.getId()));
        slabs.sort(Comparator.comparingInt(ContractSlab::getFromKm));

        StringBuilder sb = new StringBuilder("PER_KM: ");
        List<String> segments = new ArrayList<>();
        long runningTotal = 0L;

        for (ContractSlab slab : slabs) {
            int effectiveFrom = slab.getFromKm() == 0 ? 1 : slab.getFromKm();
            if (billableDistance < effectiveFrom) {
                break;
            }

            int slabCeiling = slab.getToKm() != null ? slab.getToKm() : Integer.MAX_VALUE;
            int kmUpToSlabCeiling = Math.min(billableDistance, slabCeiling);
            int kmInThisSlab = kmUpToSlabCeiling - effectiveFrom + 1;

            if (kmInThisSlab > 0) {
                long slabCost = (long) kmInThisSlab * slab.getRatePerKmPaisa();
                runningTotal += slabCost;
                segments.add(kmInThisSlab + "km@" + slab.getRatePerKmPaisa() + "p");
            }
        }

        sb.append(String.join(" + ", segments));
        sb.append(" = ").append(runningTotal).append("p");
        return sb.toString();
    }
}