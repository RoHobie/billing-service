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

    @Builder.Default
    @Column(name = "base_paisa", nullable = false)
    private long basePaisa = 0L;

    @Builder.Default
    @Column(name = "extra_charges_paisa", nullable = false)
    private long extraChargesPaisa = 0L;

    @Builder.Default
    @Column(name = "fixed_fee_share_paisa", nullable = false)
    private long fixedFeeSharePaisa = 0L;

    @Builder.Default
    @Column(name = "total_paisa", nullable = false)
    private long totalPaisa = 0L;

    @Column(name = "computation_note", length = 500)
    private String computationNote;
}
