package com.rohobie.billing.repository;

import com.rohobie.billing.domain.BillLineItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BillLineItemRepository extends JpaRepository<BillLineItem, Long> {

    List<BillLineItem> findByBillingRunId(Long billingRunId);

    Page<BillLineItem> findByBillingRunId(Long billingRunId, Pageable pageable);

    @Modifying
    @Query("DELETE FROM BillLineItem b WHERE b.billingRun.id = :billingRunId")
    void deleteByBillingRunId(@Param("billingRunId") Long billingRunId);

    @Query("SELECT COALESCE(SUM(b.totalPaisa), 0) FROM BillLineItem b")
    Long sumTotalPaisa();
}
