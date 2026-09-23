package com.rohobie.billing.service;

import com.rohobie.billing.domain.Contract;
import com.rohobie.billing.domain.Trip;
import com.rohobie.billing.dto.TripFareResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * TripBillingService
 *
 * Orchestrates fare computation for an individual trip by resolving its active contract version,
 * computing base slab/flat fare, computing extra surcharges, and producing a traceable audit note.
 *
 * Assumption: Dead-leg trips receive 0 base fare, but remain billable for any valid extra charges.
 * Design decision: The generated computationNote is stored verbatim on BillLineItem for auditing.
 */
@Slf4j
@Service
public class TripBillingService {

    private final ContractLookupService contractLookupService;
    private final SlabComputationService slabComputationService;
    private final ExtraChargeService extraChargeService;

    public TripBillingService(ContractLookupService contractLookupService,
                              SlabComputationService slabComputationService,
                              ExtraChargeService extraChargeService) {
        this.contractLookupService = contractLookupService;
        this.slabComputationService = slabComputationService;
        this.extraChargeService = extraChargeService;
    }

    /**
     * Computes the complete fare and audit trace for an individual vehicle trip.
     *
     * @param trip the trip record to evaluate
     * @return TripFareResult containing base fare, extra charges, and audit trail note
     */
    @Transactional(readOnly = true)
    public TripFareResult computeTripFare(Trip trip) {
        log.debug("Computing fare for trip ID: {} distance: {}km", trip.getId(), trip.getDistanceKm());

        // 1. Resolve active contract for this trip's vehicle as of the trip's start date
        Contract activeContract = contractLookupService.findActiveContract(
                trip.getVehicle().getId(),
                trip.getStartTime().toLocalDate()
        );

        // 2. Compute base fare
        // ASSUMPTION: Dead-leg trips have 0 base fare per spec (empty repositioning run)
        long basePaisa;
        String baseNote;
        if (trip.isDeadLeg()) {
            basePaisa = 0L;
            baseNote = "DEAD_LEG: base = 0p";
        } else {
            basePaisa = slabComputationService.compute(trip, activeContract);
            baseNote = slabComputationService.buildSlabBreakdownNote(trip, activeContract);
        }

        // 3. Compute extra charges (night, waiting, tolls)
        long extraChargesPaisa = extraChargeService.compute(trip, activeContract);
        String extrasNote = extraChargeService.buildExtraChargesNote(trip, activeContract);

        // 4. Assemble human-readable computation audit note
        String computationNote = extrasNote.isEmpty()
                ? baseNote
                : baseNote + " | " + extrasNote;

        log.debug("Trip ID: {} completed fare computation -> base: {}p, extras: {}p, note: {}",
                trip.getId(), basePaisa, extraChargesPaisa, computationNote);

        return new TripFareResult(basePaisa, extraChargesPaisa, computationNote);
    }
}