package com.ulvratech.pratoemdia.server.features.audit.domain.port.out;

import com.ulvratech.pratoemdia.server.features.audit.domain.entity.AuditAccountIdentity;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public interface AuditAccountDirectory {
    /** Resolves all the given accounts in one query; ids without an account are absent from the result. */
    Map<UUID, AuditAccountIdentity> findByIds(Collection<UUID> ids);
}
