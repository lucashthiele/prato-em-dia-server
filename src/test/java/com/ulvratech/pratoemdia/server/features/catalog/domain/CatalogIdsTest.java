package com.ulvratech.pratoemdia.server.features.catalog.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CatalogIdsTest {
    @Test
    void generatesStableUuidV5ForTacoFood() {
        UUID expected = UUID.fromString("ec2255d4-1b3c-534e-b069-046dcfd75c3c");

        assertEquals(expected, CatalogIds.tacoFood("1"));
        assertEquals(expected, CatalogIds.tacoFood(" 1 "));
        assertEquals(5, expected.version());
    }

    @Test
    void generatesStableUuidV5ForFoodNutrient() {
        UUID foodId = CatalogIds.tacoFood("1");

        assertEquals(
                UUID.fromString("31ee1e06-b44e-5428-89b7-e42f1f4829d9"),
                CatalogIds.foodPortionNutrient(foodId, "sodium")
        );
    }
}
