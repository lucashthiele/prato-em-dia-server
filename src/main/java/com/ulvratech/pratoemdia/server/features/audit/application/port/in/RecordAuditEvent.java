package com.ulvratech.pratoemdia.server.features.audit.application.port.in;

import com.ulvratech.pratoemdia.server.features.audit.application.dto.RecordAuditEventInput;

/** Stores an audit event in the caller's transaction (joins it when one is active). */
public interface RecordAuditEvent {
    void execute(RecordAuditEventInput input);
}
