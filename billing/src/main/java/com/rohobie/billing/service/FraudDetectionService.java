package com.rohobie.billing.service;

import com.rohobie.billing.domain.BillLineItem;
import com.rohobie.billing.domain.BillingRun;
import com.rohobie.billing.domain.FraudFlag;
import com.rohobie.billing.domain.FraudFlagType;
import com.rohobie.billing.domain.Trip;
import com.rohobie.billing.dto.response.FraudFlagResponse;
import com.rohobie.billing.exception.ResourceNotFoundException;
import com.rohobie.billing.repository.BillLineItemRepository;
import com.rohobie.billing.repository.BillingRunRepository;
import com.rohobie.billing.repository.FraudFlagRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * FraudDetectionService
 *
 * Scans generated billing run line items for anomalous patterns such as
 * duplicate trip billing, impossible single-trip distances (> 500 km),
 * and orphan trips missing contract coverage.
 *
 * Assumption: Impossible distance threshold is fixed at 500 km per spec.
 * Design decision: Fraud flags are purely advisory and do not abort billing run completion.
 */
@Service
public class FraudDetectionService {

    private static final Logger logger = LoggerFactory.getLogger(FraudDetectionService.class);
    public static final int IMPOSSIBLE_DISTANCE_THRESHOLD_KM = 500;

    private final BillingRunRepository billingRunRepository;
    private final BillLineItemRepository billLineItemRepository;
    private final FraudFlagRepository fraudFlagRepository;
    private final ContractLookupService contractLookupService;

    public FraudDetectionService(BillingRunRepository billingRunRepository,
                                 BillLineItemRepository billLineItemRepository,
                                 FraudFlagRepository fraudFlagRepository,
                                 ContractLookupService contractLookupService) {
        this.billingRunRepository = billingRunRepository;
        this.billLineItemRepository = billLineItemRepository;
        this.fraudFlagRepository = fraudFlagRepository;
        this.contractLookupService = contractLookupService;
    }

    /**
     * Scans all line items of the specified billing run and records advisory fraud flags.
     *
     * @param billingRunId the unique identifier of the billing run
     */
    @Transactional
    public void scan(Long billingRunId) {
        BillingRun billingRun = billingRunRepository.findById(billingRunId)
                .orElseThrow(() -> new ResourceNotFoundException("Billing run with id " + billingRunId + " not found"));

        List<BillLineItem> lineItems = billLineItemRepository.findByBillingRunId(billingRunId);
        List<FraudFlag> flagsToSave = new ArrayList<>();

        Set<Long> seenTripIds = new HashSet<>();
        Set<Long> duplicateTripIds = new HashSet<>();

        for (BillLineItem item : lineItems) {
            Trip trip = item.getTrip();
            if (trip == null) {
                continue;
            }

            Long tripId = trip.getId();

            // Check 1: Duplicate Trip
            if (tripId != null && !seenTripIds.add(tripId)) {
                if (duplicateTripIds.add(tripId)) {
                    String desc = String.format("Trip %d appears more than once in billing run %d.", tripId, billingRunId);
                    logger.warn("Fraud alert - Duplicate trip detected: {}", desc);
                    flagsToSave.add(new FraudFlag(null, billingRun, trip, FraudFlagType.DUPLICATE_TRIP, desc));
                }
            }

            // Check 2: Impossible Distance (> 500 km)
            if (trip.getDistanceKm() > IMPOSSIBLE_DISTANCE_THRESHOLD_KM) {
                String desc = String.format("Trip %d: %d km in a single trip exceeds threshold of %d km.",
                        tripId, trip.getDistanceKm(), IMPOSSIBLE_DISTANCE_THRESHOLD_KM);
                logger.warn("Fraud alert - Impossible distance detected: {}", desc);
                flagsToSave.add(new FraudFlag(null, billingRun, trip, FraudFlagType.IMPOSSIBLE_DISTANCE, desc));
            }

            // Check 3: Orphan Trip (vehicle has no active contract for trip date)
            try {
                contractLookupService.findActiveContract(trip.getVehicle().getId(), trip.getStartTime().toLocalDate());
            } catch (ResourceNotFoundException ex) {
                String desc = String.format("Trip %d: Vehicle %d has no active contract for date %s.",
                        tripId, trip.getVehicle().getId(), trip.getStartTime().toLocalDate());
                logger.warn("Fraud alert - Orphan trip detected: {}", desc);
                flagsToSave.add(new FraudFlag(null, billingRun, trip, FraudFlagType.ORPHAN_TRIP, desc));
            }
        }

        if (!flagsToSave.isEmpty()) {
            fraudFlagRepository.saveAll(flagsToSave);
            logger.warn("Persisted {} advisory fraud flag(s) for billing run ID: {}", flagsToSave.size(), billingRunId);
        } else {
            logger.debug("Fraud scan completed cleanly with 0 flags for billing run ID: {}", billingRunId);
        }
    }

    /**
     * Retrieves all recorded fraud flags for a given billing run.
     *
     * @param billingRunId the billing run identifier
     * @return list of fraud flag response DTOs
     */
    @Transactional(readOnly = true)
    public List<FraudFlagResponse> getFlagsForRun(Long billingRunId) {
        return fraudFlagRepository.findByBillingRunId(billingRunId).stream()
                .map(flag -> new FraudFlagResponse(
                        flag.getId(),
                        flag.getBillingRun().getId(),
                        flag.getTrip() != null ? flag.getTrip().getId() : null,
                        flag.getFlagType(),
                        flag.getDescription()
                ))
                .collect(Collectors.toList());
    }
}
