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

import java.time.LocalDateTime;

@Entity
@Table(name = "billing_run")
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

    public BillingRun() {
    }

    public BillingRun(Long id, Vehicle vehicle, String billingMonth, BillingRunStatus status, LocalDateTime runAt) {
        this.id = id;
        this.vehicle = vehicle;
        this.billingMonth = billingMonth;
        this.status = status;
        this.runAt = runAt;
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

    public String getBillingMonth() {
        return billingMonth;
    }

    public void setBillingMonth(String billingMonth) {
        this.billingMonth = billingMonth;
    }

    public BillingRunStatus getStatus() {
        return status;
    }

    public void setStatus(BillingRunStatus status) {
        this.status = status;
    }

    public LocalDateTime getRunAt() {
        return runAt;
    }

    public void setRunAt(LocalDateTime runAt) {
        this.runAt = runAt;
    }
}
