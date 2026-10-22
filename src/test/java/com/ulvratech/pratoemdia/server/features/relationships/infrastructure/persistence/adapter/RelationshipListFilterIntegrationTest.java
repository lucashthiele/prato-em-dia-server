package com.ulvratech.pratoemdia.server.features.relationships.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.relationships.domain.entity.RelationshipFilter;
import com.ulvratech.pratoemdia.server.features.relationships.domain.entity.RelationshipView;
import com.ulvratech.pratoemdia.server.features.relationships.domain.enums.RelationshipEmailStatus;
import com.ulvratech.pratoemdia.server.features.relationships.domain.enums.RelationshipSort;
import com.ulvratech.pratoemdia.server.features.relationships.domain.port.out.RelationshipRepository;
import com.ulvratech.pratoemdia.server.shared.pagination.domain.PageRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "security.jwt.secret=01234567890123456789012345678901",
        "spring.docker.compose.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate"
})
class RelationshipListFilterIntegrationTest {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    static final UUID NUTRITIONIST_ACCOUNT = UUID.randomUUID();
    static final UUID OTHER_NUTRITIONIST_ACCOUNT = UUID.randomUUID();

    @Autowired
    RelationshipRepository repository;

    static boolean seeded;

    @BeforeEach
    void seed() throws SQLException {
        if (seeded)
            return;
        seeded = true;
        try (Connection c = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()
        )) {
            c.setAutoCommit(true);
            var nutritionist = nutritionist(c, NUTRITIONIST_ACCOUNT, "n@example.com");
            var other = nutritionist(c, OTHER_NUTRITIONIST_ACCOUNT, "other@example.com");
            // created oldest -> newest
            var ana = patient(c, nutritionist, "Ana Álvares", "ana@example.com", "SENT", "ACTIVE", 5);
            var bruno = patient(c, nutritionist, "bruno", "bruno@clinic.com", "FAILED", "ACTIVE", 4);
            var carla = patient(c, nutritionist, "Carla 100%", "carla@example.com", "PENDING", "ACTIVE", 3);
            patient(c, nutritionist, "Davi Encerrado", "davi@example.com", "FAILED", "ENDED", 2);
            patient(c, other, "Eva Outra", "eva@example.com", "FAILED", "ACTIVE", 1);
            var anaPrescription = prescription(c, nutritionist, ana);
            version(c, anaPrescription, 1, "PUBLISHED");
            version(c, prescription(c, nutritionist, bruno), 1, "DRAFT");
            var carlaPrescription = prescription(c, nutritionist, carla);
            version(c, carlaPrescription, 1, "ARCHIVED");
            version(c, carlaPrescription, 2, "DRAFT");
            version(c, carlaPrescription, 3, "DRAFT");
        }
    }

    @Test
    void defaultsToNewestFirstAndOnlyActiveRelationshipsOfTheNutritionist() {
        var page = list(RelationshipFilter.none());

        assertEquals(List.of("Carla 100%", "bruno", "Ana Álvares"), names(page.content()));
        assertEquals(3, page.totalElements());
    }

    @Test
    void sortsByCreatedAtAscendingAndByName() {
        assertEquals(
                List.of("Ana Álvares", "bruno", "Carla 100%"),
                names(list(filter(null, null, false, RelationshipSort.CREATED_AT_ASC)).content())
        );
        assertEquals(
                List.of("Ana Álvares", "bruno", "Carla 100%"),
                names(list(filter(null, null, false, RelationshipSort.NAME_ASC)).content())
        );
    }

    @Test
    void filtersByEmailDeliveryStatus() {
        var page = list(
                filter(null, RelationshipEmailStatus.FAILED, false, RelationshipSort.CREATED_AT_DESC)
        );

        assertEquals(List.of("bruno"), names(page.content()));
        assertEquals(1, page.totalElements());
    }

    @Test
    void filtersPatientsWithOpenDraftWithoutDuplicatingRows() {
        var page = list(filter(null, null, true, RelationshipSort.CREATED_AT_DESC));

        assertEquals(List.of("Carla 100%", "bruno"), names(page.content()));
        assertEquals(2, page.totalElements());
    }

    @Test
    void searchesNameIgnoringCaseAndAccentsAndEmail() {
        assertEquals(List.of("Ana Álvares"), names(search("alvares")));
        assertEquals(List.of("bruno"), names(search("clinic.com")));
    }

    @Test
    void treatsLikeWildcardsAsLiteralText() {
        assertEquals(List.of("Carla 100%"), names(search("100%")));
        assertEquals(List.of(), names(search("_")));
    }

    @Test
    void combinesFiltersAndReportsFilteredTotalAcrossPages() {
        var all = repository.findActiveByNutritionistAccount(
                NUTRITIONIST_ACCOUNT,
                filter(null, null, true, RelationshipSort.CREATED_AT_ASC),
                new PageRequest(0, 1)
        );
        assertEquals(List.of("bruno"), names(all.content()));
        assertEquals(2, all.totalElements());
        assertEquals(2, all.totalPages());

        var none = list(
                filter("ana", null, true, RelationshipSort.CREATED_AT_DESC)
        );
        assertEquals(0, none.totalElements());
    }

    private List<RelationshipView> search(String term) {
        return list(filter(term, null, false, RelationshipSort.CREATED_AT_DESC)).content();
    }

    private com.ulvratech.pratoemdia.server.shared.pagination.domain.PageResult<RelationshipView> list(
            RelationshipFilter f
    ) {
        return repository.findActiveByNutritionistAccount(
                NUTRITIONIST_ACCOUNT, f, new PageRequest(0, 20)
        );
    }

    private static RelationshipFilter filter(
            String search, RelationshipEmailStatus status, boolean draft, RelationshipSort sort
    ) {
        return new RelationshipFilter(search, status, draft, sort);
    }

    private static List<String> names(List<RelationshipView> views) {
        return views.stream().map(RelationshipView::patientFullName).toList();
    }

    private static UUID nutritionist(Connection c, UUID account, String email) throws SQLException {
        var id = UUID.randomUUID();
        exec(
                c, "insert into user_account(id,email,password_hash,role) values (?,?,?,?::user_role)",
                account, email, "hash", "NUTRITIONIST"
        );
        exec(
                c, "insert into nutritionist_profile(id,user_account_id,full_name) values (?,?,?)",
                id, account, "Nutri"
        );
        return id;
    }

    private static UUID patient(
            Connection c, UUID nutritionist, String name, String email, String emailStatus,
            String status, int daysAgo
    ) throws SQLException {
        var account = UUID.randomUUID();
        var id = UUID.randomUUID();
        exec(
                c, "insert into user_account(id,email,password_hash,role) values (?,?,?,?::user_role)",
                account, email, "hash", "PATIENT"
        );
        exec(
                c, "insert into patient_profile(id,user_account_id,full_name) values (?,?,?)",
                id, account, name
        );
        exec(
                c, "insert into nutritionist_patient_relationship(id,nutritionist_id,patient_id,"
                        + "status,email_delivery_status,created_at,ended_at,ended_by_user_account_id)"
                        + " values (?,?,?,?::relationship_status,?::relationship_email_status,"
                        + "current_timestamp - make_interval(days => ?),"
                        + "case when ?='ENDED' then current_timestamp end,"
                        + "case when ?='ENDED' then ?::uuid end)",
                UUID.randomUUID(), nutritionist, id, status, emailStatus, daysAgo, status, status,
                account
        );
        return id;
    }

    private static UUID prescription(Connection c, UUID nutritionist, UUID patient)
            throws SQLException {
        var id = UUID.randomUUID();
        exec(
                c, "insert into prescription(id,patient_id,nutritionist_id) values (?,?,?)",
                id, patient, nutritionist
        );
        return id;
    }

    private static void version(Connection c, UUID prescription, int number, String status)
            throws SQLException {
        exec(
                c, "insert into prescription_version(id,prescription_id,version_number,status,"
                        + "created_by_user_id,published_at,archived_at)"
                        + " values (?,?,?,?::prescription_version_status,?,"
                        + "case when ?='PUBLISHED' then current_timestamp end,"
                        + "case when ?='ARCHIVED' then current_timestamp end)",
                UUID.randomUUID(), prescription, number, status, NUTRITIONIST_ACCOUNT, status,
                status
        );
    }

    private static void exec(Connection c, String sql, Object... values) throws SQLException {
        try (PreparedStatement s = c.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++)
                s.setObject(i + 1, values[i]);
            s.executeUpdate();
        }
    }
}
