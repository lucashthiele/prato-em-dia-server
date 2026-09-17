package com.ulvratech.pratoemdia.server.features.importer.infrastructure.config;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "catalog.import")
public class TacoImportProperties {
  private Path file;
  private boolean dryRun = true;

  public Path getFile() {
    return file;
  }

  public void setFile(Path file) {
    this.file = file;
  }

  public boolean isDryRun() {
    return dryRun;
  }

  public void setDryRun(boolean dryRun) {
    this.dryRun = dryRun;
  }
}
