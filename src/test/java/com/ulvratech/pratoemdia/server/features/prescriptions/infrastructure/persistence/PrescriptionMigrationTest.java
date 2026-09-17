package com.ulvratech.pratoemdia.server.features.prescriptions.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ulvratech.pratoemdia.server.features.prescriptions.domain.entity.PrescribedMeal;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.entity.PrescribedMealItem;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.entity.Prescription;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.port.out.PrescriptionRepository;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(
    properties = {
      "security.jwt.secret=01234567890123456789012345678901",
      "spring.docker.compose.enabled=false",
      "spring.jpa.hibernate.ddl-auto=validate"
    })
class PrescriptionMigrationTest {
  @Container @ServiceConnection
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

  @Autowired PrescriptionRepository repository;

  private static void execute(Connection connection, String sql, Object... values)
      throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      for (int index = 0; index < values.length; index++)
        statement.setObject(index + 1, values[index]);
      statement.executeUpdate();
    }
  }

  @Test
  void persistsTheAggregateAndDatabaseProtectsPublishedContent() throws Exception {
    UUID nutritionistAccount = UUID.randomUUID();
    UUID patientAccount = UUID.randomUUID();
    UUID nutritionist = UUID.randomUUID();
    UUID patient = UUID.randomUUID();
    UUID food = UUID.randomUUID();
    try (var connection = connection()) {
      execute(
          connection,
          "insert into user_account(id,email,password_hash,role) values (?,?,?,?::user_role)",
          nutritionistAccount,
          "nutritionist-prescription@example.com",
          "hash",
          "NUTRITIONIST");
      execute(
          connection,
          "insert into user_account(id,email,password_hash,role) values (?,?,?,?::user_role)",
          patientAccount,
          "patient-prescription@example.com",
          "hash",
          "PATIENT");
      execute(
          connection,
          "insert into nutritionist_profile(id,user_account_id,full_name) values (?,?,?)",
          nutritionist,
          nutritionistAccount,
          "Nutritionist");
      execute(
          connection,
          "insert into patient_profile(id,user_account_id,full_name) values (?,?,?)",
          patient,
          patientAccount,
          "Patient");
      execute(
          connection,
          "insert into food(id,name,default_unit,created_by_user_id,origin) values (?,?,?,?,?::food_origin)",
          food,
          "Rice",
          "g",
          nutritionistAccount,
          "NUTRITIONIST");
    }

    var meal =
        PrescribedMeal.create(
            "Lunch",
            0,
            null,
            null,
            List.of(
                PrescribedMealItem.create(
                    food, null, new BigDecimal("100"), "g", 0, null, List.of())));
    var saved =
        repository.save(
            Prescription.create(
                patient, nutritionist, nutritionistAccount, "Initial", null, List.of(meal)));
    UUID versionId = saved.getVersions().getFirst().getId();
    saved.publish(versionId, Instant.parse("2026-09-11T12:00:00Z"));
    repository.save(saved);

    try (var connection = connection()) {
      assertThrows(
          SQLException.class,
          () ->
              execute(
                  connection,
                  "update prescription_version set title=? where id=?",
                  "Changed",
                  versionId));
    }
    assertEquals(
        "Initial",
        repository.findByVersionId(versionId).orElseThrow().requireVersion(versionId).getTitle());
  }

  private Connection connection() throws SQLException {
    return DriverManager.getConnection(
        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
  }
}
