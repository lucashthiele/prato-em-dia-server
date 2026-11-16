package com.ulvratech.pratoemdia.server.features.prescriptions.domain.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.MealType;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.PrescriptionStatus;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.PrescriptionVersionStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PrescriptionTest {
    @Test
    void publishesAtomicallyAndKeepsPublishedVersionsImmutable() {
        var prescription = prescription();
        var first = prescription.getVersions().getFirst();
        var firstPublication = Instant.parse("2026-09-11T12:00:00Z");

        prescription.publish(first.getId(), firstPublication);

        assertEquals(PrescriptionVersionStatus.PUBLISHED, first.getStatus());
        assertThrows(
                IllegalStateException.class, () -> first.replaceContent("Changed", null, first.getMeals())
        );

        var second = prescription.addDraft(UUID.randomUUID());
        assertNotEquals(first.getMeals().getFirst().id(), second.getMeals().getFirst().id());
        prescription.publish(second.getId(), firstPublication.plusSeconds(60));

        assertEquals(PrescriptionVersionStatus.ARCHIVED, first.getStatus());
        assertEquals(PrescriptionVersionStatus.PUBLISHED, second.getStatus());
    }

    @Test
    void archivesOnlyPublishedVersions() {
        var prescription = prescription();
        var draft = prescription.getVersions().getFirst();

        assertThrows(IllegalStateException.class, () -> draft.archive(Instant.now()));
        assertEquals(PrescriptionVersionStatus.DRAFT, draft.getStatus());

        prescription.publish(draft.getId(), Instant.parse("2026-09-11T12:00:00Z"));
        draft.archive(Instant.parse("2026-09-12T12:00:00Z"));
        assertEquals(PrescriptionVersionStatus.ARCHIVED, draft.getStatus());
        assertThrows(IllegalStateException.class, () -> draft.archive(Instant.now()));
    }

    @Test
    void newDraftCopiesThePublishedVersionAndNeverAnArchivedDraftNumber() {
        var prescription = prescription();
        var v1 = prescription.getVersions().getFirst();
        prescription.publish(v1.getId(), Instant.parse("2026-09-11T12:00:00Z"));
        var v2 = prescription.addDraft(UUID.randomUUID());
        v2.replaceContent("Draft v2", null, v2.getMeals());

        // a vigente continua sendo v1; v2 (rascunho) não é a base do próximo
        assertEquals(1, v1.getNumber());
        assertEquals(PrescriptionVersionStatus.DRAFT, v2.getStatus());
        prescription.discardDraft(v2.getId());
        var again = prescription.addDraft(UUID.randomUUID());

        assertEquals(2, again.getNumber());
        assertEquals("Plan", again.getTitle());
    }

    @Test
    void newDraftCopiesTheLatestPublishedWhenNoneIsCurrent() {
        var prescription = prescription();
        var v1 = prescription.getVersions().getFirst();
        prescription.publish(v1.getId(), Instant.parse("2026-09-11T12:00:00Z"));
        var v2 = prescription.addDraft(UUID.randomUUID());
        v2.replaceContent("Second", null, v2.getMeals());
        prescription.publish(v2.getId(), Instant.parse("2026-09-12T12:00:00Z"));
        var v3 = prescription.addDraft(UUID.randomUUID());
        v3.replaceContent("Discarded by archiving", null, v3.getMeals());
        // v3 é arquivado por outro caminho (ex.: dados legados): nunca pode servir de base
        var legacy = new PrescriptionVersion(
                UUID.randomUUID(), 4, PrescriptionVersionStatus.ARCHIVED, "Legacy archived draft", null,
                UUID.randomUUID(), Instant.parse("2026-09-13T12:00:00Z"), null,
                Instant.parse("2026-09-13T13:00:00Z"), v3.getMeals(), List.of()
        );
        var rebuilt = new Prescription(
                prescription.getId(), prescription.getPatientId(), prescription.getNutritionistId(),
                prescription.getStatus(), Instant.now(), Instant.now(), 0,
                List.of(v1, v2, legacy)
        );
        v2.archive(Instant.parse("2026-09-14T12:00:00Z"));

        var draft = rebuilt.addDraft(UUID.randomUUID());

        assertEquals(5, draft.getNumber());
        assertEquals("Second", draft.getTitle());
    }

    @Test
    void refusesToCopyWhenNoVersionWasEverPublished() {
        var prescription = new Prescription(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.PrescriptionStatus.ACTIVE,
                Instant.now(), Instant.now(), 0, List.of()
        );

        assertThrows(IllegalStateException.class, () -> prescription.addDraft(UUID.randomUUID()));
    }

    @Test
    void discardsOnlyDraftsAndReportsWhenNothingRemains() {
        var prescription = prescription();
        var first = prescription.getVersions().getFirst();
        prescription.publish(first.getId(), Instant.parse("2026-09-11T12:00:00Z"));
        var second = prescription.addDraft(UUID.randomUUID());

        assertThrows(IllegalStateException.class, () -> prescription.discardDraft(first.getId()));
        assertEquals(false, prescription.discardDraft(second.getId()));
        assertEquals(1, prescription.getVersions().size());

        var fresh = prescription();
        assertEquals(true, fresh.discardDraft(fresh.getVersions().getFirst().getId()));
        assertThrows(IllegalArgumentException.class, () -> fresh.discardDraft(UUID.randomUUID()));
    }

    @Test
    void draftUpdatedAtMatchesDatabasePrecision() {
        var draft = prescription().getVersions().getFirst();

        draft.replaceContent("Changed", null, draft.getMeals());

        assertEquals(draft.getUpdatedAt().truncatedTo(java.time.temporal.ChronoUnit.MICROS), draft.getUpdatedAt());
    }

    @Test
    void refusesToPublishAnEmptyDraft() {
        var prescription = Prescription.create(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Plan", null, List.of()
        );
        assertThrows(
                IllegalStateException.class,
                () -> prescription.publish(prescription.getVersions().getFirst().getId(), Instant.now())
        );
    }

    @Test
    void refusesDuplicateMealCategories() {
        var first = PrescribedMeal.create("First", MealType.LUNCH, 0, null, null, List.of());
        var second = PrescribedMeal.create("Second", MealType.LUNCH, 1, null, null, List.of());
        assertThrows(
                IllegalArgumentException.class,
                () -> Prescription.create(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "Plan",
                        null,
                        List.of(first, second)
                )
        );
    }

    @Test
    void archivingArchivesThePublishedVersionAndAnOpenDraft() {
        var prescription = prescription();
        var first = prescription.getVersions().getFirst();
        prescription.publish(first.getId(), Instant.parse("2026-09-11T12:00:00Z"));
        var draft = prescription.addDraft(UUID.randomUUID());
        var now = Instant.parse("2026-09-12T12:00:00Z");

        prescription.archive(now);

        assertEquals(PrescriptionStatus.ARCHIVED, prescription.getStatus());
        assertEquals(PrescriptionVersionStatus.ARCHIVED, first.getStatus());
        assertEquals(now, first.getArchivedAt());
        assertEquals(PrescriptionVersionStatus.ARCHIVED, draft.getStatus());
        assertEquals(now, draft.getArchivedAt());
        assertThrows(IllegalStateException.class, () -> prescription.addDraft(UUID.randomUUID()));
    }

    @Test
    void archivingKeepsTheArchivedAtOfVersionsAlreadyArchivedAndIsIdempotent() {
        var prescription = prescription();
        var first = prescription.getVersions().getFirst();
        var firstArchive = Instant.parse("2026-09-11T12:01:00Z");
        prescription.publish(first.getId(), Instant.parse("2026-09-11T12:00:00Z"));
        var second = prescription.addDraft(UUID.randomUUID());
        prescription.publish(second.getId(), firstArchive);

        prescription.archive(Instant.parse("2026-09-12T12:00:00Z"));
        prescription.archive(Instant.parse("2026-09-13T12:00:00Z"));

        assertEquals(firstArchive, first.getArchivedAt());
        assertEquals(Instant.parse("2026-09-12T12:00:00Z"), second.getArchivedAt());
        assertEquals(PrescriptionStatus.ARCHIVED, prescription.getStatus());
    }

    private Prescription prescription() {
        var item = PrescribedMealItem.create(UUID.randomUUID(), null, BigDecimal.ONE, "g", 0, null, List.of());
        var meal = PrescribedMeal.create("Breakfast", 0, null, null, List.of(item));
        return Prescription.create(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Plan", null, List.of(meal)
        );
    }
}
