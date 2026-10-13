package com.ulvratech.pratoemdia.server.features.catalog.infrastructure.persistence;

import com.ulvratech.pratoemdia.server.features.catalog.application.usecase.SyncImportedFoodsUseCase;
import com.ulvratech.pratoemdia.server.features.catalog.domain.CatalogIds;
import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.NutrientValue;
import com.ulvratech.pratoemdia.server.features.catalog.domain.enums.NutrientValueStatus;
import com.ulvratech.pratoemdia.server.features.catalog.domain.port.out.FoodRepository;
import com.ulvratech.pratoemdia.server.features.catalog.domain.port.out.FoodPortionRepository;
import com.ulvratech.pratoemdia.server.features.importer.application.dto.ImportTacoCatalogInput;
import com.ulvratech.pratoemdia.server.features.importer.application.dto.TacoFoodRecord;
import com.ulvratech.pratoemdia.server.features.importer.application.dto.TacoSpreadsheetData;
import com.ulvratech.pratoemdia.server.features.importer.application.usecase.ImportTacoCatalogUseCase;
import com.ulvratech.pratoemdia.server.features.importer.application.usecase.ImportMongoCatalogUseCase;
import com.ulvratech.pratoemdia.server.features.importer.application.dto.ImportMongoCatalogInput;
import com.ulvratech.pratoemdia.server.features.importer.infrastructure.config.MongoImportProperties;
import com.ulvratech.pratoemdia.server.features.importer.infrastructure.mongodb.MongoCatalogReader;
import com.ulvratech.pratoemdia.server.features.catalog.application.port.in.SyncImportedFoods;
import com.ulvratech.pratoemdia.server.features.catalog.application.port.in.ListFoods;
import com.ulvratech.pratoemdia.server.features.catalog.application.dto.ListFoodsInput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "security.jwt.secret=01234567890123456789012345678901",
        "spring.docker.compose.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate"
})
class CatalogMigrationTest {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    FoodRepository foods;

    @Autowired
    FoodPortionRepository portions;

    @Autowired
    SyncImportedFoods syncImportedFoods;

    @Autowired
    ListFoods listFoods;

    @Test
    void searchesActiveFoodsWithoutCaseOrAccentAndKeepsPagination() throws Exception {
        try (var connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()
        )) {
            execute(
                    connection,
                    "insert into food(id,name,default_unit,origin,active,source,source_external_id,source_description) values (?,?,?,?::food_origin,?,?,?,?)",
                    UUID.randomUUID(), "Maçã verde", "g", "IMPORTED", true, "TEST", "green", "Maçã verde"
            );
            execute(
                    connection,
                    "insert into food(id,name,default_unit,origin,active,source,source_external_id,source_description) values (?,?,?,?::food_origin,?,?,?,?)",
                    UUID.randomUUID(), "Maçã vermelha", "g", "IMPORTED", true, "TEST", "red", "Maçã vermelha"
            );
            execute(
                    connection,
                    "insert into food(id,name,default_unit,origin,active,source,source_external_id,source_description) values (?,?,?,?::food_origin,?,?,?,?)",
                    UUID.randomUUID(), "Maçã inativa", "g", "IMPORTED", false, "TEST", "inactive", "Maçã inativa"
            );
        }

        var first = listFoods.execute(new ListFoodsInput(0, 1, "MACA"));
        var second = listFoods.execute(new ListFoodsInput(1, 1, "MACA"));

