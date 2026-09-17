package com.ulvratech.pratoemdia.server.features.importer.application.port.in;

import com.ulvratech.pratoemdia.server.features.importer.application.dto.CatalogImportReport;
import com.ulvratech.pratoemdia.server.features.importer.application.dto.ImportMongoCatalogInput;

public interface ImportMongoCatalog {
  CatalogImportReport execute(ImportMongoCatalogInput input);
}
