package com.ulvratech.pratoemdia.server.features.profiles.presentation.exception;

import com.ulvratech.pratoemdia.server.shared.errorcatalog.ErrorCatalogEntry;
import com.ulvratech.pratoemdia.server.shared.errorcatalog.ErrorCatalogSection;
import java.util.List;
import org.springframework.stereotype.Component;

import static com.ulvratech.pratoemdia.server.shared.errorcatalog.ErrorCatalogEntry.error;

@Component
public class ProfilesErrorCatalogSection implements ErrorCatalogSection {
    @Override
    public List<ErrorCatalogEntry> entries() {
        return List.of(
                error("PROFILE_NOT_FOUND", 404, "O perfil não existe.", "/api/v1/me", "/api/v1/patients/me"),
                error(
                        "PROFILE_NOT_AVAILABLE", 403, "O perfil não está disponível para esta conta.",
                        "/api/v1/me", "/api/v1/patients/me"
                ),
                error(
                        "PROFILE_FIELDS_NOT_ALLOWED", 400,
                        "A requisição tenta alterar campos que o papel da conta não pode editar.", "PUT /api/v1/me"
                ),
                error("PROFILE_INVALID_VALUE", 400, "Um ou mais valores do perfil são inválidos.", "/api/v1/me")
        );
    }
}
