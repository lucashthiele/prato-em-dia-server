package com.ulvratech.pratoemdia.server.features.audit.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.audit.infrastructure.persistence.model.AuditAccountModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuditAccountJpaRepository extends JpaRepository<AuditAccountModel, UUID> {
}
