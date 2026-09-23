package com.rohobie.billing.dto.response;

import com.rohobie.billing.domain.ContractType;
import java.time.LocalDate;

public record ContractResponse(
        Long id,
        Long vehicleId,
        String vehicleRegistrationNumber,
        Long vendorId,
        String vendorName,
        ContractType contractType,
        long baseAmountPaisa,
        int freeKm,
        LocalDate effectiveFrom,
        long nightChargePaisa,
        long waitingRatePerMinutePaisa
) {
}