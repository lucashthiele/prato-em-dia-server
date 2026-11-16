package com.ulvratech.pratoemdia.server.features.relationships.presentation.exception;

import com.ulvratech.pratoemdia.server.shared.errorcatalog.ErrorCatalogEntry;
import com.ulvratech.pratoemdia.server.shared.errorcatalog.ErrorCatalogSection;
import java.util.List;
import org.springframework.stereotype.Component;

import static com.ulvratech.pratoemdia.server.shared.errorcatalog.ErrorCatalogEntry.error;

@Component
public class RelationshipsErrorCatalogSection implements ErrorCatalogSection {
    @Override
    public List<ErrorCatalogEntry> entries() {
        return List.of(
                error(
                        "RELATIONSHIP_NOT_FOUND", 404, "O vínculo solicitado não foi encontrado.",
                        "/api/v1/patients/me/relationship", "/api/v1/nutritionists/me/patients/**"
                ),
                error(
                        "RELATIONSHIP_CONFLICT", 409,
                        "O paciente tem vínculo ativo com outro nutricionista ou o vínculo está em um estado que não permite a operação.",
                        "/api/v1/patients/me/relationship", "/api/v1/nutritionists/me/patients/**"
                ),
                error(
                        "RELATIONSHIP_ALREADY_EXISTS", 409,
                        "O paciente já tem vínculo ativo com o nutricionista autenticado. O id do paciente vem em errors[0] (field=patientId, message=id).",
                        "POST /api/v1/nutritionists/me/patients"
                ),
                error(
                        "RELATIONSHIP_EMAIL_FAILED", 503,
                        "O e-mail do convite não pôde ser enfileirado ou enviado. Tente novamente mais tarde.",
                        "POST /api/v1/nutritionists/me/patients"
                ),
                error(
                        "RELATIONSHIP_INVALID_VALUE", 400, "Um ou mais valores do vínculo são inválidos.",
                        "/api/v1/nutritionists/me/patients/**"
                )
        );
    }
}
