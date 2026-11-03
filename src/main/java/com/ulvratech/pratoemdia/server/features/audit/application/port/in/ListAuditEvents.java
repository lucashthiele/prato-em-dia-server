package com.ulvratech.pratoemdia.server.features.audit.application.port.in;

import com.ulvratech.pratoemdia.server.features.audit.application.dto.ListAuditEventsInput;
import com.ulvratech.pratoemdia.server.features.audit.domain.entity.AuditEvent;
import com.ulvratech.pratoemdia.server.shared.application.usecase.UseCase;
import com.ulvratech.pratoemdia.server.shared.pagination.domain.PageResult;

public interface ListAuditEvents extends UseCase<ListAuditEventsInput, PageResult<AuditEvent>> {
}
