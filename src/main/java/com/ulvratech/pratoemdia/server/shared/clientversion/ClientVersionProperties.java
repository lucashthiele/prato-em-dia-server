package com.ulvratech.pratoemdia.server.shared.clientversion;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/** Minimum client version per platform ({@code client.min-versions.web=1.2.0}). */
@ConfigurationProperties(prefix = "client")
public record ClientVersionProperties(Map<String, String> minVersions) {
    public ClientVersionProperties {
        minVersions = minVersions == null
                ? Map.of()
                : minVersions.entrySet().stream().collect(
                        Collectors.toUnmodifiableMap(
                                e -> e.getKey().toLowerCase(Locale.ROOT), e -> e.getValue().strip()
                        )
                );
    }
}
