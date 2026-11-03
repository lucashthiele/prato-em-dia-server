package com.ulvratech.pratoemdia.server.features.identity.presentation.exception;

import com.ulvratech.pratoemdia.server.shared.errorcatalog.ErrorCatalogEntry;
import com.ulvratech.pratoemdia.server.shared.errorcatalog.ErrorCatalogSection;
import java.util.List;
import org.springframework.stereotype.Component;

import static com.ulvratech.pratoemdia.server.shared.errorcatalog.ErrorCatalogEntry.error;

@Component
public class IdentityErrorCatalogSection implements ErrorCatalogSection {
    @Override
    public List<ErrorCatalogEntry> entries() {
        return List.of(
                error("AUTH_INVALID_CREDENTIALS", 401, "E-mail ou senha inválidos.", "POST /api/v1/auth/login"),
                error("AUTH_ACCOUNT_INACTIVE", 401, "A conta está inativa.", "POST /api/v1/auth/login"),
                error(
                        "AUTH_REFRESH_TOKEN_INVALID", 401, "O token de renovação é inválido ou foi revogado.",
                        "POST /api/v1/auth/refresh"
                ),
                error(
                        "AUTH_REFRESH_TOKEN_EXPIRED", 401, "O token de renovação expirou.",
                        "POST /api/v1/auth/refresh"
                ),
                error(
                        "IDENTITY_ACCOUNT_ALREADY_EXISTS", 409, "Já existe uma conta com este e-mail.",
                        "POST /api/v1/identity/useraccounts"
                ),
                error(
                        "PATIENT_ACCOUNT_CONFLICT", 409,
                        "A conta do paciente não está disponível para esta operação.",
                        "POST /api/v1/nutritionists/me/patients"
                ),
                error(
                        "INITIAL_PASSWORD_NOT_AVAILABLE", 409,
                        "A senha inicial do paciente não está mais disponível.",
                        "/api/v1/nutritionists/me/patients/**"
                )
        );
    }
}
