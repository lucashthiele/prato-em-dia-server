package com.ulvratech.pratoemdia.server.shared.clientversion;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/** Value of the {@code X-Client-Version} header: {@code <platform>/<version>}, e.g. {@code web/1.4.2}. */
public record ClientVersion(String platform, String version) {
    public static final String HEADER = "X-Client-Version";
    private static final Pattern FORMAT = Pattern.compile("^([A-Za-z][A-Za-z0-9_-]*)/(\\d+(?:\\.\\d+)*)(?:[-+].*)?$");

    public static Optional<ClientVersion> parse(String header) {
        if (header == null) {
            return Optional.empty();
        }
        var matcher = FORMAT.matcher(header.strip());
        if (!matcher.matches()) {
            return Optional.empty();
        }
        return Optional.of(new ClientVersion(matcher.group(1).toLowerCase(Locale.ROOT), matcher.group(2)));
    }

    public boolean isOlderThan(String minimum) {
        var mine = version.split("\\.");
        var other = minimum.strip().split("\\.");
        for (int i = 0; i < Math.max(mine.length, other.length); i++) {
            long a = i < mine.length ? Long.parseLong(mine[i]) : 0;
            long b = i < other.length ? Long.parseLong(other[i]) : 0;
            if (a != b) {
                return a < b;
            }
        }
        return false;
    }
}
