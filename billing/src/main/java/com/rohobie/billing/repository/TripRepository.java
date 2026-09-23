package com.rohobie.billing.repository;

import com.rohobie.billing.domain.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {
    List<Trip> findByVehicleIdAndStartTimeBetweenOrderByStartTimeAsc(
            Long vehicleId, LocalDateTime startInclusive, LocalDateTime endInclusive);
}