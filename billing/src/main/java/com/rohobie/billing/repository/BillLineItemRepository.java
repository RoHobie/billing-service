package com.rohobie.billing.repository;

import com.rohobie.billing.domain.BillLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BillLineItemRepository extends JpaRepository<BillLineItem, Long> {

    List<BillLineItem> findByBillingRunId(Long billingRunId);

    @Modifying
    @Query("DELETE FROM BillLineItem b WHERE b.billingRun.id = :billingRunId")
    void deleteByBillingRunId(@Param("billingRunId") Long billingRunId);
}
