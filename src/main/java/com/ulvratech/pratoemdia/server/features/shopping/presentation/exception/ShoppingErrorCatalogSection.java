package com.ulvratech.pratoemdia.server.features.shopping.presentation.exception;

import com.ulvratech.pratoemdia.server.shared.errorcatalog.ErrorCatalogEntry;
import com.ulvratech.pratoemdia.server.shared.errorcatalog.ErrorCatalogSection;
import java.util.List;
import org.springframework.stereotype.Component;

import static com.ulvratech.pratoemdia.server.shared.errorcatalog.ErrorCatalogEntry.error;

@Component
public class ShoppingErrorCatalogSection implements ErrorCatalogSection {
    @Override
    public List<ErrorCatalogEntry> entries() {
        return List.of(
                error(
                        "SHOPPING_LIST_NOT_FOUND", 404, "O plano não tem lista de compras ativa.",
                        "GET /api/v1/patients/me/weekly-plans/{planId}/shopping-list"
                ),
                error(
                        "SHOPPING_PLAN_NOT_FOUND", 404,
                        "O plano não existe, foi excluído ou pertence a outro paciente.",
                        "/api/v1/patients/me/weekly-plans/{planId}/shopping-list"
                ),
                error(
                        "SHOPPING_ACCESS_DENIED", 403, "A conta não é de um paciente com perfil.",
                        "/api/v1/patients/me/weekly-plans/{planId}/shopping-list"
                )
        );
    }
}
