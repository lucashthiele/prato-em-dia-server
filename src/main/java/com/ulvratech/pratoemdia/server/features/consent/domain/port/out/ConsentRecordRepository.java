package com.ulvratech.pratoemdia.server.features.consent.domain.port.out;

import com.ulvratech.pratoemdia.server.features.consent.domain.entity.ConsentRecord;
import com.ulvratech.pratoemdia.server.features.consent.domain.enums.ConsentDocumentType;

import java.util.Optional;
import java.util.UUID;

public interface ConsentRecordRepository {
    Optional<ConsentRecord> find(UUID userAccountId, ConsentDocumentType documentType, String version);

    ConsentRecord save(ConsentRecord record);
}
