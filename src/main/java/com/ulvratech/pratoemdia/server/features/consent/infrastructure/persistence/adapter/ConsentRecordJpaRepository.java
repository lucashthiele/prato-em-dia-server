package com.ulvratech.pratoemdia.server.features.consent.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.consent.domain.enums.ConsentDocumentType;
import com.ulvratech.pratoemdia.server.features.consent.infrastructure.persistence.model.ConsentRecordModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ConsentRecordJpaRepository extends JpaRepository<ConsentRecordModel, UUID> {
    Optional<ConsentRecordModel> findByUserAccountIdAndDocumentTypeAndVersion(
            UUID userAccountId, ConsentDocumentType documentType, String version
    );
}
