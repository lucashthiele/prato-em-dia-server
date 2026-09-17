package com.ulvratech.pratoemdia.server.features.importer.infrastructure.taco;

import com.ulvratech.pratoemdia.server.features.importer.application.dto.ImportTacoCatalogInput;
import com.ulvratech.pratoemdia.server.features.importer.application.port.in.ImportTacoCatalog;
import com.ulvratech.pratoemdia.server.features.importer.application.usecase.ImportTacoCatalogUseCase;
import com.ulvratech.pratoemdia.server.features.importer.infrastructure.config.TacoImportProperties;
import com.ulvratech.pratoemdia.server.features.importer.infrastructure.persistence.adapter.ImportRunJpaRepository;
import com.ulvratech.pratoemdia.server.features.importer.infrastructure.persistence.model.ImportRunModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

public class TacoCatalogImportRunner implements ApplicationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(TacoCatalogImportRunner.class);

    private final TacoImportProperties properties;
    private final TacoSpreadsheetReader reader;
    private final ImportTacoCatalog importCatalog;
    private final ImportRunJpaRepository runs;

    public TacoCatalogImportRunner(
            TacoImportProperties properties,
            TacoSpreadsheetReader reader,
            ImportTacoCatalog importCatalog,
            ImportRunJpaRepository runs
    ) {
        this.properties = properties;
        this.reader = reader;
        this.importCatalog = importCatalog;
        this.runs = runs;
    }

    @Override
    public void run(ApplicationArguments arguments) throws Exception {
        if (properties.getFile() == null) {
            throw new IllegalArgumentException("catalog.import.file is required");
        }
        var path = properties.getFile().toAbsolutePath().normalize();
        String checksum = sha256(path);
        var startedAt = Instant.now();
        var data = reader.read(path);
        var report = importCatalog.execute(new ImportTacoCatalogInput(data, properties.isDryRun()));

        if (!properties.isDryRun()) {
            runs.save(
                    ImportRunModel.builder()
                            .id(UUID.randomUUID())
                            .source(ImportTacoCatalogUseCase.SOURCE)
                            .sourceVersion(ImportTacoCatalogUseCase.SOURCE_VERSION)
                            .sourceLocation(path.getFileName().toString())
                            .sourceChecksum(checksum)
                            .dryRun(false)
                            .startedAt(startedAt)
                            .finishedAt(Instant.now())
                            .status(report.aborted() ? "FAILED" : "COMPLETED")
                            .rowsRead(report.rowsRead())
                            .rowsInserted(report.aborted() ? 0 : report.inserted())
                            .rowsUpdated(report.aborted() ? 0 : report.updated())
                            .rowsSkipped(report.aborted() ? 0 : report.skipped())
                            .rowsFailed(report.failed())
                            .build()
            );
        }
        LOGGER.info("{}", report.formatted());
    }

    private String sha256(java.nio.file.Path path) throws IOException {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(Files.readAllBytes(path))
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
