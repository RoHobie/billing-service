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
import java.time.LocalDateTime;

@Entity
@Table(name = "trip")
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "distance_km", nullable = false)
    private int distanceKm = 0;

    @Column(name = "is_dead_leg", nullable = false)
    private boolean isDeadLeg = false;

    @Column(name = "has_night_charge", nullable = false)
    private boolean hasNightCharge = false;

    @Column(name = "waiting_minutes", nullable = false)
    private int waitingMinutes = 0;

    @Column(name = "toll_amount_paisa", nullable = false)
    private long tollAmountPaisa = 0L;

    public Trip() {
    }

    public Trip(Long id, Vehicle vehicle, LocalDateTime startTime, LocalDateTime endTime,
                int distanceKm, boolean isDeadLeg, boolean hasNightCharge,
                int waitingMinutes, long tollAmountPaisa) {
        this.id = id;
        this.vehicle = vehicle;
        this.startTime = startTime;
        this.endTime = endTime;
        this.distanceKm = distanceKm;
        this.isDeadLeg = isDeadLeg;
        this.hasNightCharge = hasNightCharge;
        this.waitingMinutes = waitingMinutes;
        this.tollAmountPaisa = tollAmountPaisa;
    }

    public Trip(Vehicle vehicle, LocalDateTime startTime, LocalDateTime endTime,
                int distanceKm, boolean isDeadLeg, boolean hasNightCharge,
                int waitingMinutes, long tollAmountPaisa) {
        this.vehicle = vehicle;
        this.startTime = startTime;
        this.endTime = endTime;
        this.distanceKm = distanceKm;
        this.isDeadLeg = isDeadLeg;
        this.hasNightCharge = hasNightCharge;
        this.waitingMinutes = waitingMinutes;
        this.tollAmountPaisa = tollAmountPaisa;
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

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public int getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(int distanceKm) {
        this.distanceKm = distanceKm;
    }

    public boolean isDeadLeg() {
        return isDeadLeg;
    }

    public void setDeadLeg(boolean deadLeg) {
        isDeadLeg = deadLeg;
    }

    public boolean isHasNightCharge() {
        return hasNightCharge;
    }

    public void setHasNightCharge(boolean hasNightCharge) {
        this.hasNightCharge = hasNightCharge;
    }

    public int getWaitingMinutes() {
        return waitingMinutes;
    }

    public void setWaitingMinutes(int waitingMinutes) {
        this.waitingMinutes = waitingMinutes;
    }

    public long getTollAmountPaisa() {
        return tollAmountPaisa;
    }

    public void setTollAmountPaisa(long tollAmountPaisa) {
        this.tollAmountPaisa = tollAmountPaisa;
    }
}