package com.rohobie.billing.service;

import com.rohobie.billing.domain.Trip;
import com.rohobie.billing.domain.Vehicle;
import com.rohobie.billing.dto.request.TripRequest;
import com.rohobie.billing.dto.response.TripResponse;
import com.rohobie.billing.exception.ResourceNotFoundException;
import com.rohobie.billing.repository.TripRepository;
import com.rohobie.billing.repository.VehicleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * TripService
 *
 * Records and retrieves trip duty records for vehicles.
 * Design decision: Links each trip to a registered vehicle and preserves
 * dead-leg, waiting time, and night flags for downstream billing.
 */
@Slf4j
@Service
public class TripService {

    private final TripRepository tripRepository;
    private final VehicleRepository vehicleRepository;

    public TripService(TripRepository tripRepository, VehicleRepository vehicleRepository) {
        this.tripRepository = tripRepository;
        this.vehicleRepository = vehicleRepository;
    }

    /** Records a new trip for a vehicle. */
    @Transactional
    public TripResponse createTrip(TripRequest request) {
        log.info("Recording trip for vehicle ID: {} distance: {}km", request.vehicleId(), request.distanceKm());
        Vehicle vehicle = vehicleRepository.findById(request.vehicleId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle with id " + request.vehicleId() + " not found"));

        Trip trip = new Trip(
                vehicle,
                request.startTime(),
                request.endTime(),
                request.distanceKm(),
                request.isDeadLeg(),
                request.hasNightCharge(),
                request.waitingMinutes(),
                request.tollAmountPaisa()
        );
        Trip saved = tripRepository.save(trip);
        return mapToResponse(saved);
    }

    /** Retrieves trip details by ID. */
    @Transactional(readOnly = true)
    public TripResponse getTripById(Long id) {
        Trip trip = tripRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trip with id " + id + " not found"));
        return mapToResponse(trip);
    }

    /** Retrieves all trips for a specific vehicle ordered by start time. */
    @Transactional(readOnly = true)
    public java.util.List<TripResponse> getTripsByVehicle(Long vehicleId) {
        if (!vehicleRepository.existsById(vehicleId)) {
            throw new ResourceNotFoundException("Vehicle with id " + vehicleId + " not found");
        }
        return tripRepository.findByVehicleIdOrderByStartTimeAsc(vehicleId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    private TripResponse mapToResponse(Trip trip) {
        return new TripResponse(
                trip.getId(),
                trip.getVehicle().getId(),
                trip.getVehicle().getRegistrationNumber(),
                trip.getStartTime(),
                trip.getEndTime(),
                trip.getDistanceKm(),
                trip.isDeadLeg(),
                trip.isHasNightCharge(),
                trip.getWaitingMinutes(),
                trip.getTollAmountPaisa()
        );
    }
}