package com.ulvratech.pratoemdia.server.features.planning.presentation.exception;

import com.ulvratech.pratoemdia.server.shared.errorcatalog.ErrorCatalogEntry;
import com.ulvratech.pratoemdia.server.shared.errorcatalog.ErrorCatalogSection;
import java.util.List;
import org.springframework.stereotype.Component;

import static com.ulvratech.pratoemdia.server.shared.errorcatalog.ErrorCatalogEntry.error;

@Component
public class PlanningErrorCatalogSection implements ErrorCatalogSection {
    @Override
    public List<ErrorCatalogEntry> entries() {
        return List.of(
                error(
                        "WEEKLY_PLAN_NOT_FOUND", 404, "O plano semanal não existe ou foi excluído.",
                        "/api/v1/patients/me/weekly-plans/**"
                ),
                error(
                        "PLANNING_ACCESS_DENIED", 403, "A conta não tem acesso a este planejamento.",
                        "/api/v1/patients/me/weekly-plans/**"
                ),
                error(
                        "PLANNING_CONFLICT", 409, "O estado atual do plano não permite a alteração pedida.",
                        "/api/v1/patients/me/weekly-plans/**"
                ),
                error(
                        "WEEKLY_PLAN_GENERATION_CONFLICT", 409,
                        "Outra geração do plano para o mesmo período está em andamento. "
                                + "O cliente deve consultar o plano em instantes em vez de gerar de novo.",
                        "POST /api/v1/patients/me/weekly-plans/generate"
                ),
                error(
                        "INVALID_PLANNING", 400, "A alteração do plano viola a prescrição ou os valores são inválidos.",
                        "/api/v1/patients/me/weekly-plans/**"
                )
        );
    }
}
