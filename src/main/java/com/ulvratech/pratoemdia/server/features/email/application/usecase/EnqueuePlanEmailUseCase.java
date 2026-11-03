package com.ulvratech.pratoemdia.server.features.email.application.usecase;

import com.ulvratech.pratoemdia.server.features.email.application.dto.EnqueuePlanEmailInput;
import com.ulvratech.pratoemdia.server.features.email.application.port.in.EnqueuePlanEmail;
import com.ulvratech.pratoemdia.server.features.email.domain.entity.OutboxEmail;
import com.ulvratech.pratoemdia.server.features.email.domain.port.out.OutboxEmailRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@Transactional
public class EnqueuePlanEmailUseCase implements EnqueuePlanEmail {
    private final OutboxEmailRepository repository;
    private final Clock clock;

    public EnqueuePlanEmailUseCase(OutboxEmailRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public void execute(EnqueuePlanEmailInput input) {
        repository.save(
                OutboxEmail.plan(input.type(), input.recipient(), input.subject(), input.body(), clock.instant())
        );
    }
}
