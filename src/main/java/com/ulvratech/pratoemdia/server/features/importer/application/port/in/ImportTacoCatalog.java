package com.ulvratech.pratoemdia.server.features.importer.application.port.in;

import com.ulvratech.pratoemdia.server.features.importer.application.dto.ImportTacoCatalogInput;
import com.ulvratech.pratoemdia.server.features.importer.application.dto.CatalogImportReport;

public interface ImportTacoCatalog {
    CatalogImportReport execute(ImportTacoCatalogInput input);
}
