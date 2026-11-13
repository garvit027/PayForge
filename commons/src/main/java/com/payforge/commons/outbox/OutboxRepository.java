package com.payforge.commons.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OutboxRepository extends JpaRepository<OutboxEntry, Long> {

    /**
     * Claims a batch of pending rows for this relay instance.
     *
     * <p>{@code SKIP LOCKED} lets several relay pods run concurrently without handing the
     * same event to two of them. Rows stay locked until the surrounding transaction commits.</p>
     */
    @Query(value = """
            select * from outbox_entry
             where status = 'PENDING'
             order by id
             limit :batchSize
             for update skip locked
            """, nativeQuery = true)
    List<OutboxEntry> claimPending(@Param("batchSize") int batchSize);
}
