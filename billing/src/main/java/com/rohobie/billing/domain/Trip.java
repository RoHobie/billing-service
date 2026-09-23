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

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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

    @Builder.Default
    @Column(name = "distance_km", nullable = false)
    private int distanceKm = 0;

    @Builder.Default
    @Column(name = "is_dead_leg", nullable = false)
    private boolean isDeadLeg = false;

    @Builder.Default
    @Column(name = "has_night_charge", nullable = false)
    private boolean hasNightCharge = false;

    @Builder.Default
    @Column(name = "waiting_minutes", nullable = false)
    private int waitingMinutes = 0;

    @Builder.Default
    @Column(name = "toll_amount_paisa", nullable = false)
    private long tollAmountPaisa = 0L;

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
}