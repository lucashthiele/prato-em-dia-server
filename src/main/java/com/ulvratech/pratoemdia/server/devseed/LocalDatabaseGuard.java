package com.ulvratech.pratoemdia.server.devseed;

import java.util.Set;
import java.util.regex.Pattern;

/** Refuses to seed anything but a database running on this machine. */
final class LocalDatabaseGuard {
    private static final Pattern HOST = Pattern.compile("^jdbc:postgresql://(?:\\[([^\\]]+)\\]|([^/:?,]+))");
    private static final Set<String> LOCAL_HOSTS = Set.of("localhost", "127.0.0.1", "::1", "0:0:0:0:0:0:0:1");

    private LocalDatabaseGuard() {
    }

    static void requireLocal(String jdbcUrl) {
        var matcher = HOST.matcher(jdbcUrl == null ? "" : jdbcUrl.trim());
        if (!matcher.find() || !LOCAL_HOSTS.contains(host(matcher).toLowerCase())) {
            throw new IllegalStateException("Dev seed only runs against a local database, got: " + jdbcUrl);
        }
    }

    private static String host(java.util.regex.Matcher matcher) {
        return matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
    }
}
