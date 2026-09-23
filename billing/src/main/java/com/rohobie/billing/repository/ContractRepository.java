package com.rohobie.billing.repository;

import com.rohobie.billing.domain.Contract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ContractRepository extends JpaRepository<Contract, Long> {

    @Query("SELECT c FROM Contract c WHERE c.vehicle.id = :vehicleId AND c.effectiveFrom <= :tripDate ORDER BY c.effectiveFrom DESC")
    List<Contract> findActiveContractsOnDate(@Param("vehicleId") Long vehicleId, @Param("tripDate") LocalDate tripDate);
}