package com.rohobie.billing.service;

import com.rohobie.billing.domain.Vehicle;
import com.rohobie.billing.domain.Vendor;
import com.rohobie.billing.dto.request.VehicleRequest;
import com.rohobie.billing.dto.response.VehicleResponse;
import com.rohobie.billing.exception.ResourceNotFoundException;
import com.rohobie.billing.repository.VehicleRepository;
import com.rohobie.billing.repository.VendorRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * VehicleService
 *
 * Handles vehicle registration and retrieval associated with a fleet vendor.
 * Design decision: Validates vendor existence before associating with a vehicle.
 */
@Slf4j
@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final VendorRepository vendorRepository;

    public VehicleService(VehicleRepository vehicleRepository, VendorRepository vendorRepository) {
        this.vehicleRepository = vehicleRepository;
        this.vendorRepository = vendorRepository;
    }

    /** Registers a new vehicle under an existing vendor. */
    @CacheEvict(value = "vehicles", allEntries = true)
    @Transactional
    public VehicleResponse createVehicle(VehicleRequest request) {
        log.info("Registering vehicle: {}", request.registrationNumber());
        Vendor vendor = vendorRepository.findById(request.vendorId())
                .orElseThrow(() -> new ResourceNotFoundException("Vendor with id " + request.vendorId() + " not found"));

        Vehicle vehicle = new Vehicle(request.registrationNumber(), request.type(), vendor);
        Vehicle saved = vehicleRepository.save(vehicle);
        return mapToResponse(saved);
    }

    /** Retrieves vehicle details by ID. */
    @Cacheable(value = "vehicles", key = "#id")
    @Transactional(readOnly = true)
    public VehicleResponse getVehicleById(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle with id " + id + " not found"));
        return mapToResponse(vehicle);
    }

    /** Retrieves all registered vehicles. */
    @Transactional(readOnly = true)
    public List<VehicleResponse> getAllVehicles() {
        return vehicleRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    private VehicleResponse mapToResponse(Vehicle vehicle) {
        return new VehicleResponse(
                vehicle.getId(),
                vehicle.getRegistrationNumber(),
                vehicle.getType(),
                vehicle.getVendor().getId(),
                vehicle.getVendor().getName()
        );
    }
}