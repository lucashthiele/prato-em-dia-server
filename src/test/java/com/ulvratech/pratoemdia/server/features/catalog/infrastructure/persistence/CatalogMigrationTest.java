package com.ulvratech.pratoemdia.server.features.catalog.infrastructure.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

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
