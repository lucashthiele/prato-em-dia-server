package com.ulvratech.pratoemdia.server.features.admin.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.admin.infrastructure.persistence.model.AdminUserModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface AdminUserJpaRepository
        extends JpaRepository<AdminUserModel, UUID>, JpaSpecificationExecutor<AdminUserModel> {
}
