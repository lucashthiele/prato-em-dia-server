package com.ulvratech.pratoemdia.server.features.audit.application.usecase;

import com.ulvratech.pratoemdia.server.features.audit.application.dto.ListAuditEventsInput;
import com.ulvratech.pratoemdia.server.features.audit.application.dto.RecordAuditEventInput;
import com.ulvratech.pratoemdia.server.features.audit.domain.entity.AuditEvent;
import com.ulvratech.pratoemdia.server.features.audit.domain.enums.AuditAction;
import com.ulvratech.pratoemdia.server.features.audit.domain.port.out.AuditEventRepository;
import com.ulvratech.pratoemdia.server.shared.pagination.domain.PageResult;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditUseCasesTest {
    private static final Instant NOW = Instant.parse("2026-11-03T12:00:00Z");
    private final AuditEventRepository repository = mock(AuditEventRepository.class);

    @Test
    void recordsTheEventWithTheClockTime() {
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        var actor = UUID.randomUUID();
        var target = UUID.randomUUID();

        new RecordAuditEventUseCase(repository, Clock.fixed(NOW, ZoneOffset.UTC))
                .execute(
                        new RecordAuditEventInput(actor, AuditAction.USER_STATUS_CHANGED, target, "ACTIVE -> INACTIVE")
                );

        var saved = ArgumentCaptor.forClass(AuditEvent.class);
        verify(repository).save(saved.capture());
        assertEquals(actor, saved.getValue().actorAccountId());
        assertEquals(AuditAction.USER_STATUS_CHANGED, saved.getValue().action());
        assertEquals(target, saved.getValue().targetAccountId());
        assertEquals("ACTIVE -> INACTIVE", saved.getValue().details());
        assertEquals(NOW, saved.getValue().occurredAt());
    }

    @Test
    void listsAPageOfEventsApplyingTheFilter() {
        var event = new AuditEvent(
                UUID.randomUUID(), NOW, UUID.randomUUID(), AuditAction.NUTRITIONIST_CREATED, UUID.randomUUID(), "x"
        );
        when(repository.search(any(), any())).thenReturn(new PageResult<>(List.of(event), 0, 20, 1, 1));

        var page = new ListAuditEventsUseCase(repository)
                .execute(new ListAuditEventsInput(AuditAction.NUTRITIONIST_CREATED, null, null, null, null, 0, 20));

        assertEquals(1, page.content().size());
        assertEquals(1, page.totalElements());
    }

    @Test
    void rejectsAnInvertedDateRange() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ListAuditEventsUseCase(repository).execute(
                        new ListAuditEventsInput(null, null, null, NOW, NOW.minusSeconds(1), 0, 20)
                )
        );
    }
}
