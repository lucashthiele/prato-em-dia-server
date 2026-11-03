package com.ulvratech.pratoemdia.server.features.prescriptions.presentation.exception;

import com.ulvratech.pratoemdia.server.shared.errorcatalog.ErrorCatalogEntry;
import com.ulvratech.pratoemdia.server.shared.errorcatalog.ErrorCatalogSection;
import java.util.List;
import org.springframework.stereotype.Component;

import static com.ulvratech.pratoemdia.server.shared.errorcatalog.ErrorCatalogEntry.error;

@Component
public class PrescriptionsErrorCatalogSection implements ErrorCatalogSection {
    @Override
    public List<ErrorCatalogEntry> entries() {
        return List.of(
                error(
                        "PRESCRIPTION_NOT_FOUND", 404, "A prescrição solicitada não foi encontrada.",
                        "/api/v1/prescriptions/**"
                ),
                error(
                        "PRESCRIPTION_ACCESS_DENIED", 403, "A conta não tem acesso a esta prescrição.",
                        "/api/v1/prescriptions/**"
                ),
                error(
                        "PRESCRIPTION_CONFLICT", 409,
                        "A versão da prescrição mudou ou o estado atual não permite a operação.",
                        "/api/v1/prescriptions/**"
                ),
                error(
                        "INVALID_PRESCRIPTION_REFERENCE", 422,
                        "A prescrição referencia alimento, porção ou refeição inexistente ou inválida.",
                        "/api/v1/prescriptions/**"
                ),
                error(
                        "INVALID_PRESCRIPTION", 400, "Um ou mais valores da prescrição são inválidos.",
                        "/api/v1/prescriptions/**"
                )
        );
    }
}
