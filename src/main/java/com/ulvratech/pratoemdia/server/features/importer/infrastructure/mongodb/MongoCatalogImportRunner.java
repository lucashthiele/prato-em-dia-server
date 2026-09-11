package com.ulvratech.pratoemdia.server.features.importer.infrastructure.mongodb;

import com.ulvratech.pratoemdia.server.features.importer.application.dto.ImportMongoCatalogInput;
import com.ulvratech.pratoemdia.server.features.importer.application.port.in.ImportMongoCatalog;
import com.ulvratech.pratoemdia.server.features.importer.application.usecase.ImportMongoCatalogUseCase;
import com.ulvratech.pratoemdia.server.features.importer.infrastructure.config.MongoImportProperties;
import com.ulvratech.pratoemdia.server.features.importer.infrastructure.persistence.adapter.ImportRunJpaRepository;
import com.ulvratech.pratoemdia.server.features.importer.infrastructure.persistence.model.ImportRunModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

import java.time.Instant;
import java.util.UUID;

public class MongoCatalogImportRunner implements ApplicationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(MongoCatalogImportRunner.class);

    private final MongoImportProperties properties;
    private final MongoCatalogReader reader;
    private final ImportMongoCatalog importCatalog;
    private final ImportRunJpaRepository runs;

    public MongoCatalogImportRunner(
        MongoImportProperties properties,
        MongoCatalogReader reader,
        ImportMongoCatalog importCatalog,
        ImportRunJpaRepository runs
    ) {
        this.properties = properties;
        this.reader = reader;
        this.importCatalog = importCatalog;
        this.runs = runs;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        var startedAt = Instant.now();
        var data = reader.read();
        var report = importCatalog.execute(
            new ImportMongoCatalogInput(data, properties.isDryRun())
        );

        if (!properties.isDryRun()) {
            runs.save(ImportRunModel.builder()
                .id(UUID.randomUUID())
                .source(ImportMongoCatalogUseCase.SOURCE)
                .sourceVersion(ImportMongoCatalogUseCase.SOURCE_VERSION)
                .sourceLocation(properties.getDatabase() + "." + properties.getCollection())
                .sourceChecksum(data.checksum())
                .dryRun(false)
                .startedAt(startedAt)
                .finishedAt(Instant.now())
                .status(report.aborted() ? "FAILED" : "COMPLETED")
                .rowsRead(report.rowsRead())
                .rowsInserted(report.aborted() ? 0 : report.inserted())
                .rowsUpdated(report.aborted() ? 0 : report.updated())
                .rowsSkipped(report.aborted() ? 0 : report.skipped())
                .rowsFailed(report.failed())
                .build());
        }
        LOGGER.info("{}", report.formatted());
    }
}
