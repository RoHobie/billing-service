package com.rohobie.billing.service;

import com.rohobie.billing.domain.BillLineItem;
import com.rohobie.billing.domain.BillingRun;
import com.rohobie.billing.domain.BillingRunStatus;
import com.rohobie.billing.domain.Contract;
import com.rohobie.billing.domain.ContractType;
import com.rohobie.billing.domain.Trip;
import com.rohobie.billing.domain.Vehicle;
import com.rohobie.billing.dto.TripFareResult;
import com.rohobie.billing.dto.response.BillLineItemResponse;
import com.rohobie.billing.dto.response.BillingRunResponse;
import com.rohobie.billing.dto.response.BillingRunSummary;
import com.rohobie.billing.exception.ResourceNotFoundException;
import com.rohobie.billing.repository.BillLineItemRepository;
import com.rohobie.billing.repository.BillingRunRepository;
import com.rohobie.billing.repository.FraudFlagRepository;
import com.rohobie.billing.repository.TripRepository;
import com.rohobie.billing.repository.VehicleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * BillingRunService
 *
 * Orchestrates end-of-month billing calculations for a vehicle.
 * Ensures idempotent execution, per-trip fare computation, proportional
 * allocation of fixed monthly fees via Largest Remainder Method, and
 * persistence of auditable line items.
 *
 * Assumption: Billing month is formatted as YYYY-MM.
 * Design decision: Query-before-write idempotency key on (vehicleId, billingMonth).
 */
@Service
public class BillingRunService {

    private static final Logger logger = LoggerFactory.getLogger(BillingRunService.class);

    private final BillingRunRepository billingRunRepository;
    private final BillLineItemRepository billLineItemRepository;
    private final FraudFlagRepository fraudFlagRepository;
    private final VehicleRepository vehicleRepository;
    private final TripRepository tripRepository;
    private final TripBillingService tripBillingService;
    private final ContractLookupService contractLookupService;
    private final FixedFeeSplitService fixedFeeSplitService;
    private final FraudDetectionService fraudDetectionService;

    public BillingRunService(BillingRunRepository billingRunRepository,
                             BillLineItemRepository billLineItemRepository,
                             FraudFlagRepository fraudFlagRepository,
                             VehicleRepository vehicleRepository,
                             TripRepository tripRepository,
                             TripBillingService tripBillingService,
                             ContractLookupService contractLookupService,
                             FixedFeeSplitService fixedFeeSplitService,
                             FraudDetectionService fraudDetectionService) {
        this.billingRunRepository = billingRunRepository;
        this.billLineItemRepository = billLineItemRepository;
        this.fraudFlagRepository = fraudFlagRepository;
        this.vehicleRepository = vehicleRepository;
        this.tripRepository = tripRepository;
        this.tripBillingService = tripBillingService;
        this.contractLookupService = contractLookupService;
        this.fixedFeeSplitService = fixedFeeSplitService;
        this.fraudDetectionService = fraudDetectionService;
    }

