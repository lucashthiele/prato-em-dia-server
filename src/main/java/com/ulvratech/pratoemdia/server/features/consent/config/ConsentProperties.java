package com.ulvratech.pratoemdia.server.features.consent.config;

import com.ulvratech.pratoemdia.server.features.consent.domain.enums.ConsentDocumentType;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.EnumMap;
import java.util.Map;

/**
 * Current version of each legal document ({@code consent.current-versions.terms-of-use=...}).
 * Bumping a version makes every patient and nutritionist accept it again. {@code consent.enforced=false}
 * turns the {@code CONSENT_REQUIRED} block off (status and acceptance routes keep working).
 */
@ConfigurationProperties(prefix = "consent")
public record ConsentProperties(boolean enforced, Map<ConsentDocumentType, String> currentVersions) {
    public ConsentProperties {
        var all = new EnumMap<ConsentDocumentType, String>(ConsentDocumentType.class);
        if (currentVersions != null) {
            all.putAll(currentVersions);
        }
        for (var type : ConsentDocumentType.values()) {
            if (all.get(type) == null || all.get(type).isBlank()) {
                throw new IllegalArgumentException("consent.current-versions." + type + " must be set");
            }
        }
        currentVersions = Map.copyOf(all);
    }
}
