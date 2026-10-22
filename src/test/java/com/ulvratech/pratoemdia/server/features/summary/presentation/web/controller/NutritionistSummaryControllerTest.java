package com.ulvratech.pratoemdia.server.features.summary.presentation.web.controller;

import com.ulvratech.pratoemdia.server.features.summary.application.dto.GetSummaryInput;
import com.ulvratech.pratoemdia.server.features.summary.application.port.in.GetNutritionistSummary;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NutritionistSummaryControllerTest {
    @Test
    void delegatesWithAuthenticatedAccountAndDraftLimit() {
        var summary = mock(GetNutritionistSummary.class);
        var account = UUID.randomUUID();
        var jwt = Jwt.withTokenValue("token").header("alg", "none").subject(account.toString())
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();

        new NutritionistSummaryController(summary).get(jwt, 5);

        verify(summary).execute(new GetSummaryInput(account, 5));
    }
}
