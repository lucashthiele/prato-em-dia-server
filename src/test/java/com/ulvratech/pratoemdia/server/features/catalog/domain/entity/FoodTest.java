package com.ulvratech.pratoemdia.server.features.catalog.domain.entity;

import com.ulvratech.pratoemdia.server.features.catalog.domain.enums.FoodOrigin;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FoodTest {
    @Test
    void createsFoodWithAuthorAndAdditionalDetails() {
        UUID author = UUID.randomUUID();

        var food = Food.create(
                " Arroz integral ", " Cereais ", " g ", " Sem sal ",
                " Cozinhar em água ", author, FoodOrigin.NUTRITIONIST
        );

        assertEquals("Arroz integral", food.getName());
        assertEquals("Cereais", food.getCategory());
        assertEquals("g", food.getDefaultUnit());
        assertEquals("Sem sal", food.getNotes());
        assertEquals("Cozinhar em água", food.getPreparationMethod());
        assertEquals(author, food.getCreatedByUserId());
        assertEquals(FoodOrigin.NUTRITIONIST, food.getOrigin());
        assertTrue(food.isActive());
    }

    @Test
    void derivesNormalizedNameOnCreateAndUpdate() {
        var food = Food.create(
                " Maçã Verde ", null, "g", null, null, UUID.randomUUID(), FoodOrigin.PATIENT
        );
        assertEquals("maca verde", food.getNormalizedName());

        food.update("Pão  Francês!", null, "g", null, null, true);
        assertEquals("pao frances", food.getNormalizedName());
    }

    @Test
    void rejectsBlankRequiredFields() {
        UUID author = UUID.randomUUID();

        assertThrows(
                IllegalArgumentException.class,
                () -> Food.create(" ", null, "g", null, null, author, FoodOrigin.PATIENT)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> Food.create("Arroz", null, " ", null, null, author, FoodOrigin.PATIENT)
        );
    }

    @Test
    void fullyReplacesFoodFieldsOnUpdate() {
        var food = Food.create(
                "Arroz", "Cereais", "g", "Sem sal", "Cozinhar", UUID.randomUUID(),
                FoodOrigin.ADMIN
        );

        food.update("Arroz integral", null, "kg", null, null, false);

        assertEquals("Arroz integral", food.getName());
        assertNull(food.getCategory());
        assertEquals("kg", food.getDefaultUnit());
        assertNull(food.getNotes());
        assertNull(food.getPreparationMethod());
        assertFalse(food.isActive());
    }
}
