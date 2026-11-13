package com.payforge.ledger.repo;

import com.payforge.ledger.domain.Posting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.UUID;

public interface PostingRepository extends JpaRepository<Posting, UUID> {

    @Query(value = "select coalesce(sum(amount), 0) from posting "
            + "where ledger_account_id = :accountId and direction = 'CREDIT'", nativeQuery = true)
    BigDecimal totalCredits(@Param("accountId") UUID accountId);

    @Query(value = "select coalesce(sum(amount), 0) from posting "
            + "where ledger_account_id = :accountId and direction = 'DEBIT'", nativeQuery = true)
    BigDecimal totalDebits(@Param("accountId") UUID accountId);
}
