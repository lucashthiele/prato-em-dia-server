package com.ulvratech.pratoemdia.server.devseed;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Deterministic ids so a seeded row can always be found (and removed) again. */
final class SeedIds {
    private SeedIds() {
    }

    static UUID of(String key) {
        return UUID.nameUUIDFromBytes(("dev-seed:" + key).getBytes(StandardCharsets.UTF_8));
    }
}
