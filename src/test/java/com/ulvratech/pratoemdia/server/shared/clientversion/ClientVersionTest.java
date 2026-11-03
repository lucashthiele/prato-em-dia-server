package com.ulvratech.pratoemdia.server.shared.clientversion;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientVersionTest {
    @Test
    void parsesPlatformAndVersionFromTheHeader() {
        var parsed = ClientVersion.parse("Web/1.4.2").orElseThrow();

        assertEquals("web", parsed.platform());
        assertEquals("1.4.2", parsed.version());
    }

    @Test
    void malformedHeadersAreIgnored() {
        assertTrue(ClientVersion.parse(null).isEmpty());
        assertTrue(ClientVersion.parse("").isEmpty());
        assertTrue(ClientVersion.parse("1.2.3").isEmpty());
        assertTrue(ClientVersion.parse("web/").isEmpty());
        assertTrue(ClientVersion.parse("web/abc").isEmpty());
    }

    @Test
    void comparesNumericSegmentsNotText() {
        assertTrue(ClientVersion.parse("web/1.9.5").orElseThrow().isOlderThan("1.10.0"));
        assertTrue(ClientVersion.parse("web/1.2").orElseThrow().isOlderThan("1.2.1"));
        assertFalse(ClientVersion.parse("web/1.2.0").orElseThrow().isOlderThan("1.2"));
        assertFalse(ClientVersion.parse("web/2.0.0").orElseThrow().isOlderThan("1.9.9"));
    }

    @Test
    void ignoresBuildAndPreReleaseSuffixes() {
        assertFalse(ClientVersion.parse("android/1.2.0+45").orElseThrow().isOlderThan("1.2.0"));
        assertTrue(ClientVersion.parse("android/1.1.9-beta.1").orElseThrow().isOlderThan("1.2.0"));
    }
}
