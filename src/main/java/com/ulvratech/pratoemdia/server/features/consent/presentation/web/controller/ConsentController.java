package com.ulvratech.pratoemdia.server.features.consent.presentation.web.controller;

import com.ulvratech.pratoemdia.server.features.consent.application.dto.AcceptConsentInput;
import com.ulvratech.pratoemdia.server.features.consent.application.port.in.AcceptConsent;
import com.ulvratech.pratoemdia.server.features.consent.application.port.in.GetConsentStatus;
import com.ulvratech.pratoemdia.server.features.consent.domain.enums.ConsentDocumentType;
import com.ulvratech.pratoemdia.server.features.consent.presentation.web.request.AcceptConsentRequest;
import com.ulvratech.pratoemdia.server.features.consent.presentation.web.response.ConsentStatusResponse;
import com.ulvratech.pratoemdia.server.shared.openapi.OpenApiConfiguration;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/me/consents")
@SecurityRequirement(name = OpenApiConfiguration.COOKIE_AUTH)
@Tag(name = "Aceite de termos", description = "Aceite versionado dos termos de uso e da política de privacidade.")
public class ConsentController {
    private final GetConsentStatus getConsentStatus;
    private final AcceptConsent acceptConsent;

    public ConsentController(GetConsentStatus getConsentStatus, AcceptConsent acceptConsent) {
        this.getConsentStatus = getConsentStatus;
        this.acceptConsent = acceptConsent;
    }

    @GetMapping
    @Operation(summary = "Consultar aceites", description = "Lista cada documento com a versão vigente e se a versão já foi aceita. Enquanto upToDate for falso, a API responde 403 CONSENT_REQUIRED nas demais rotas de paciente e nutricionista.")
    ConsentStatusResponse status(@AuthenticationPrincipal Jwt jwt) {
        return ConsentStatusResponse.from(getConsentStatus.execute(UUID.fromString(jwt.getSubject())));
    }

    @PostMapping
    @Operation(summary = "Aceitar documento", description = "Registra o aceite da versão vigente de um documento. Repetir o aceite é inócuo; uma versão que não é a vigente retorna 409 CONSENT_VERSION_MISMATCH. Devolve o estado atualizado.")
    ConsentStatusResponse accept(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AcceptConsentRequest request) {
        var user = UUID.fromString(jwt.getSubject());
        acceptConsent.execute(
                new AcceptConsentInput(user, ConsentDocumentType.valueOf(request.documentType()), request.version())
        );
        return ConsentStatusResponse.from(getConsentStatus.execute(user));
    }
}
