package com.ulvratech.pratoemdia.server.features.importer.application.dto;

import java.util.List;

public record CatalogImportReport(
        int rowsRead,
        int valid,
        int invalid,
        int inserted,
        int updated,
        int skipped,
        int failed,
        boolean dryRun,
        boolean aborted,
        List<CatalogImportIssue> issues) {
    public CatalogImportReport {
        issues = List.copyOf(issues);
    }

    public int toInsert() {
        return inserted;
    }

    public int toUpdate() {
        return updated;
    }

    public int toSkip() {
        return skipped;
    }

    public String formatted() {
        var text = new StringBuilder("Catalog import\n\n")
                .append(String.format("Records read:%6d%n", rowsRead))
                .append(String.format("Valid:       %6d%n", valid))
                .append(String.format("Invalid:     %6d%n%n", invalid));
        if (dryRun) {
            text.append(String.format("To insert:   %6d%n", inserted))
                    .append(String.format("To update:   %6d%n", updated))
                    .append(String.format("To skip:     %6d%n%n", skipped))
                    .append("Dry-run: no database changes performed.");
        } else if (aborted) {
            text.append(String.format("Would insert:%6d%n", inserted))
                    .append(String.format("Would update:%6d%n", updated))
                    .append(String.format("Would skip:  %6d%n", skipped))
                    .append(String.format("Failed:      %6d%n%n", failed))
                    .append("Import aborted: no food changes performed.");
        } else {
            text.append(String.format("Inserted:    %6d%n", inserted))
                    .append(String.format("Updated:     %6d%n", updated))
                    .append(String.format("Skipped:     %6d%n", skipped))
                    .append(String.format("Failed:      %6d", failed));
        }
        if (!issues.isEmpty()) {
            text.append("\n\nIssues:");
            issues.stream().limit(20).forEach(
                    issue -> text.append(
                            String.format(
                                    "%n- Record %d: %s", issue.recordNumber(), issue.message()
                            )
                    )
            );
        }
        return text.toString();
    }
}
