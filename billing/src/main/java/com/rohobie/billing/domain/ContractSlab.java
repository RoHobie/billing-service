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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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

    public ContractSlab(Contract contract, int fromKm, Integer toKm, long ratePerKmPaisa) {
        this.contract = contract;
        this.fromKm = fromKm;
        this.toKm = toKm;
        this.ratePerKmPaisa = ratePerKmPaisa;
    }
}