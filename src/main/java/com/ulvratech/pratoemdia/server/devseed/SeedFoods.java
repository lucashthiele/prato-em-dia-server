package com.ulvratech.pratoemdia.server.devseed;

import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Picks foods that already exist in the catalog. The catalog is never written to: the seed only needs
 * foods with a standard portion that carries energy, so prescriptions and executions have nutrients.
 */
class SeedFoods {
    record Food(UUID foodId, UUID portionId, BigDecimal portionQuantity, String unit, String name) {
    }

    /** Preferred name prefixes per slot, in order. Slots are used by index in {@link PrescriptionSeeder}. */
    private static final List<List<String>> SLOTS = List.of(
            List.of("Pão, trigo, francês", "Pão, de forma", "Pão"),
            List.of("Leite, de vaca, integral", "Leite"),
            List.of("Banana, prata", "Banana", "Mamão"),
            List.of("Arroz, integral, cozido", "Arroz, tipo 1, cozido", "Arroz"),
            List.of("Feijão, carioca, cozido", "Feijão, preto, cozido", "Feijão"),
            List.of("Frango, peito, sem pele, grelhado", "Frango, peito", "Frango"),
            List.of("Alface, crespa", "Alface"),
            List.of("Iogurte, natural", "Iogurte"),
            List.of("Maçã", "Laranja", "Pera"),
            List.of("Batata, inglesa, cozida", "Batata"),
            List.of("Carne, bovina, patinho", "Ovo, de galinha, inteiro, cozido", "Carne"),
            List.of("Cenoura, cozida", "Cenoura"),
            List.of("Tapioca", "Biscoito", "Pão, de forma"),
            List.of("Macarrão", "Batata, doce"),
            List.of("Peixe", "Ovo")
    );

    private final List<Food> foods;

    private SeedFoods(List<Food> foods) {
        this.foods = foods;
    }

    static SeedFoods load(JdbcTemplate jdbc) {
        Set<UUID> used = new LinkedHashSet<>();
        List<Food> picked = new ArrayList<>();
        for (var prefixes : SLOTS) {
            Food food = null;
            for (var prefix : prefixes) {
                food = candidates(jdbc, prefix + "%").stream()
                        .filter(candidate -> !used.contains(candidate.foodId())).findFirst().orElse(null);
                if (food != null) {
                    break;
                }
            }
            if (food == null) {
                food = candidates(jdbc, "%").stream()
                        .filter(candidate -> !used.contains(candidate.foodId())).findFirst().orElse(null);
            }
            if (food == null) {
                throw new IllegalStateException(
                        "The food catalog has too few foods with energy values. Import the catalog first."
                );
            }
            used.add(food.foodId());
            picked.add(food);
        }
        return new SeedFoods(List.copyOf(picked));
    }

    private static List<Food> candidates(JdbcTemplate jdbc, String pattern) {
        return jdbc.query(
                """
                        SELECT f.id, p.id AS portion_id, p.quantity, p.unit, f.name
                        FROM food f
                        JOIN food_portion p ON p.food_id = f.id AND p.source_type = 'STANDARD' AND p.active
                        JOIN food_portion_nutrient n ON n.food_portion_id = p.id
                             AND n.nutrient_code = 'energy_kcal' AND n.value_status = 'VALUE'
                        WHERE f.active AND p.unit IS NOT NULL AND f.name ILIKE ?
                        ORDER BY length(f.name), f.name
                        LIMIT 30
                        """, (rs, row) -> new Food(
                        rs.getObject("id", UUID.class), rs.getObject("portion_id", UUID.class),
                        rs.getBigDecimal("quantity"), rs.getString("unit"), rs.getString("name")
                ), pattern
        );
    }

    Food get(int slot) {
        return foods.get(slot);
    }
}
