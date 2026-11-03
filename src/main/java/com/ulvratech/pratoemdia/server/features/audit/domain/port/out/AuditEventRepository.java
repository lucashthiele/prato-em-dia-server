package com.ulvratech.pratoemdia.server.features.audit.domain.port.out;

import com.ulvratech.pratoemdia.server.features.audit.domain.entity.AuditEvent;
import com.ulvratech.pratoemdia.server.features.audit.domain.entity.AuditEventFilter;
import com.ulvratech.pratoemdia.server.shared.pagination.domain.PageRequest;
import com.ulvratech.pratoemdia.server.shared.pagination.domain.PageResult;

public interface AuditEventRepository {
    AuditEvent save(AuditEvent event);

    /** Newest first. */
    PageResult<AuditEvent> search(AuditEventFilter filter, PageRequest page);
}
