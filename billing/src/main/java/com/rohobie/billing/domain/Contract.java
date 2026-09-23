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
import java.time.LocalDate;

@Entity
@Table(name = "contract")
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @Enumerated(EnumType.STRING)
    @Column(name = "contract_type", nullable = false, length = 20)
    private ContractType contractType;

    @Column(name = "base_amount_paisa", nullable = false)
    private long baseAmountPaisa = 0L;

    @Column(name = "free_km", nullable = false)
    private int freeKm = 0;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "night_charge_paisa", nullable = false)
    private long nightChargePaisa = 0L;

    @Column(name = "waiting_rate_per_minute_paisa", nullable = false)
    private long waitingRatePerMinutePaisa = 0L;

    public Contract() {
    }

    public Contract(Long id, Vehicle vehicle, Vendor vendor, ContractType contractType,
                    long baseAmountPaisa, int freeKm, LocalDate effectiveFrom,
                    long nightChargePaisa, long waitingRatePerMinutePaisa) {
        this.id = id;
        this.vehicle = vehicle;
        this.vendor = vendor;
        this.contractType = contractType;
        this.baseAmountPaisa = baseAmountPaisa;
        this.freeKm = freeKm;
        this.effectiveFrom = effectiveFrom;
        this.nightChargePaisa = nightChargePaisa;
        this.waitingRatePerMinutePaisa = waitingRatePerMinutePaisa;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }

    public Vendor getVendor() {
        return vendor;
    }

    public void setVendor(Vendor vendor) {
        this.vendor = vendor;
    }

    public ContractType getContractType() {
        return contractType;
    }

    public void setContractType(ContractType contractType) {
        this.contractType = contractType;
    }

    public long getBaseAmountPaisa() {
        return baseAmountPaisa;
    }

    public void setBaseAmountPaisa(long baseAmountPaisa) {
        this.baseAmountPaisa = baseAmountPaisa;
    }

    public int getFreeKm() {
        return freeKm;
    }

    public void setFreeKm(int freeKm) {
        this.freeKm = freeKm;
    }

    public LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(LocalDate effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public long getNightChargePaisa() {
        return nightChargePaisa;
    }

    public void setNightChargePaisa(long nightChargePaisa) {
        this.nightChargePaisa = nightChargePaisa;
    }

    public long getWaitingRatePerMinutePaisa() {
        return waitingRatePerMinutePaisa;
    }

    public void setWaitingRatePerMinutePaisa(long waitingRatePerMinutePaisa) {
        this.waitingRatePerMinutePaisa = waitingRatePerMinutePaisa;
    }
}