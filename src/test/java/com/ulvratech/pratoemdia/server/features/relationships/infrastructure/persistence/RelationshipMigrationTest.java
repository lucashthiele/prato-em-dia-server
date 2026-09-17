package com.ulvratech.pratoemdia.server.features.relationships.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.*;

import com.ulvratech.pratoemdia.server.features.identity.domain.entity.UserAccount;
import com.ulvratech.pratoemdia.server.features.identity.domain.enums.UserAccountRole;
import com.ulvratech.pratoemdia.server.features.identity.domain.enums.UserAccountStatus;
import com.ulvratech.pratoemdia.server.features.identity.domain.port.out.UserAccountRepository;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
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
class RelationshipMigrationTest {
  @Container @ServiceConnection
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

  @Autowired UserAccountRepository accounts;

  private static void insertAccount(Connection connection, UUID id, String email, String role)
      throws SQLException {
    execute(
        connection,
        "insert into user_account(id,email,password_hash,role) values (?,?,?,?::user_role)",
        id,
        email,
        "hash",
        role);
  }

  private static void execute(Connection connection, String sql, Object... values)
      throws SQLException {
    connection.setAutoCommit(true);
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      for (int i = 0; i < values.length; i++) statement.setObject(i + 1, values[i]);
      statement.executeUpdate();
    }
  }

  @Test
  void enforcesOneActiveNutritionistAndAllowsReassignmentAfterEnding() throws Exception {
    UUID nutritionistAccount1 = UUID.randomUUID();
    UUID nutritionistAccount2 = UUID.randomUUID();
    UUID patientAccount = UUID.randomUUID();
    UUID nutritionist1 = UUID.randomUUID();
    UUID nutritionist2 = UUID.randomUUID();
    UUID patient = UUID.randomUUID();
    UUID first = UUID.randomUUID();
    try (Connection connection =
        DriverManager.getConnection(
            POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())) {
      insertAccount(connection, nutritionistAccount1, "n1@example.com", "NUTRITIONIST");
      insertAccount(connection, nutritionistAccount2, "n2@example.com", "NUTRITIONIST");
      insertAccount(connection, patientAccount, "p@example.com", "PATIENT");
      execute(
          connection,
          "insert into nutritionist_profile(id,user_account_id,full_name) values (?,?,?)",
          nutritionist1,
          nutritionistAccount1,
          "N1");
      execute(
          connection,
          "insert into nutritionist_profile(id,user_account_id,full_name) values (?,?,?)",
          nutritionist2,
          nutritionistAccount2,
          "N2");
      execute(
          connection,
          "insert into patient_profile(id,user_account_id,full_name) values (?,?,?)",
          patient,
          patientAccount,
          "Patient");
      execute(
          connection,
          "insert into nutritionist_patient_relationship(id,nutritionist_id,patient_id)"
              + " values (?,?,?)",
          first,
          nutritionist1,
          patient);
      assertThrows(
          SQLException.class,
          () ->
              execute(
                  connection,
                  "insert into"
                      + " nutritionist_patient_relationship(id,nutritionist_id,patient_id)"
                      + " values (?,?,?)",
                  UUID.randomUUID(),
                  nutritionist2,
                  patient));
      execute(
          connection,
          "update nutritionist_patient_relationship set"
              + " status='ENDED',ended_at=current_timestamp,ended_by_user_account_id=?"
              + " where id=?",
          patientAccount,
          first);
      assertDoesNotThrow(
          () ->
              execute(
                  connection,
                  "insert into"
                      + " nutritionist_patient_relationship(id,nutritionist_id,patient_id)"
                      + " values (?,?,?)",
                  UUID.randomUUID(),
                  nutritionist2,
                  patient));
    }
  }

  @Test
  void persistsPasswordChangeFlagAcrossRepeatedCredentialUpdates() {
    var saved =
        accounts.save(
            new UserAccount(
                "flag-" + UUID.randomUUID() + "@example.com",
                "hash-1",
                UserAccountRole.PATIENT,
                UserAccountStatus.ACTIVE,
                true));
    saved.replacePassword("hash-2", true);
    accounts.save(saved);
    var first = accounts.findById(saved.getId()).orElseThrow();
    assertTrue(first.isPasswordChangeRequired());
    first.replacePassword("hash-3", false);
    accounts.save(first);
    assertFalse(accounts.findById(saved.getId()).orElseThrow().isPasswordChangeRequired());
  }
}
