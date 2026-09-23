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
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "billing_run", uniqueConstraints = {
        @UniqueConstraint(name = "uk_billing_run_vehicle_month", columnNames = {"vehicle_id", "billing_month"})
})
public class BillingRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(name = "billing_month", nullable = false, length = 7)
    private String billingMonth;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15)
    private BillingRunStatus status;

    @Column(name = "run_at", nullable = false)
    private LocalDateTime runAt;

    @Version
    private Long version;

    public BillingRun(Long id, Vehicle vehicle, String billingMonth, BillingRunStatus status, LocalDateTime runAt) {
        this.id = id;
        this.vehicle = vehicle;
        this.billingMonth = billingMonth;
        this.status = status;
        this.runAt = runAt;
    }
}
