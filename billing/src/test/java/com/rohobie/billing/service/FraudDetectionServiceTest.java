package com.rohobie.billing.service;

import com.rohobie.billing.domain.BillLineItem;
import com.rohobie.billing.domain.BillingRun;
import com.rohobie.billing.domain.BillingRunStatus;
import com.rohobie.billing.domain.Contract;
import com.rohobie.billing.domain.ContractType;
import com.rohobie.billing.domain.FraudFlag;
import com.rohobie.billing.domain.FraudFlagType;
import com.rohobie.billing.domain.Trip;
import com.rohobie.billing.domain.Vehicle;
import com.rohobie.billing.domain.Vendor;
import com.rohobie.billing.dto.response.FraudFlagResponse;
import com.rohobie.billing.exception.ResourceNotFoundException;
import com.rohobie.billing.repository.BillLineItemRepository;
import com.rohobie.billing.repository.BillingRunRepository;
import com.rohobie.billing.repository.FraudFlagRepository;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FraudDetectionServiceTest {

    @Mock
    private BillingRunRepository billingRunRepository;

    @Mock
    private BillLineItemRepository billLineItemRepository;

    @Mock
    private FraudFlagRepository fraudFlagRepository;

    @Mock
    private ContractLookupService contractLookupService;

    @InjectMocks
    private FraudDetectionService fraudDetectionService;

    private Vehicle vehicle;
    private BillingRun billingRun;
    private Contract contract;

    @BeforeEach
    void setUp() {
        Vendor vendor = new Vendor(1L, "Metro Rentals", "info@metro.com");
        vehicle = new Vehicle(1L, "KA-01-HH-1234", "Sedan", vendor);
        billingRun = new BillingRun(10L, vehicle, "2026-01", BillingRunStatus.PENDING, LocalDateTime.now());
        contract = new Contract(1L, vehicle, vendor, ContractType.PER_KM, 0L, 0, LocalDate.of(2026, 1, 1), 0L, 0L);
    }

    @Test
    @DisplayName("Clean billing run with normal trips records zero fraud flags")
    void scan_cleanRun_recordsZeroFlags() {
        when(billingRunRepository.findById(10L)).thenReturn(Optional.of(billingRun));

        Trip trip1 = new Trip(101L, vehicle, LocalDateTime.of(2026, 1, 5, 10, 0),
                LocalDateTime.of(2026, 1, 5, 12, 0), 120, false, false, 0, 0L);
        BillLineItem item1 = new BillLineItem(1L, billingRun, trip1, 120000L, 0L, 0L, 120000L, "note");

        when(billLineItemRepository.findByBillingRunId(10L)).thenReturn(List.of(item1));
        when(contractLookupService.findActiveContract(1L, LocalDate.of(2026, 1, 5))).thenReturn(contract);

        fraudDetectionService.scan(10L);

        verify(fraudFlagRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Trip with distance > 500 km flags IMPOSSIBLE_DISTANCE")
    void scan_impossibleDistance_savesFlag() {
        when(billingRunRepository.findById(10L)).thenReturn(Optional.of(billingRun));

        Trip longTrip = new Trip(102L, vehicle, LocalDateTime.of(2026, 1, 10, 8, 0),
                LocalDateTime.of(2026, 1, 10, 20, 0), 700, false, false, 0, 0L);
        BillLineItem item = new BillLineItem(2L, billingRun, longTrip, 700000L, 0L, 0L, 700000L, "note");

        when(billLineItemRepository.findByBillingRunId(10L)).thenReturn(List.of(item));
        when(contractLookupService.findActiveContract(1L, LocalDate.of(2026, 1, 10))).thenReturn(contract);

        fraudDetectionService.scan(10L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<FraudFlag>> captor = ArgumentCaptor.forClass(List.class);
        verify(fraudFlagRepository).saveAll(captor.capture());
        List<FraudFlag> savedFlags = captor.getValue();

        assertThat(savedFlags).hasSize(1);
        FraudFlag flag = savedFlags.get(0);
        assertThat(flag.getFlagType()).isEqualTo(FraudFlagType.IMPOSSIBLE_DISTANCE);
        assertThat(flag.getDescription()).contains("700 km in a single trip exceeds threshold of 500 km");
    }

    @Test
    @DisplayName("Duplicate trip in same billing run flags DUPLICATE_TRIP")
    void scan_duplicateTrip_savesFlag() {
        when(billingRunRepository.findById(10L)).thenReturn(Optional.of(billingRun));

        Trip trip = new Trip(103L, vehicle, LocalDateTime.of(2026, 1, 12, 9, 0),
                LocalDateTime.of(2026, 1, 12, 10, 0), 50, false, false, 0, 0L);
        BillLineItem item1 = new BillLineItem(3L, billingRun, trip, 50000L, 0L, 0L, 50000L, "note1");
        BillLineItem item2 = new BillLineItem(4L, billingRun, trip, 50000L, 0L, 0L, 50000L, "note2");

        when(billLineItemRepository.findByBillingRunId(10L)).thenReturn(List.of(item1, item2));
        when(contractLookupService.findActiveContract(1L, LocalDate.of(2026, 1, 12))).thenReturn(contract);

        fraudDetectionService.scan(10L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<FraudFlag>> captor = ArgumentCaptor.forClass(List.class);
        verify(fraudFlagRepository).saveAll(captor.capture());
        List<FraudFlag> savedFlags = captor.getValue();

        assertThat(savedFlags).hasSize(1);
        FraudFlag flag = savedFlags.get(0);
        assertThat(flag.getFlagType()).isEqualTo(FraudFlagType.DUPLICATE_TRIP);
        assertThat(flag.getDescription()).contains("appears more than once in billing run 10");
    }

    @Test
    @DisplayName("Trip without active contract flags ORPHAN_TRIP")
    void scan_orphanTrip_savesFlag() {
        when(billingRunRepository.findById(10L)).thenReturn(Optional.of(billingRun));

        Trip orphanTrip = new Trip(104L, vehicle, LocalDateTime.of(2026, 1, 15, 9, 0),
                LocalDateTime.of(2026, 1, 15, 10, 0), 30, false, false, 0, 0L);
        BillLineItem item = new BillLineItem(5L, billingRun, orphanTrip, 0L, 0L, 0L, 0L, "orphan");

        when(billLineItemRepository.findByBillingRunId(10L)).thenReturn(List.of(item));
        when(contractLookupService.findActiveContract(1L, LocalDate.of(2026, 1, 15)))
                .thenThrow(new ResourceNotFoundException("No active contract found"));

        fraudDetectionService.scan(10L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<FraudFlag>> captor = ArgumentCaptor.forClass(List.class);
        verify(fraudFlagRepository).saveAll(captor.capture());
        List<FraudFlag> savedFlags = captor.getValue();

        assertThat(savedFlags).hasSize(1);
        FraudFlag flag = savedFlags.get(0);
        assertThat(flag.getFlagType()).isEqualTo(FraudFlagType.ORPHAN_TRIP);
        assertThat(flag.getDescription()).contains("has no active contract");
    }

    @Test
    @DisplayName("getFlagsForRun returns mapped FraudFlagResponse DTOs")
    void getFlagsForRun_returnsDtoList() {
        FraudFlag flag = new FraudFlag(1L, billingRun, null, FraudFlagType.IMPOSSIBLE_DISTANCE, "Flag desc");
        when(fraudFlagRepository.findByBillingRunId(10L)).thenReturn(List.of(flag));

        List<FraudFlagResponse> responses = fraudDetectionService.getFlagsForRun(10L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).flagType()).isEqualTo(FraudFlagType.IMPOSSIBLE_DISTANCE);
        assertThat(responses.get(0).description()).isEqualTo("Flag desc");
    }
}
