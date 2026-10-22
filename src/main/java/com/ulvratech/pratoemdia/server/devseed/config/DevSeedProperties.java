package com.ulvratech.pratoemdia.server.devseed.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "devseed")
public class DevSeedProperties {
    private String password = "Seed@12345";
    private Path credentialsFile = Path.of("docs/dev-seed-credentials.md");
    private boolean reset;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Path getCredentialsFile() {
        return credentialsFile;
    }

    public void setCredentialsFile(Path credentialsFile) {
        this.credentialsFile = credentialsFile;
    }

    public boolean isReset() {
        return reset;
    }

    public void setReset(boolean reset) {
        this.reset = reset;
    }
}
