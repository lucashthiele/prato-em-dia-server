package com.ulvratech.pratoemdia.server.features.catalog.presentation.exception;

import com.ulvratech.pratoemdia.server.shared.errorcatalog.ErrorCatalogEntry;
import com.ulvratech.pratoemdia.server.shared.errorcatalog.ErrorCatalogSection;
import java.util.List;
import org.springframework.stereotype.Component;

import static com.ulvratech.pratoemdia.server.shared.errorcatalog.ErrorCatalogEntry.error;

@Component
public class CatalogErrorCatalogSection implements ErrorCatalogSection {
    @Override
    public List<ErrorCatalogEntry> entries() {
        return List.of(
                error("FOOD_NOT_FOUND", 404, "O alimento não existe.", "/api/v1/foods/**"),
                error(
                        "FOOD_ACCESS_DENIED", 403, "A conta não pode ver ou alterar este alimento.",
                        "/api/v1/foods/**"
                ),
                error(
                        "CATALOG_INVALID_VALUE", 400, "Um ou mais valores do alimento ou da porção são inválidos.",
                        "/api/v1/foods/**"
                )
        );
    }
}