        assertEquals(2, first.totalElements());
        assertEquals("Maçã verde", first.content().getFirst().name());
        assertEquals("Maçã vermelha", second.content().getFirst().name());
        assertTrue(
                listFoods.execute(new ListFoodsInput(0, 10, "banana inexistente"))
                        .content().isEmpty()
        );
    }

    @Test
    void rejectsDifferentUnitsForTheSameNutrientCode() throws Exception {
        UUID foodId = UUID.randomUUID();
        UUID firstPortion = UUID.randomUUID();
        UUID secondPortion = UUID.randomUUID();
        try (var connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()
        )) {
            execute(
                    connection,
                    "insert into food(id,name,default_unit,origin,source,source_external_id,source_description) values (?,?,?,'IMPORTED'::food_origin,?,?,?)",
                    foodId, "Nutrient unit test", "g", "TEST", foodId.toString(), "Nutrient unit test"
            );
            execute(
                    connection, "insert into food_portion(id,food_id,label,quantity,unit) values (?,?,?,100,'g')",
                    firstPortion, foodId, "First 100 g"
            );
            execute(
                    connection, "insert into food_portion(id,food_id,label,quantity,unit) values (?,?,?,100,'g')",
                    secondPortion, foodId, "Second 100 g"
            );
            execute(
                    connection,
                    "insert into food_portion_nutrient(id,food_portion_id,nutrient_code,amount,unit,value_status) values (?,?,?,?,?,'VALUE')",
                    UUID.randomUUID(), firstPortion, "test_nutrient", 10, "mg"
            );

            assertThrows(
                    SQLException.class, () -> execute(
                            connection,
                            "insert into food_portion_nutrient(id,food_portion_id,nutrient_code,amount,unit,value_status) values (?,?,?,?,?,'VALUE')",
                            UUID.randomUUID(), secondPortion, "test_nutrient", 1, "g"
                    )
            );
        }
    }

    @Test
    void persistsFoodDetailsAndEnforcesPositivePortionQuantity() throws Exception {
        UUID accountId = UUID.randomUUID();
        UUID foodId = UUID.randomUUID();
        try (var connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()
        )) {
            execute(
                    connection,
                    "insert into user_account(id,email,password_hash,role) values (?,?,?,?::user_role)",
                    accountId, "patient@example.com", "hash", "PATIENT"
            );
            execute(
                    connection,
                    "insert into food(id,name,default_unit,notes,preparation_method,created_by_user_id)"
                            + " values (?,?,?,?,?,?)",
                    foodId, "Arroz", "g", "Sem sal", "Cozinhar em água", accountId
            );

            try (var statement = connection.prepareStatement(
                    "select notes,preparation_method,created_by_user_id,origin from food where id=?"
            )) {
                statement.setObject(1, foodId);
                try (var result = statement.executeQuery()) {
                    assertTrue(result.next());
                    assertEquals("Sem sal", result.getString("notes"));
                    assertEquals("Cozinhar em água", result.getString("preparation_method"));
                    assertEquals(accountId, result.getObject("created_by_user_id", UUID.class));
                    assertEquals("IMPORTED", result.getString("origin"));
                }
            }

            assertThrows(
                    SQLException.class, () -> execute(
                            connection,
                            "insert into food_portion(id,food_id,label,quantity,unit) values (?,?,?,?,?)",
                            UUID.randomUUID(), foodId, "Porção inválida", 0, "g"
                    )
            );
        }
    }

    @Test
    void enforcesUniqueSourceExternalId() throws Exception {
        try (var connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()
        )) {
            execute(
                    connection,
                    "insert into food(id,name,default_unit,origin,source,source_external_id,"
                            + "source_description,normalized_name) values (?,?,?,?::food_origin,?,?,?,?)",
                    UUID.randomUUID(), "Arroz", "100 g", "IMPORTED", "TACO", "1",
                    "Arroz, integral, cozido", "arroz integral cozido"
            );

            assertThrows(
                    SQLException.class, () -> execute(
                            connection,
                            "insert into food(id,name,default_unit,origin,source,source_external_id,"
                                    + "source_description) values (?,?,?,?::food_origin,?,?,?)",
                            UUID.randomUUID(), "Outro arroz", "100 g", "IMPORTED", "TACO", "1",
                            "Outro arroz"
                    )
            );
        }
    }

    @Test
    void persistsNutrientsAndKeepsRepeatedImportIdempotent() {
        var useCase = new ImportTacoCatalogUseCase(new SyncImportedFoodsUseCase(foods, portions));
        var nutrients = Map.of(
                "sodium", new NutrientValue(
                        new BigDecimal("1.25"), "mg", NutrientValueStatus.VALUE, "1.25"
                )
        );
        var spreadsheet = new TacoSpreadsheetData(
                1,
                List.of(new TacoFoodRecord("9999", "Alimento teste", "Testes", nutrients)),
                List.of()
        );

        var first = useCase.execute(new ImportTacoCatalogInput(spreadsheet, false));
        var second = useCase.execute(new ImportTacoCatalogInput(spreadsheet, false));
        var changedSpreadsheet = new TacoSpreadsheetData(
                1,
                List.of(
                        new TacoFoodRecord(
                                "9999", "Alimento teste", "Testes",
                                Map.of(
                                        "sodium", new NutrientValue(
                                                new BigDecimal("2.50"), "mg", NutrientValueStatus.VALUE, "2.50"
                                        )
                                )
                        )
                ),
                List.of()
        );
        var third = useCase.execute(new ImportTacoCatalogInput(changedSpreadsheet, false));
        var persistedPortion = portions.findImportedByFoodIds(
                List.of(CatalogIds.tacoFood("9999"))
        ).getFirst();
        var persisted = foods.findById(CatalogIds.tacoFood("9999")).orElseThrow();

        assertEquals(1, first.inserted());
        assertEquals(1, second.skipped());
        assertEquals(1, third.updated());
        assertEquals(
                0,
                new BigDecimal("2.50").compareTo(persistedPortion.getNutrients().get("sodium").amount())
        );
        assertNull(persisted.getCreatedByUserId());

        try (var connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()
        );
                var statement = connection.prepareStatement(
                        "select count(*) from food_portion_nutrient nutrient "
                                + "join food_portion portion on portion.id=nutrient.food_portion_id "
                                + "where portion.food_id=?"
                )) {
            statement.setObject(1, CatalogIds.tacoFood("9999"));
            try (var result = statement.executeQuery()) {
                assertTrue(result.next());
                assertEquals(1, result.getInt(1));
            }

            execute(
                    connection,
                    "delete from food where id=?",
                    CatalogIds.tacoFood("9999")
            );

            statement.setObject(1, CatalogIds.tacoFood("9999"));
            try (var result = statement.executeQuery()) {
                assertTrue(result.next());
                assertEquals(0, result.getInt(1));
            }

            try (var portionStatement = connection.prepareStatement(
                    "select count(*) from food_portion where food_id=?"
            )) {
                portionStatement.setObject(1, CatalogIds.tacoFood("9999"));
                try (var result = portionStatement.executeQuery()) {
                    assertTrue(result.next());
                    assertEquals(0, result.getInt(1));
                }
            }
        } catch (SQLException exception) {
            fail(exception);
        }
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "MONGODB_URI", matches = ".+")
    void importsTheConfiguredMongoCatalogEndToEnd() {
        var properties = new MongoImportProperties();
        properties.setUri(System.getenv("MONGODB_URI"));
        properties.setDatabase(System.getenv().getOrDefault("MONGODB_DATABASE", "tbca"));
        properties.setCollection(System.getenv().getOrDefault("MONGODB_COLLECTION", "foods"));
        var data = new MongoCatalogReader(properties).read();
        var importer = new ImportMongoCatalogUseCase(syncImportedFoods);

        var first = importer.execute(new ImportMongoCatalogInput(data, false));
        var second = importer.execute(new ImportMongoCatalogInput(data, false));

        assertFalse(first.aborted());
        assertEquals(data.records().size(), first.inserted());
        assertEquals(data.records().size(), second.skipped());
        assertTrue(
                portions.findImportedByFoodIds(
                        data.records().stream()
                                .map(record -> CatalogIds.importedFood("TBCA", record.externalId()))
                                .toList()
                ).stream().anyMatch(
                        portion -> portion.getNutrients().values().stream()
                                .anyMatch(value -> value.status() == NutrientValueStatus.NOT_ANALYZED)
                )
        );
    }

    private static void execute(
            java.sql.Connection connection, String sql, Object... values
    ) throws SQLException {
        connection.setAutoCommit(true);
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < values.length; index++) {
                statement.setObject(index + 1, values[index]);
            }
            statement.executeUpdate();
        }
    }
}
