package com.ulvratech.pratoemdia.server.features.identity.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.identity.infrastructure.persistence.model.UserAccountModel;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserAccountJpaRepository extends JpaRepository<UserAccountModel, UUID> {
  Optional<UserAccountModel> findByEmail(String email);
}
