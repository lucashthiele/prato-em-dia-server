package com.ulvratech.pratoemdia.server.features.email.application.usecase;

import com.ulvratech.pratoemdia.server.features.email.application.dto.EnqueueAccessEmailInput;
import com.ulvratech.pratoemdia.server.features.email.application.port.in.EnqueueAccessEmail;
import com.ulvratech.pratoemdia.server.features.email.domain.entity.OutboxEmail;
import com.ulvratech.pratoemdia.server.features.email.domain.port.out.OutboxEmailRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
@Transactional
public class EnqueueAccessEmailUseCase implements EnqueueAccessEmail {
    private final OutboxEmailRepository repository;
    private final Clock clock;

    public EnqueueAccessEmailUseCase(OutboxEmailRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public UUID execute(EnqueueAccessEmailInput input) {
        return repository.save(
                OutboxEmail.access(
                        input.accountId(), input.recipient(), input.recipientName(), input.inviterName(),
                        clock.instant()
                )
        ).getId();
    }
}
