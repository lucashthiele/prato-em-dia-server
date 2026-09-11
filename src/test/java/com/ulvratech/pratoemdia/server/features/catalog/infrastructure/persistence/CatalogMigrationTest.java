package com.ulvratech.pratoemdia.server.features.catalog.infrastructure.persistence;

import com.ulvratech.pratoemdia.server.features.catalog.application.usecase.SyncImportedFoodsUseCase;
import com.ulvratech.pratoemdia.server.features.catalog.domain.CatalogIds;
import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.NutrientValue;
import com.ulvratech.pratoemdia.server.features.catalog.domain.enums.NutrientValueStatus;
import com.ulvratech.pratoemdia.server.features.catalog.domain.port.out.FoodRepository;
import com.ulvratech.pratoemdia.server.features.importer.application.dto.ImportTacoCatalogInput;
import com.ulvratech.pratoemdia.server.features.importer.application.dto.TacoFoodRecord;
import com.ulvratech.pratoemdia.server.features.importer.application.dto.TacoSpreadsheetData;
import com.ulvratech.pratoemdia.server.features.importer.application.usecase.ImportTacoCatalogUseCase;
import org.junit.jupiter.api.Test;
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

import static org.junit.jupiter.api.Assertions.*;

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

            assertThrows(SQLException.class, () -> execute(
                connection,
                "insert into food_portion(id,food_id,label,quantity,unit) values (?,?,?,?,?)",
                UUID.randomUUID(), foodId, "Porção inválida", 0, "g"
            ));
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

            assertThrows(SQLException.class, () -> execute(
                connection,
                "insert into food(id,name,default_unit,origin,source,source_external_id,"
                    + "source_description) values (?,?,?,?::food_origin,?,?,?)",
                UUID.randomUUID(), "Outro arroz", "100 g", "IMPORTED", "TACO", "1",
                "Outro arroz"
            ));
        }
    }

    @Test
    void persistsNutrientsAndKeepsRepeatedImportIdempotent() {
        var useCase = new ImportTacoCatalogUseCase(new SyncImportedFoodsUseCase(foods));
        var nutrients = Map.of("sodium", new NutrientValue(
            new BigDecimal("1.25"), "mg", NutrientValueStatus.VALUE, "1.25"
        ));
        var spreadsheet = new TacoSpreadsheetData(
            1,
            List.of(new TacoFoodRecord("9999", "Alimento teste", "Testes", nutrients)),
            List.of()
        );

        var first = useCase.execute(new ImportTacoCatalogInput(spreadsheet, false));
        var second = useCase.execute(new ImportTacoCatalogInput(spreadsheet, false));
        var changedSpreadsheet = new TacoSpreadsheetData(
            1,
            List.of(new TacoFoodRecord(
                "9999", "Alimento teste", "Testes",
                Map.of("sodium", new NutrientValue(
                    new BigDecimal("2.50"), "mg", NutrientValueStatus.VALUE, "2.50"
                ))
            )),
            List.of()
        );
        var third = useCase.execute(new ImportTacoCatalogInput(changedSpreadsheet, false));
        var persisted = foods.findById(CatalogIds.tacoFood("9999")).orElseThrow();

        assertEquals(1, first.inserted());
        assertEquals(1, second.skipped());
        assertEquals(1, third.updated());
        assertEquals(
            0,
            new BigDecimal("2.50").compareTo(persisted.getNutrients().get("sodium").amount())
        );
        assertNull(persisted.getCreatedByUserId());

        try (var connection = DriverManager.getConnection(
            POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()
        ); var statement = connection.prepareStatement(
            "select count(*) from food_nutrient where food_id=?"
        )) {
            statement.setObject(1, CatalogIds.tacoFood("9999"));
            try (var result = statement.executeQuery()) {
                assertTrue(result.next());
                assertEquals(1, result.getInt(1));
            }

            execute(
                connection,
                "insert into food_portion(id,food_id,label,quantity,unit) values (?,?,?,?,?)",
                UUID.randomUUID(), CatalogIds.tacoFood("9999"), "100 g", 100, "g"
            );

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
