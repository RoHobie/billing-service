package com.rohobie.billing.repository;

import com.rohobie.billing.domain.FraudFlag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FraudFlagRepository extends JpaRepository<FraudFlag, Long> {

    List<FraudFlag> findByBillingRunId(Long billingRunId);

    @Modifying
    @Query("DELETE FROM FraudFlag f WHERE f.billingRun.id = :billingRunId")
    void deleteByBillingRunId(@Param("billingRunId") Long billingRunId);
}
