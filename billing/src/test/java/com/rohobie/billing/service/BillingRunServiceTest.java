package com.rohobie.billing.service;

import com.rohobie.billing.domain.BillLineItem;
import com.rohobie.billing.domain.BillingRun;
import com.rohobie.billing.domain.BillingRunStatus;
import com.rohobie.billing.domain.Contract;
import com.rohobie.billing.domain.ContractType;
import com.rohobie.billing.domain.Trip;
import com.rohobie.billing.domain.Vehicle;
import com.rohobie.billing.domain.Vendor;
import com.rohobie.billing.dto.TripFareResult;
import com.rohobie.billing.dto.response.BillingRunSummary;
import com.rohobie.billing.exception.ResourceNotFoundException;
import com.rohobie.billing.repository.BillLineItemRepository;
import com.rohobie.billing.repository.BillingRunRepository;
import com.rohobie.billing.repository.FraudFlagRepository;
import com.rohobie.billing.repository.TripRepository;
import com.rohobie.billing.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillingRunServiceTest {

    @Mock
    private BillingRunRepository billingRunRepository;

    @Mock
    private BillLineItemRepository billLineItemRepository;

    @Mock
    private FraudFlagRepository fraudFlagRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private TripRepository tripRepository;

    @Mock
    private TripBillingService tripBillingService;

    @Mock
    private ContractLookupService contractLookupService;

    @Mock
    private FixedFeeSplitService fixedFeeSplitService;

    @InjectMocks
    private BillingRunService billingRunService;

    private Vendor vendor;
    private Vehicle vehicle;

    @BeforeEach
    void setUp() {
        vendor = new Vendor(1L, "Metro Rentals", "info@metro.com");
        vehicle = new Vehicle(1L, "KA-01-HH-1234", "Sedan", vendor);
    }

    @Test
    @DisplayName("Initial run computes trip fares, creates line items, and marks run COMPLETED")
    void runBilling_perKmTrips_completesAndSavesLineItems() {
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle));
        when(billingRunRepository.findByVehicleIdAndBillingMonth(1L, "2026-01")).thenReturn(Optional.empty());

        BillingRun pendingRun = new BillingRun(10L, vehicle, "2026-01", BillingRunStatus.PENDING, LocalDateTime.now());
        when(billingRunRepository.save(any(BillingRun.class))).thenReturn(pendingRun);

        Contract perKmContract = new Contract(1L, vehicle, vendor, ContractType.PER_KM,
                0L, 0, LocalDate.of(2026, 1, 1), 0L, 0L);

        Trip trip1 = new Trip(101L, vehicle, LocalDateTime.of(2026, 1, 5, 10, 0),
                LocalDateTime.of(2026, 1, 5, 12, 0), 100, false, false, 0, 0L);
        Trip trip2 = new Trip(102L, vehicle, LocalDateTime.of(2026, 1, 15, 10, 0),
                LocalDateTime.of(2026, 1, 15, 12, 0), 50, false, false, 0, 0L);

        when(tripRepository.findByVehicleIdAndStartTimeBetweenOrderByStartTimeAsc(eq(1L), any(), any()))
                .thenReturn(List.of(trip1, trip2));

        when(tripBillingService.computeTripFare(trip1))
                .thenReturn(new TripFareResult(120000L, 0L, "100km @ 1200p"));
        when(tripBillingService.computeTripFare(trip2))
                .thenReturn(new TripFareResult(60000L, 0L, "50km @ 1200p"));

        when(contractLookupService.findActiveContract(1L, LocalDate.of(2026, 1, 5))).thenReturn(perKmContract);
        when(contractLookupService.findActiveContract(1L, LocalDate.of(2026, 1, 15))).thenReturn(perKmContract);

        BillingRunSummary summary = billingRunService.runBilling(1L, "2026-01");

        assertThat(summary.runId()).isEqualTo(10L);
        assertThat(summary.status()).isEqualTo(BillingRunStatus.COMPLETED);
        assertThat(summary.lineItemCount()).isEqualTo(2);
        assertThat(summary.grandTotalPaisa()).isEqualTo(180000L);

        verify(billLineItemRepository).saveAll(any());
    }

    @Test
    @DisplayName("Idempotency: existing COMPLETED run returns saved summary immediately without recomputation")
    void runBilling_existingCompletedRun_returnsImmediately() {
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle));

        BillingRun completedRun = new BillingRun(20L, vehicle, "2026-01", BillingRunStatus.COMPLETED, LocalDateTime.now());
        when(billingRunRepository.findByVehicleIdAndBillingMonth(1L, "2026-01"))
                .thenReturn(Optional.of(completedRun));

        BillLineItem item1 = new BillLineItem(1L, completedRun, null, 100000L, 0L, 0L, 100000L, "note1");
        BillLineItem item2 = new BillLineItem(2L, completedRun, null, 50000L, 0L, 0L, 50000L, "note2");
        when(billLineItemRepository.findByBillingRunId(20L)).thenReturn(List.of(item1, item2));

        BillingRunSummary summary = billingRunService.runBilling(1L, "2026-01");

        assertThat(summary.runId()).isEqualTo(20L);
        assertThat(summary.status()).isEqualTo(BillingRunStatus.COMPLETED);
        assertThat(summary.lineItemCount()).isEqualTo(2);
        assertThat(summary.grandTotalPaisa()).isEqualTo(150000L);

        // Verify zero trip fare calls and no database saves
        verify(tripBillingService, never()).computeTripFare(any());
        verify(billLineItemRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("FIXED_MONTHLY contract splits base amount across trips using Largest Remainder")
    void runBilling_fixedMonthlyContract_allocatesFeeAcrossTrips() {
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle));
        when(billingRunRepository.findByVehicleIdAndBillingMonth(1L, "2026-01")).thenReturn(Optional.empty());

        BillingRun pendingRun = new BillingRun(30L, vehicle, "2026-01", BillingRunStatus.PENDING, LocalDateTime.now());
        when(billingRunRepository.save(any(BillingRun.class))).thenReturn(pendingRun);

        long monthlyFeePaisa = 3000000L; // ₹30,000
        Contract fixedContract = new Contract(2L, vehicle, vendor, ContractType.FIXED_MONTHLY,
                monthlyFeePaisa, 0, LocalDate.of(2026, 1, 1), 0L, 0L);

        Trip trip1 = new Trip(201L, vehicle, LocalDateTime.of(2026, 1, 10, 10, 0),
                LocalDateTime.of(2026, 1, 10, 12, 0), 100, false, false, 0, 0L);
        Trip trip2 = new Trip(202L, vehicle, LocalDateTime.of(2026, 1, 20, 10, 0),
                LocalDateTime.of(2026, 1, 20, 12, 0), 200, false, false, 0, 0L);

        when(tripRepository.findByVehicleIdAndStartTimeBetweenOrderByStartTimeAsc(eq(1L), any(), any()))
                .thenReturn(List.of(trip1, trip2));

        when(tripBillingService.computeTripFare(trip1))
                .thenReturn(new TripFareResult(0L, 0L, "FIXED_MONTHLY: base = 0p"));
        when(tripBillingService.computeTripFare(trip2))
                .thenReturn(new TripFareResult(0L, 0L, "FIXED_MONTHLY: base = 0p"));

        when(contractLookupService.findActiveContract(1L, LocalDate.of(2026, 1, 10))).thenReturn(fixedContract);
        when(contractLookupService.findActiveContract(1L, LocalDate.of(2026, 1, 20))).thenReturn(fixedContract);

        // 100km and 200km split 30,000 INR (10,000 and 20,000)
        when(fixedFeeSplitService.split(monthlyFeePaisa, List.of(trip1, trip2)))
                .thenReturn(Map.of(201L, 1000000L, 202L, 2000000L));

        BillingRunSummary summary = billingRunService.runBilling(1L, "2026-01");

        assertThat(summary.grandTotalPaisa()).isEqualTo(monthlyFeePaisa);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<BillLineItem>> captor = ArgumentCaptor.forClass(List.class);
        verify(billLineItemRepository).saveAll(captor.capture());
        List<BillLineItem> savedItems = captor.getValue();

        assertThat(savedItems).hasSize(2);
        assertThat(savedItems.get(0).getFixedFeeSharePaisa()).isEqualTo(1000000L);
        assertThat(savedItems.get(0).getTotalPaisa()).isEqualTo(1000000L);
        assertThat(savedItems.get(0).getComputationNote()).contains("fixedShare: 1000000p");

        assertThat(savedItems.get(1).getFixedFeeSharePaisa()).isEqualTo(2000000L);
        assertThat(savedItems.get(1).getTotalPaisa()).isEqualTo(2000000L);
        assertThat(savedItems.get(1).getComputationNote()).contains("fixedShare: 2000000p");
    }

    @Test
    @DisplayName("Stale FAILED run clears previous items and retries cleanly")
    void runBilling_failedRun_clearsOldItemsAndRetries() {
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle));

        BillingRun failedRun = new BillingRun(40L, vehicle, "2026-01", BillingRunStatus.FAILED, LocalDateTime.now());
        when(billingRunRepository.findByVehicleIdAndBillingMonth(1L, "2026-01"))
                .thenReturn(Optional.of(failedRun));
        when(billingRunRepository.save(failedRun)).thenReturn(failedRun);
        when(tripRepository.findByVehicleIdAndStartTimeBetweenOrderByStartTimeAsc(eq(1L), any(), any()))
                .thenReturn(List.of());

        BillingRunSummary summary = billingRunService.runBilling(1L, "2026-01");

        verify(billLineItemRepository).deleteByBillingRunId(40L);
        verify(fraudFlagRepository).deleteByBillingRunId(40L);
        assertThat(summary.status()).isEqualTo(BillingRunStatus.COMPLETED);
    }

    @Test
    @DisplayName("Unknown vehicle throws ResourceNotFoundException")
    void runBilling_unknownVehicle_throwsException() {
        when(vehicleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> billingRunService.runBilling(99L, "2026-01"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Vehicle with id 99 not found");
    }

    @Test
    @DisplayName("Invalid billing month string throws IllegalArgumentException")
    void runBilling_invalidMonthFormat_throwsException() {
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle));

        assertThatThrownBy(() -> billingRunService.runBilling(1L, "2026-13"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid billing month format");
    }
}
