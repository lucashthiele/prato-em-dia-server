package com.ulvratech.pratoemdia.server.features.audit.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.audit.infrastructure.persistence.model.AuditEventModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface AuditEventJpaRepository
        extends JpaRepository<AuditEventModel, UUID>, JpaSpecificationExecutor<AuditEventModel> {
}
