package com.ulvratech.pratoemdia.server.features.consent.application.usecase;

import com.ulvratech.pratoemdia.server.features.consent.application.dto.ConsentStatusOutput;
import com.ulvratech.pratoemdia.server.features.consent.application.port.in.GetConsentStatus;
import com.ulvratech.pratoemdia.server.features.consent.config.ConsentProperties;
import com.ulvratech.pratoemdia.server.features.consent.domain.enums.ConsentDocumentType;
import com.ulvratech.pratoemdia.server.features.consent.domain.port.out.ConsentRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetConsentStatusUseCase implements GetConsentStatus {
    private final ConsentRecordRepository records;
    private final ConsentProperties properties;

    public GetConsentStatusUseCase(ConsentRecordRepository records, ConsentProperties properties) {
        this.records = records;
        this.properties = properties;
    }

    @Override
    public ConsentStatusOutput execute(UUID userAccountId) {
        var documents = Arrays.stream(ConsentDocumentType.values()).map(type -> {
            var version = properties.currentVersions().get(type);
            var record = records.find(userAccountId, type, version);
            return new ConsentStatusOutput.DocumentStatus(
                    type, version, record.isPresent(), record.map(r -> r.acceptedAt()).orElse(null)
            );
        }).toList();
        return new ConsentStatusOutput(documents.stream().allMatch(d -> d.accepted()), documents);
    }
}
