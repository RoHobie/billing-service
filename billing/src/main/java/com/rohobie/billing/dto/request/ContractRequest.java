package com.rohobie.billing.dto.request;

import com.rohobie.billing.domain.ContractType;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record ContractRequest(
        @NotNull(message = "Vehicle ID is required")
        Long vehicleId,
        @NotNull(message = "Vendor ID is required")
        Long vendorId,
        @NotNull(message = "Contract type is required")
        ContractType contractType,
        long baseAmountPaisa,
        int freeKm,
        @NotNull(message = "Effective from date is required")
        LocalDate effectiveFrom,
        long nightChargePaisa,
        long waitingRatePerMinutePaisa
) {
}