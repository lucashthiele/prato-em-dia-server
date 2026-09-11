package com.ulvratech.pratoemdia.server.features.importer.infrastructure.persistence.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "catalog_import_run")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportRunModel {
    @Id
    private UUID id;
    @Column(nullable = false, length = 32)
    private String source;
    @Column(name = "source_version", length = 100)
    private String sourceVersion;
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;
    @Column(name = "file_checksum", nullable = false, length = 64)
    private String fileChecksum;
    @Column(name = "dry_run", nullable = false)
    private boolean dryRun;
    @Column(name = "started_at", nullable = false)
    private Instant startedAt;
    @Column(name = "finished_at")
    private Instant finishedAt;
    @Column(nullable = false, length = 32)
    private String status;
    @Column(name = "rows_read", nullable = false)
    private int rowsRead;
    @Column(name = "rows_inserted", nullable = false)
    private int rowsInserted;
    @Column(name = "rows_updated", nullable = false)
    private int rowsUpdated;
    @Column(name = "rows_skipped", nullable = false)
    private int rowsSkipped;
    @Column(name = "rows_failed", nullable = false)
    private int rowsFailed;
}
