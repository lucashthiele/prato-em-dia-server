package com.ulvratech.pratoemdia.server.features.tracking.presentation.exception;

import com.ulvratech.pratoemdia.server.shared.errorcatalog.ErrorCatalogEntry;
import com.ulvratech.pratoemdia.server.shared.errorcatalog.ErrorCatalogSection;
import java.util.List;
import org.springframework.stereotype.Component;

import static com.ulvratech.pratoemdia.server.shared.errorcatalog.ErrorCatalogEntry.error;
import static com.ulvratech.pratoemdia.server.shared.errorcatalog.ErrorCatalogEntry.reason;

@Component
public class TrackingErrorCatalogSection implements ErrorCatalogSection {
    @Override
    public List<ErrorCatalogEntry> entries() {
        return List.of(
                error(
                        "TRACKING_NOT_FOUND", 404, "A execução, refeição ou registro solicitado não existe.",
                        "/api/v1/patients/me/meal-executions/**", "/api/v1/planned-meals/**"
                ),
                error(
                        "TRACKING_FORBIDDEN", 403, "A conta não pode acessar este registro.",
                        "/api/v1/patients/me/**", "/api/v1/planned-meals/**"
                ),
                error(
                        "TRACKING_CONFLICT", 409,
                        "Versão, destino ou chave de idempotência conflitante na execução.",
                        "/api/v1/patients/me/meal-executions/**"
                ),
                error(
                        "TRACKING_INVALID", 400, "Os itens ou valores da execução são inválidos para a prescrição.",
                        "/api/v1/patients/me/**", "/api/v1/planned-meals/**"
                ),
                reason(
                        "NO_ALLOWED_FOODS",
                        "Valor de blockedReason: a categoria não tem itens permitidos no modo da prescrição "
                                + "(RESTRICTED ou SUBSTITUTIONS_ONLY), então o registro está bloqueado.",
                        "GET /api/v1/patients/me/daily-meal-categories"
                )
        );
    }
}
