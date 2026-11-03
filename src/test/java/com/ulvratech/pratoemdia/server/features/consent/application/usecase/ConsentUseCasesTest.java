package com.ulvratech.pratoemdia.server.features.consent.application.usecase;

import com.ulvratech.pratoemdia.server.features.consent.application.dto.AcceptConsentInput;
import com.ulvratech.pratoemdia.server.features.consent.application.exception.ConsentVersionMismatchException;
import com.ulvratech.pratoemdia.server.features.consent.config.ConsentProperties;
import com.ulvratech.pratoemdia.server.features.consent.domain.entity.ConsentRecord;
import com.ulvratech.pratoemdia.server.features.consent.domain.enums.ConsentDocumentType;
import com.ulvratech.pratoemdia.server.features.consent.domain.port.out.ConsentRecordRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConsentUseCasesTest {
    private static final Instant NOW = Instant.parse("2026-11-03T12:00:00Z");
    private final ConsentRecordRepository repository = mock(ConsentRecordRepository.class);
    private final ConsentProperties properties = new ConsentProperties(
            true, Map.of(ConsentDocumentType.TERMS_OF_USE, "2", ConsentDocumentType.PRIVACY_POLICY, "1")
    );
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final UUID user = UUID.randomUUID();

    @Test
    void statusListsEveryDocumentAndIsPendingUntilTheCurrentVersionIsAccepted() {
        when(repository.find(user, ConsentDocumentType.TERMS_OF_USE, "2")).thenReturn(Optional.empty());
        when(repository.find(user, ConsentDocumentType.PRIVACY_POLICY, "1")).thenReturn(
                Optional.of(new ConsentRecord(UUID.randomUUID(), user, ConsentDocumentType.PRIVACY_POLICY, "1", NOW))
        );

        var status = new GetConsentStatusUseCase(repository, properties).execute(user);

        assertFalse(status.upToDate());
        assertEquals(2, status.documents().size());
        var terms = status.documents().stream().filter(d -> d.documentType() == ConsentDocumentType.TERMS_OF_USE)
                .findFirst().orElseThrow();
        assertEquals("2", terms.currentVersion());
        assertFalse(terms.accepted());
        var privacy = status.documents().stream().filter(d -> d.documentType() == ConsentDocumentType.PRIVACY_POLICY)
                .findFirst().orElseThrow();
        assertTrue(privacy.accepted());
        assertEquals(NOW, privacy.acceptedAt());
    }

    @Test
    void anOlderAcceptedVersionDoesNotCount_aNewVersionAsksAgain() {
        when(repository.find(any(), any(), any())).thenReturn(Optional.empty());

        assertFalse(new GetConsentStatusUseCase(repository, properties).execute(user).upToDate());
        verify(repository).find(user, ConsentDocumentType.TERMS_OF_USE, "2");
    }

    @Test
    void upToDateWhenAllCurrentVersionsAreAccepted() {
        when(repository.find(any(), any(), any())).thenAnswer(
                i -> Optional.of(new ConsentRecord(UUID.randomUUID(), user, i.getArgument(1), i.getArgument(2), NOW))
        );

        assertTrue(new GetConsentStatusUseCase(repository, properties).execute(user).upToDate());
    }

    @Test
    void acceptingTheCurrentVersionStoresARecordWithTheClockTime() {
        when(repository.find(user, ConsentDocumentType.TERMS_OF_USE, "2")).thenReturn(Optional.empty());

        new AcceptConsentUseCase(repository, properties, clock)
                .execute(new AcceptConsentInput(user, ConsentDocumentType.TERMS_OF_USE, "2"));

        var captor = org.mockito.ArgumentCaptor.forClass(ConsentRecord.class);
        verify(repository).save(captor.capture());
        assertEquals(user, captor.getValue().userAccountId());
        assertEquals(ConsentDocumentType.TERMS_OF_USE, captor.getValue().documentType());
        assertEquals("2", captor.getValue().version());
        assertEquals(NOW, captor.getValue().acceptedAt());
    }

    @Test
    void acceptingAnOutdatedVersionIsRejected() {
        assertThrows(
                ConsentVersionMismatchException.class,
                () -> new AcceptConsentUseCase(repository, properties, clock)
                        .execute(new AcceptConsentInput(user, ConsentDocumentType.TERMS_OF_USE, "1"))
        );
        verify(repository, never()).save(any());
    }

    @Test
    void acceptingTwiceIsIdempotent() {
        when(repository.find(user, ConsentDocumentType.PRIVACY_POLICY, "1")).thenReturn(
                Optional.of(new ConsentRecord(UUID.randomUUID(), user, ConsentDocumentType.PRIVACY_POLICY, "1", NOW))
        );

        new AcceptConsentUseCase(repository, properties, clock)
                .execute(new AcceptConsentInput(user, ConsentDocumentType.PRIVACY_POLICY, "1"));

        verify(repository, never()).save(any());
    }
}
