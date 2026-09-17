package com.ulvratech.pratoemdia.server.features.catalog.domain.entity;

import static org.junit.jupiter.api.Assertions.*;

import com.ulvratech.pratoemdia.server.features.catalog.domain.enums.FoodOrigin;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FoodTest {
  @Test
  void createsFoodWithAuthorAndAdditionalDetails() {
    UUID author = UUID.randomUUID();

    var food =
        Food.create(
            " Arroz integral ",
            " Cereais ",
            " g ",
            " Sem sal ",
            " Cozinhar em água ",
            author,
            FoodOrigin.NUTRITIONIST);

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
  void rejectsBlankRequiredFields() {
    UUID author = UUID.randomUUID();

    assertThrows(
        IllegalArgumentException.class,
        () -> Food.create(" ", null, "g", null, null, author, FoodOrigin.PATIENT));
    assertThrows(
        IllegalArgumentException.class,
        () -> Food.create("Arroz", null, " ", null, null, author, FoodOrigin.PATIENT));
  }

  @Test
  void fullyReplacesFoodFieldsOnUpdate() {
    var food =
        Food.create(
            "Arroz", "Cereais", "g", "Sem sal", "Cozinhar", UUID.randomUUID(), FoodOrigin.ADMIN);

    food.update("Arroz integral", null, "kg", null, null, false);

    assertEquals("Arroz integral", food.getName());
    assertNull(food.getCategory());
    assertEquals("kg", food.getDefaultUnit());
    assertNull(food.getNotes());
    assertNull(food.getPreparationMethod());
    assertFalse(food.isActive());
  }
}
