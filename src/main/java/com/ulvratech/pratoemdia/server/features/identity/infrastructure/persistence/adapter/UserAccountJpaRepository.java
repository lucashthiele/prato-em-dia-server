package com.ulvratech.pratoemdia.server.features.identity.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.identity.infrastructure.persistence.model.UserAccountModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserAccountJpaRepository extends JpaRepository<UserAccountModel, UUID> {
}
