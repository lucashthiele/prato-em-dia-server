package com.ulvratech.pratoemdia.server.features.email.domain.port.out;

import com.ulvratech.pratoemdia.server.features.email.domain.entity.OutboxEmail;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OutboxEmailRepository {
    OutboxEmail save(OutboxEmail email);

    Optional<OutboxEmail> findById(UUID id);

    /** Locks the row ({@code FOR UPDATE}) so only one instance attempts it. */
    Optional<OutboxEmail> findByIdForUpdate(UUID id);

    List<UUID> findDueIds(Instant now, int limit);

    /** Most recent access email of each account, for the admin screens. */
    List<OutboxEmail> findLatestAccessByAccounts(List<UUID> accountIds);
}
