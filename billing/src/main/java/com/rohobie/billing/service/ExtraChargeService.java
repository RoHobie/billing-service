package com.rohobie.billing.service;

import com.rohobie.billing.domain.Contract;
import com.rohobie.billing.domain.Trip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * ExtraChargeService
 *
 * Computes ancillary surcharges including night surcharge, waiting time charge,
 * and pass-through toll fees for a trip.
 *
 * Design decision: Toll fees are treated as exact pass-through with no markup.
 * Assumption: If waitingMinutes is null or 0, waiting charge is 0 paisa.
 */
@Service
public class ExtraChargeService {

    private static final Logger logger = LoggerFactory.getLogger(ExtraChargeService.class);

    /**
     * Computes the total ancillary extra charges in paisa for a given trip.
     *
     * @param trip the trip record
     * @param contract the active governing contract
     * @return total extra charges in paisa
     */
    @Transactional(readOnly = true)
    public long compute(Trip trip, Contract contract) {
        long nightCharge = trip.isHasNightCharge() ? contract.getNightChargePaisa() : 0L;
        int waitingMinutes = Optional.ofNullable(trip.getWaitingMinutes()).orElse(0);
        long waitingCharge = (long) waitingMinutes * contract.getWaitingRatePerMinutePaisa();
        long tollAmount = trip.getTollAmountPaisa();

        long total = nightCharge + waitingCharge + tollAmount;
        logger.debug("Trip ID: {} extra charges -> night: {}, waiting: {}, toll: {}, total: {}",
                trip.getId(), nightCharge, waitingCharge, tollAmount, total);
        return total;
    }

    /**
     * Formats an audit note snippet detailing extra charges.
     *
     * @param trip the trip record
     * @param contract the active contract
     * @return formatted extra charges note snippet
     */
    @Transactional(readOnly = true)
    public String buildExtraChargesNote(Trip trip, Contract contract) {
        List<String> parts = new ArrayList<>();
        if (trip.isHasNightCharge() && contract.getNightChargePaisa() > 0) {
            parts.add("night:" + contract.getNightChargePaisa() + "p");
        }
        int waitingMinutes = Optional.ofNullable(trip.getWaitingMinutes()).orElse(0);
        if (waitingMinutes > 0 && contract.getWaitingRatePerMinutePaisa() > 0) {
            long waitTotal = (long) waitingMinutes * contract.getWaitingRatePerMinutePaisa();
            parts.add("waiting:" + waitTotal + "p (" + waitingMinutes + "m@" + contract.getWaitingRatePerMinutePaisa() + "p/m)");
        }
        if (trip.getTollAmountPaisa() > 0) {
            parts.add("toll:" + trip.getTollAmountPaisa() + "p");
        }
        return parts.isEmpty() ? "" : String.join(" | ", parts);
    }
}