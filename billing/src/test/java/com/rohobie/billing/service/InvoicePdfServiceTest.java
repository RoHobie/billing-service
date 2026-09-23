package com.rohobie.billing.service;

import com.rohobie.billing.domain.BillLineItem;
import com.rohobie.billing.domain.BillingRun;
import com.rohobie.billing.domain.BillingRunStatus;
import com.rohobie.billing.domain.Trip;
import com.rohobie.billing.domain.Vehicle;
import com.rohobie.billing.domain.Vendor;
import com.rohobie.billing.exception.ResourceNotFoundException;
import com.rohobie.billing.repository.BillLineItemRepository;
import com.rohobie.billing.repository.BillingRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoicePdfServiceTest {

    @Mock
    private BillingRunRepository billingRunRepository;

    @Mock
    private BillLineItemRepository billLineItemRepository;

    @InjectMocks
    private InvoicePdfService invoicePdfService;

    private Vendor vendor;
    private Vehicle vehicle;
    private BillingRun billingRun;
    private Trip trip;

    @BeforeEach
    void setUp() {
        vendor = new Vendor(1L, "Apex Logistics", "billing@apex.com");
        vehicle = new Vehicle(1L, "KA-01-AB-1234", "Sedan", vendor);
        billingRun = new BillingRun(10L, vehicle, "2026-01", BillingRunStatus.COMPLETED, LocalDateTime.now());
        trip = new Trip(101L, vehicle, LocalDateTime.of(2026, 1, 10, 9, 0),
                LocalDateTime.of(2026, 1, 10, 11, 0), 45, false, false, 0, 0L);
    }

    @Test
    @DisplayName("Successfully generates non-empty PDF byte stream with valid PDF header")
    void generateInvoicePdf_validRun_returnsPdfBytes() {
        BillLineItem item = new BillLineItem(1L, billingRun, trip, 54000L, 0L, 10000L, 64000L, "Slab: 45km @ 1200p/km");
        when(billingRunRepository.findById(10L)).thenReturn(Optional.of(billingRun));
        when(billLineItemRepository.findByBillingRunId(10L)).thenReturn(List.of(item));

        byte[] pdf = invoicePdfService.generateInvoicePdf(10L);

        assertThat(pdf).isNotNull();
        assertThat(pdf.length).isGreaterThan(500);
        String pdfHeader = new String(pdf, 0, Math.min(pdf.length, 5));
        assertThat(pdfHeader).isEqualTo("%PDF-");
    }

    @Test
    @DisplayName("Throws ResourceNotFoundException when billing run does not exist")
    void generateInvoicePdf_notFound_throwsException() {
        when(billingRunRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoicePdfService.generateInvoicePdf(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Billing run with id 999 not found");
    }
}
