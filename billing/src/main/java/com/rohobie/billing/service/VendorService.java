package com.rohobie.billing.service;

import com.rohobie.billing.domain.Vendor;
import com.rohobie.billing.dto.request.VendorRequest;
import com.rohobie.billing.dto.response.VendorResponse;
import com.rohobie.billing.exception.ResourceNotFoundException;
import com.rohobie.billing.repository.VendorRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * VendorService
 *
 * Manages lifecycle and queries for rental fleet vendors.
 * Design decision: Basic CRUD operations encapsulated in service layer
 * to ensure controllers remain strictly HTTP-facing.
 */
@Slf4j
@Service
public class VendorService {

    private final VendorRepository vendorRepository;

    public VendorService(VendorRepository vendorRepository) {
        this.vendorRepository = vendorRepository;
    }

    /** Creates a new vendor from the given request. */
    @Transactional
    public VendorResponse createVendor(VendorRequest request) {
        log.info("Creating vendor with name: {}", request.name());
        Vendor vendor = new Vendor(request.name(), request.contactEmail());
        Vendor saved = vendorRepository.save(vendor);
        return mapToResponse(saved);
    }

    /** Retrieves a vendor by its ID or throws ResourceNotFoundException. */
    @Transactional(readOnly = true)
    public VendorResponse getVendorById(Long id) {
        Vendor vendor = vendorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor with id " + id + " not found"));
        return mapToResponse(vendor);
    }

    /** Retrieves all registered vendors. */
    @Transactional(readOnly = true)
    public List<VendorResponse> getAllVendors() {
        return vendorRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    private VendorResponse mapToResponse(Vendor vendor) {
        return new VendorResponse(vendor.getId(), vendor.getName(), vendor.getContactEmail());
    }
}