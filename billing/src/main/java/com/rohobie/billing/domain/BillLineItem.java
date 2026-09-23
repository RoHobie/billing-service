package com.rohobie.billing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "bill_line_item")
public class BillLineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "billing_run_id", nullable = false)
    private BillingRun billingRun;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Column(name = "base_paisa", nullable = false)
    private long basePaisa = 0L;

    @Column(name = "extra_charges_paisa", nullable = false)
    private long extraChargesPaisa = 0L;

    @Column(name = "fixed_fee_share_paisa", nullable = false)
    private long fixedFeeSharePaisa = 0L;

    @Column(name = "total_paisa", nullable = false)
    private long totalPaisa = 0L;

    @Column(name = "computation_note", length = 500)
    private String computationNote;

    public BillLineItem() {
    }

    public BillLineItem(Long id, BillingRun billingRun, Trip trip, long basePaisa,
                        long extraChargesPaisa, long fixedFeeSharePaisa, long totalPaisa,
                        String computationNote) {
        this.id = id;
        this.billingRun = billingRun;
        this.trip = trip;
        this.basePaisa = basePaisa;
        this.extraChargesPaisa = extraChargesPaisa;
        this.fixedFeeSharePaisa = fixedFeeSharePaisa;
        this.totalPaisa = totalPaisa;
        this.computationNote = computationNote;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BillingRun getBillingRun() {
        return billingRun;
    }

    public void setBillingRun(BillingRun billingRun) {
        this.billingRun = billingRun;
    }

    public Trip getTrip() {
        return trip;
    }

    public void setTrip(Trip trip) {
        this.trip = trip;
    }

    public long getBasePaisa() {
        return basePaisa;
    }

    public void setBasePaisa(long basePaisa) {
        this.basePaisa = basePaisa;
    }

    public long getExtraChargesPaisa() {
        return extraChargesPaisa;
    }

    public void setExtraChargesPaisa(long extraChargesPaisa) {
        this.extraChargesPaisa = extraChargesPaisa;
    }

    public long getFixedFeeSharePaisa() {
        return fixedFeeSharePaisa;
    }

    public void setFixedFeeSharePaisa(long fixedFeeSharePaisa) {
        this.fixedFeeSharePaisa = fixedFeeSharePaisa;
    }

    public long getTotalPaisa() {
        return totalPaisa;
    }

    public void setTotalPaisa(long totalPaisa) {
        this.totalPaisa = totalPaisa;
    }

    public String getComputationNote() {
        return computationNote;
    }

    public void setComputationNote(String computationNote) {
        this.computationNote = computationNote;
    }
}
