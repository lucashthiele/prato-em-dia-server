package com.ulvratech.pratoemdia.server.features.summary.application.usecase;

import com.ulvratech.pratoemdia.server.features.summary.application.dto.GetSummaryInput;
import com.ulvratech.pratoemdia.server.features.summary.application.dto.NutritionistSummaryOutput.DraftToContinue;
import com.ulvratech.pratoemdia.server.features.summary.application.dto.PatientAccessCounts;
import com.ulvratech.pratoemdia.server.features.summary.application.port.out.SummaryReferences;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GetNutritionistSummaryUseCaseTest {
    @Test
    void composesCountsAndDraftsKeepingPendingSeparateFromFailed() {
        var references = mock(SummaryReferences.class);
        var account = UUID.randomUUID();
        var draft = new DraftToContinue(
                UUID.randomUUID(), "Patient", UUID.randomUUID(), UUID.randomUUID(), "Plan", 2, Instant.now()
        );
        when(references.patientAccessCounts(account)).thenReturn(new PatientAccessCounts(7, 2, 1));
        when(references.countOpenDrafts(account)).thenReturn(4L);
        when(references.openDrafts(account, 3)).thenReturn(List.of(draft));

        var output = new GetNutritionistSummaryUseCase(references).execute(new GetSummaryInput(account, 3));

        assertEquals(7, output.counts().activePatients());
        assertEquals(2, output.counts().pendingAccess());
        assertEquals(1, output.counts().failedAccess());
        assertEquals(4, output.counts().openDrafts());
        assertEquals(List.of(draft), output.drafts());
    }
}
