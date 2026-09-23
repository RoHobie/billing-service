package com.rohobie.billing.service;

import com.rohobie.billing.domain.Contract;
import com.rohobie.billing.domain.ContractType;
import com.rohobie.billing.domain.Vehicle;
import com.rohobie.billing.domain.Vendor;
import com.rohobie.billing.exception.ResourceNotFoundException;
import com.rohobie.billing.repository.ContractRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContractLookupServiceTest {

    @Mock
    private ContractRepository contractRepository;

    @InjectMocks
    private ContractLookupService contractLookupService;

    @Test
    @DisplayName("Trip on Jan 14 with Contract A (Jan 1) and Contract B (Jan 15) returns Contract A")
    void findActiveContract_tripBeforeNewContract_returnsOldContract() {
        Vendor vendor = new Vendor(1L, "Vendor", "v@v.com");
        Vehicle vehicle = new Vehicle(1L, "KA-01-AB-1234", "Sedan", vendor);

        Contract contractA = new Contract(1L, vehicle, vendor, ContractType.PER_KM,
                0L, 0, LocalDate.of(2026, 1, 1), 0L, 0L);

        // On Jan 14, only contract A is <= 2026-01-14
        when(contractRepository.findActiveContractsOnDate(1L, LocalDate.of(2026, 1, 14)))
                .thenReturn(List.of(contractA));

        Contract active = contractLookupService.findActiveContract(1L, LocalDate.of(2026, 1, 14));
        assertThat(active.getId()).isEqualTo(1L);
        assertThat(active.getEffectiveFrom()).isEqualTo(LocalDate.of(2026, 1, 1));
    }

    @Test
    @DisplayName("Trip on Jan 16 with Contract A (Jan 1) and Contract B (Jan 15) returns Contract B")
    void findActiveContract_tripAfterNewContract_returnsNewContract() {
        Vendor vendor = new Vendor(1L, "Vendor", "v@v.com");
        Vehicle vehicle = new Vehicle(1L, "KA-01-AB-1234", "Sedan", vendor);

        Contract contractA = new Contract(1L, vehicle, vendor, ContractType.PER_KM,
                0L, 0, LocalDate.of(2026, 1, 1), 0L, 0L);
        Contract contractB = new Contract(2L, vehicle, vendor, ContractType.PER_KM,
                0L, 0, LocalDate.of(2026, 1, 15), 0L, 0L);

        // On Jan 16, both are <= 2026-01-16, ordered DESC -> contractB first
        when(contractRepository.findActiveContractsOnDate(1L, LocalDate.of(2026, 1, 16)))
                .thenReturn(List.of(contractB, contractA));

        Contract active = contractLookupService.findActiveContract(1L, LocalDate.of(2026, 1, 16));
        assertThat(active.getId()).isEqualTo(2L);
        assertThat(active.getEffectiveFrom()).isEqualTo(LocalDate.of(2026, 1, 15));
    }

    @Test
    @DisplayName("No matching contract throws ResourceNotFoundException")
    void findActiveContract_notFound_throwsException() {
        when(contractRepository.findActiveContractsOnDate(1L, LocalDate.of(2025, 12, 31)))
                .thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> contractLookupService.findActiveContract(1L, LocalDate.of(2025, 12, 31)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No active contract found for vehicle 1");
    }
}