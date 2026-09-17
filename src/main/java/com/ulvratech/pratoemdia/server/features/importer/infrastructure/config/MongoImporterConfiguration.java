package com.ulvratech.pratoemdia.server.features.importer.infrastructure.config;

import com.ulvratech.pratoemdia.server.features.catalog.application.port.in.SyncImportedFoods;
import com.ulvratech.pratoemdia.server.features.importer.application.port.in.ImportMongoCatalog;
import com.ulvratech.pratoemdia.server.features.importer.application.usecase.ImportMongoCatalogUseCase;
import com.ulvratech.pratoemdia.server.features.importer.infrastructure.mongodb.MongoCatalogImportRunner;
import com.ulvratech.pratoemdia.server.features.importer.infrastructure.mongodb.MongoCatalogReader;
import com.ulvratech.pratoemdia.server.features.importer.infrastructure.persistence.adapter.ImportRunJpaRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("mongodb-catalog-import")
@EnableConfigurationProperties(MongoImportProperties.class)
public class MongoImporterConfiguration {
  @Bean
  MongoCatalogReader mongoCatalogReader(MongoImportProperties properties) {
    return new MongoCatalogReader(properties);
  }

  @Bean
  ImportMongoCatalog importMongoCatalog(SyncImportedFoods catalog) {
    return new ImportMongoCatalogUseCase(catalog);
  }

  @Bean
  ApplicationRunner mongoCatalogImportRunner(
      MongoImportProperties properties,
      MongoCatalogReader reader,
      ImportMongoCatalog importCatalog,
      ImportRunJpaRepository runs) {
    return new MongoCatalogImportRunner(properties, reader, importCatalog, runs);
  }
}
