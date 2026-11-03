package com.ulvratech.pratoemdia.server.features.email.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.email.infrastructure.persistence.model.OutboxEmailModel;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OutboxEmailJpaRepository extends JpaRepository<OutboxEmailModel, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from OutboxEmailModel e where e.id = :id")
    Optional<OutboxEmailModel> findByIdForUpdate(@Param("id") UUID id);

    @Query("select e.id from OutboxEmailModel e where e.status = com.ulvratech.pratoemdia.server.features.email.domain.enums.EmailStatus.PENDING"
            + " and e.nextAttemptAt <= :now order by e.nextAttemptAt")
    List<UUID> findDueIds(@Param("now") Instant now, Pageable limit);

    @Query("select e from OutboxEmailModel e where e.type = com.ulvratech.pratoemdia.server.features.email.domain.enums.EmailType.ACCESS"
            + " and e.accountId in :accountIds order by e.createdAt desc")
    List<OutboxEmailModel> findAccessByAccounts(@Param("accountIds") Collection<UUID> accountIds);
}