    /**
     * Executes or idempotently retrieves a month-end billing run for a vehicle.
     *
     * @param vehicleId    the vehicle identifier
     * @param billingMonth the billing period in YYYY-MM format
     * @return BillingRunSummary containing run status, item count, and grand total in paisa
     */
    @org.springframework.cache.annotation.CacheEvict(value = "billingSummaries", allEntries = true)
    @Transactional
    public BillingRunSummary runBilling(Long vehicleId, String billingMonth) {
        logger.info("Starting billing run for vehicle ID: {} and month: {}", vehicleId, billingMonth);

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle with id " + vehicleId + " not found"));

        YearMonth yearMonth;
        try {
            yearMonth = YearMonth.parse(billingMonth);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Invalid billing month format. Expected YYYY-MM, received: " + billingMonth);
        }

        // Idempotency check:
        Optional<BillingRun> existingRunOpt = billingRunRepository.findByVehicleIdAndBillingMonth(vehicleId, billingMonth);
        BillingRun billingRun;

        if (existingRunOpt.isPresent()) {
            BillingRun existingRun = existingRunOpt.get();
            if (existingRun.getStatus() == BillingRunStatus.COMPLETED) {
                logger.info("Idempotent hit: COMPLETED billing run {} already exists for vehicle {} and month {}; returning existing summary",
                        existingRun.getId(), vehicleId, billingMonth);
                List<BillLineItem> existingItems = billLineItemRepository.findByBillingRunId(existingRun.getId());
                long grandTotalPaisa = existingItems.stream().mapToLong(BillLineItem::getTotalPaisa).sum();
                return new BillingRunSummary(existingRun.getId(), vehicleId, billingMonth,
                        existingRun.getStatus(), existingItems.size(), grandTotalPaisa);
            }

            // Stale or failed run: clear previous line items and fraud flags, reset to PENDING
            logger.info("Retrying existing {} billing run ID: {}; resetting stale line items",
                    existingRun.getStatus(), existingRun.getId());
            billLineItemRepository.deleteByBillingRunId(existingRun.getId());
            fraudFlagRepository.deleteByBillingRunId(existingRun.getId());
            existingRun.setStatus(BillingRunStatus.PENDING);
            existingRun.setRunAt(LocalDateTime.now());
            billingRun = billingRunRepository.save(existingRun);
        } else {
            billingRun = billingRunRepository.save(new BillingRun(
                    null, vehicle, billingMonth, BillingRunStatus.PENDING, LocalDateTime.now()));
        }

        // Fetch all trips for vehicle within billing month
        LocalDateTime startInclusive = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime endInclusive = yearMonth.atEndOfMonth().atTime(23, 59, 59, 999999999);
        List<Trip> trips = tripRepository.findByVehicleIdAndStartTimeBetweenOrderByStartTimeAsc(
                vehicleId, startInclusive, endInclusive);

        List<BillLineItem> lineItems = new ArrayList<>();
        Map<Contract, List<Trip>> fixedMonthlyTripsByContract = new HashMap<>();
        Map<Long, BillLineItem> lineItemByTripId = new HashMap<>();

        // Compute individual trip fares
        for (Trip trip : trips) {
            TripFareResult fareResult = tripBillingService.computeTripFare(trip);
            BillLineItem lineItem = new BillLineItem(
                    null,
                    billingRun,
                    trip,
                    fareResult.basePaisa(),
                    fareResult.extraChargesPaisa(),
                    0L,
                    fareResult.totalPaisa(),
                    fareResult.computationNote()
            );
            lineItems.add(lineItem);
            lineItemByTripId.put(trip.getId(), lineItem);

            // Check if governed by FIXED_MONTHLY contract
            Contract activeContract = contractLookupService.findActiveContract(
                    vehicleId, trip.getStartTime().toLocalDate());
            if (activeContract.getContractType() == ContractType.FIXED_MONTHLY) {
                fixedMonthlyTripsByContract.computeIfAbsent(activeContract, k -> new ArrayList<>()).add(trip);
            }
        }

        // Allocate fixed monthly fees via Largest Remainder Method
        for (Map.Entry<Contract, List<Trip>> entry : fixedMonthlyTripsByContract.entrySet()) {
            Contract contract = entry.getKey();
            List<Trip> contractTrips = entry.getValue();

            Map<Long, Long> feeShares = fixedFeeSplitService.split(contract.getBaseAmountPaisa(), contractTrips);
            for (Trip trip : contractTrips) {
                BillLineItem lineItem = lineItemByTripId.get(trip.getId());
                long share = feeShares.getOrDefault(trip.getId(), 0L);
                lineItem.setFixedFeeSharePaisa(share);
                lineItem.setTotalPaisa(lineItem.getBasePaisa() + lineItem.getExtraChargesPaisa() + share);
                lineItem.setComputationNote(lineItem.getComputationNote() + " | fixedShare: " + share + "p");
            }
        }

        // Persist all line items
        billLineItemRepository.saveAll(lineItems);

        // Run advisory fraud scan prior to completion
        fraudDetectionService.scan(billingRun.getId());

        // Finalize billing run status
        billingRun.setStatus(BillingRunStatus.COMPLETED);
        billingRun.setRunAt(LocalDateTime.now());
        billingRunRepository.save(billingRun);

        long grandTotalPaisa = lineItems.stream().mapToLong(BillLineItem::getTotalPaisa).sum();
        logger.info("Completed billing run ID: {} for vehicle ID: {} with grand total: {} paisa across {} items",
                billingRun.getId(), vehicleId, grandTotalPaisa, lineItems.size());

        return new BillingRunSummary(billingRun.getId(), vehicleId, billingMonth,
                billingRun.getStatus(), lineItems.size(), grandTotalPaisa);
    }

    /**
     * Retrieves full itemised details of a billing run.
     *
     * @param runId the billing run identifier
     * @return BillingRunResponse containing metadata and per-trip line items
     */
    @Transactional(readOnly = true)
    public BillingRunResponse getBillingRun(Long runId) {
        BillingRun billingRun = billingRunRepository.findById(runId)
                .orElseThrow(() -> new ResourceNotFoundException("Billing run with id " + runId + " not found"));

        List<BillLineItem> items = billLineItemRepository.findByBillingRunId(runId);
        List<BillLineItemResponse> itemResponses = items.stream().map(item -> new BillLineItemResponse(
                item.getId(),
                item.getTrip().getId(),
                item.getBasePaisa(),
                item.getExtraChargesPaisa(),
                item.getFixedFeeSharePaisa(),
                item.getTotalPaisa(),
                item.getComputationNote()
        )).collect(Collectors.toList());

        long grandTotalPaisa = itemResponses.stream().mapToLong(BillLineItemResponse::totalPaisa).sum();

        return new BillingRunResponse(
                billingRun.getId(),
                billingRun.getVehicle().getId(),
                billingRun.getBillingMonth(),
                billingRun.getStatus(),
                billingRun.getRunAt(),
                itemResponses.size(),
                grandTotalPaisa,
                itemResponses
        );
    }

    /**
     * Retrieves summary total and line item count for a billing run.
     *
     * @param runId the billing run identifier
     * @return BillingRunSummary DTO
     */
    @org.springframework.cache.annotation.Cacheable(value = "billingSummaries", key = "#runId")
    @Transactional(readOnly = true)
    public BillingRunSummary getBillingRunSummary(Long runId) {
        BillingRun billingRun = billingRunRepository.findById(runId)
                .orElseThrow(() -> new ResourceNotFoundException("Billing run with id " + runId + " not found"));

        List<BillLineItem> items = billLineItemRepository.findByBillingRunId(runId);
        long grandTotalPaisa = items.stream().mapToLong(BillLineItem::getTotalPaisa).sum();

        return new BillingRunSummary(
                billingRun.getId(),
                billingRun.getVehicle().getId(),
                billingRun.getBillingMonth(),
                billingRun.getStatus(),
                items.size(),
                grandTotalPaisa
        );
    }
}
