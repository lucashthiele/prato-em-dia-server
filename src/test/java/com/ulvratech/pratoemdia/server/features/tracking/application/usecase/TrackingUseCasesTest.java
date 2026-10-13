package com.ulvratech.pratoemdia.server.features.tracking.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ulvratech.pratoemdia.server.features.tracking.application.dto.DiaryTypes;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.ExecutionTypes;
import com.ulvratech.pratoemdia.server.features.tracking.application.exception.TrackingException;
import com.ulvratech.pratoemdia.server.features.tracking.application.port.out.DiaryData;
import com.ulvratech.pratoemdia.server.features.tracking.application.port.out.ExecutionData;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TrackingUseCasesTest {
    @Test
    void executionCannotReadAnotherPatientsData() {
        var diary = mock(DiaryData.class);
        var data = mock(ExecutionData.class);
        var useCase = new ExecutionUseCase(diary, data);
        var account = UUID.randomUUID();
        var execution = UUID.randomUUID();
        when(diary.patientId(account)).thenReturn(Optional.empty());

        var error = assertThrows(
                TrackingException.class,
                () -> useCase.get(new ExecutionTypes.Actor(account, "PATIENT"), execution)
        );

        assertEquals(TrackingException.Kind.FORBIDDEN, error.kind());
        verify(data, never()).get(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void executionUsesResolvedPatientIdentityForMutations() {
        var diary = mock(DiaryData.class);
        var data = mock(ExecutionData.class);
        var useCase = new ExecutionUseCase(diary, data);
        var account = UUID.randomUUID();
        var patient = UUID.randomUUID();
        var date = LocalDate.parse("2026-10-13");
        var input = new ExecutionTypes.Write(date, null, java.util.List.of());
        when(diary.patientId(account)).thenReturn(Optional.of(patient));

        useCase.create(new ExecutionTypes.Actor(account, "PATIENT"), "request-1", input);

        verify(data).create(patient, "request-1", input);
    }

    @Test
    void diaryRequiresADateAndPatientRoleBeforeReadingSummary() {
        var data = mock(DiaryData.class);
        var useCase = new DiaryUseCase(data);
        var account = UUID.randomUUID();

        var missingDate = assertThrows(
                TrackingException.class,
                () -> useCase.dailySummary(new DiaryTypes.Actor(account, "PATIENT"), null)
        );
        var wrongRole = assertThrows(
                TrackingException.class,
                () -> useCase.dailySummary(new DiaryTypes.Actor(account, "NUTRITIONIST"), LocalDate.now())
        );

        assertEquals(TrackingException.Kind.INVALID, missingDate.kind());
        assertEquals(TrackingException.Kind.FORBIDDEN, wrongRole.kind());
        verify(data, never()).dailySummary(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }
}
