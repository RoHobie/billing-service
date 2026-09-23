package com.rohobie.billing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "fraud_flag")
public class FraudFlag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "billing_run_id", nullable = false)
    private BillingRun billingRun;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "trip_id")
    private Trip trip;

    @Enumerated(EnumType.STRING)
    @Column(name = "flag_type", nullable = false, length = 30)
    private FraudFlagType flagType;

    @Column(name = "description", nullable = false, length = 300)
    private String description;

    public FraudFlag() {
    }

    public FraudFlag(Long id, BillingRun billingRun, Trip trip, FraudFlagType flagType, String description) {
        this.id = id;
        this.billingRun = billingRun;
        this.trip = trip;
        this.flagType = flagType;
        this.description = description;
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

    public FraudFlagType getFlagType() {
        return flagType;
    }

    public void setFlagType(FraudFlagType flagType) {
        this.flagType = flagType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
