package com.rohobie.billing.repository;

import com.rohobie.billing.domain.ContractSlab;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContractSlabRepository extends JpaRepository<ContractSlab, Long> {
    List<ContractSlab> findByContractIdOrderByFromKmAsc(Long contractId);
}