package com.rohobie.billing.repository;

import com.rohobie.billing.domain.BillingRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BillingRunRepository extends JpaRepository<BillingRun, Long> {

    Optional<BillingRun> findByVehicleIdAndBillingMonth(Long vehicleId, String billingMonth);

    java.util.List<BillingRun> findByVehicleIdOrderByRunAtDesc(Long vehicleId);
}
