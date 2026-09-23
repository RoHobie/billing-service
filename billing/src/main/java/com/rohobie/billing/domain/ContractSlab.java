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
@Table(name = "contract_slab")
public class ContractSlab {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    @Column(name = "from_km", nullable = false)
    private int fromKm;

    @Column(name = "to_km")
    private Integer toKm;

    @Column(name = "rate_per_km_paisa", nullable = false)
    private long ratePerKmPaisa;

    public ContractSlab() {
    }

    public ContractSlab(Long id, Contract contract, int fromKm, Integer toKm, long ratePerKmPaisa) {
        this.id = id;
        this.contract = contract;
        this.fromKm = fromKm;
        this.toKm = toKm;
        this.ratePerKmPaisa = ratePerKmPaisa;
    }

    public ContractSlab(Contract contract, int fromKm, Integer toKm, long ratePerKmPaisa) {
        this.contract = contract;
        this.fromKm = fromKm;
        this.toKm = toKm;
        this.ratePerKmPaisa = ratePerKmPaisa;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Contract getContract() {
        return contract;
    }

    public void setContract(Contract contract) {
        this.contract = contract;
    }

    public int getFromKm() {
        return fromKm;
    }

    public void setFromKm(int fromKm) {
        this.fromKm = fromKm;
    }

    public Integer getToKm() {
        return toKm;
    }

    public void setToKm(Integer toKm) {
        this.toKm = toKm;
    }

    public long getRatePerKmPaisa() {
        return ratePerKmPaisa;
    }

    public void setRatePerKmPaisa(long ratePerKmPaisa) {
        this.ratePerKmPaisa = ratePerKmPaisa;
    }
}