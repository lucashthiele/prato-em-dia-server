package com.ulvratech.pratoemdia.server.features.importer.infrastructure.config;

import com.ulvratech.pratoemdia.server.features.catalog.application.port.in.SyncImportedFoods;
import com.ulvratech.pratoemdia.server.features.importer.application.port.in.ImportTacoCatalog;
import com.ulvratech.pratoemdia.server.features.importer.application.usecase.ImportTacoCatalogUseCase;
import com.ulvratech.pratoemdia.server.features.importer.infrastructure.persistence.adapter.ImportRunJpaRepository;
import com.ulvratech.pratoemdia.server.features.importer.infrastructure.taco.TacoCatalogImportRunner;
import com.ulvratech.pratoemdia.server.features.importer.infrastructure.taco.TacoNutrientValueParser;
import com.ulvratech.pratoemdia.server.features.importer.infrastructure.taco.TacoSpreadsheetReader;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("catalog-import")
@EnableConfigurationProperties(TacoImportProperties.class)
public class ImporterConfiguration {
    @Bean
    TacoNutrientValueParser tacoNutrientValueParser() {
        return new TacoNutrientValueParser();
    }

    @Bean
    TacoSpreadsheetReader tacoSpreadsheetReader(TacoNutrientValueParser valueParser) {
        return new TacoSpreadsheetReader(valueParser);
    }

    @Bean
    ImportTacoCatalog importTacoCatalog(SyncImportedFoods catalog) {
        return new ImportTacoCatalogUseCase(catalog);
    }

    @Bean
    ApplicationRunner tacoCatalogImportRunner(
            TacoImportProperties properties,
            TacoSpreadsheetReader reader,
            ImportTacoCatalog importCatalog,
            ImportRunJpaRepository runs
    ) {
        return new TacoCatalogImportRunner(properties, reader, importCatalog, runs);
    }
}
