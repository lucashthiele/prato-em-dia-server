package com.ulvratech.pratoemdia.server.devseed;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalDatabaseGuardTest {
    @ParameterizedTest
    @ValueSource(strings = {
            "jdbc:postgresql://localhost:5432/pratoemdia",
            "jdbc:postgresql://127.0.0.1/pratoemdia?user=postgres",
            "jdbc:postgresql://[::1]:5432/pratoemdia"
    })
    void acceptsLocalHosts(String url) {
        assertThatCode(() -> LocalDatabaseGuard.requireLocal(url)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "jdbc:postgresql://db.example.com:5432/pratoemdia",
            "jdbc:postgresql://localhost.example.com/pratoemdia",
            "jdbc:postgresql://10.0.0.5/pratoemdia",
            "jdbc:mysql://localhost/pratoemdia",
            ""
    })
    void rejectsEverythingElse(String url) {
        assertThatThrownBy(() -> LocalDatabaseGuard.requireLocal(url)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsMissingUrl() {
        assertThatThrownBy(() -> LocalDatabaseGuard.requireLocal(null)).isInstanceOf(IllegalStateException.class);
    }
}
