package com.ulvratech.pratoemdia.server.features.consent.application.usecase;

import com.ulvratech.pratoemdia.server.features.consent.application.dto.AcceptConsentInput;
import com.ulvratech.pratoemdia.server.features.consent.application.exception.ConsentVersionMismatchException;
import com.ulvratech.pratoemdia.server.features.consent.application.port.in.AcceptConsent;
import com.ulvratech.pratoemdia.server.features.consent.config.ConsentProperties;
import com.ulvratech.pratoemdia.server.features.consent.domain.entity.ConsentRecord;
import com.ulvratech.pratoemdia.server.features.consent.domain.port.out.ConsentRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
@Transactional
public class AcceptConsentUseCase implements AcceptConsent {
    private final ConsentRecordRepository records;
    private final ConsentProperties properties;
    private final Clock clock;

    public AcceptConsentUseCase(ConsentRecordRepository records, ConsentProperties properties, Clock clock) {
        this.records = records;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public void execute(AcceptConsentInput input) {
        if (!properties.currentVersions().get(input.documentType()).equals(input.version())) {
            throw new ConsentVersionMismatchException();
        }
        if (records.find(input.userAccountId(), input.documentType(), input.version()).isPresent()) {
            return;
        }
        records.save(
                new ConsentRecord(
                        UUID.randomUUID(), input.userAccountId(), input.documentType(), input.version(),
                        clock.instant()
                )
        );
    }
}
