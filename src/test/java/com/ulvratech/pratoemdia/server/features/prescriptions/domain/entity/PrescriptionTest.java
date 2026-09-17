package com.ulvratech.pratoemdia.server.features.prescriptions.domain.entity;

import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.PrescriptionVersionStatus;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PrescriptionTest {
    @Test
    void publishesAtomicallyAndKeepsPublishedVersionsImmutable() {
        var prescription = prescription();
        var first = prescription.getVersions().getFirst();
        var firstPublication = Instant.parse("2026-09-11T12:00:00Z");

        prescription.publish(first.getId(), firstPublication);

        assertEquals(PrescriptionVersionStatus.PUBLISHED, first.getStatus());
        assertThrows(
                IllegalStateException.class,
                () -> first.replaceContent("Changed", null, first.getMeals())
        );

        var second = prescription.addDraft(UUID.randomUUID());
        assertNotEquals(first.getMeals().getFirst().id(), second.getMeals().getFirst().id());
        prescription.publish(second.getId(), firstPublication.plusSeconds(60));

        assertEquals(PrescriptionVersionStatus.ARCHIVED, first.getStatus());
        assertEquals(PrescriptionVersionStatus.PUBLISHED, second.getStatus());
    }

    @Test
    void refusesToPublishAnEmptyDraft() {
        var prescription = Prescription.create(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Plan", null, List.of()
        );
        assertThrows(
                IllegalStateException.class,
                () -> prescription.publish(prescription.getVersions().getFirst().getId(), Instant.now())
        );
    }

    private Prescription prescription() {
        var item = PrescribedMealItem.create(
                UUID.randomUUID(), null, BigDecimal.ONE, "g", 0,
                null, List.of()
        );
        var meal = PrescribedMeal.create("Breakfast", 0, null, null, List.of(item));
        return Prescription.create(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Plan", null, List.of(meal)
        );
    }
}
