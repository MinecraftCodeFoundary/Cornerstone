package org.yeyao.cornerstone.data;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Internal aggregate. Callers receive snapshots, never this mutable object. */
final class PlayerProfile {
    final UUID id;
    String lastKnownName;
    Instant lastOnline;
    StoredLocation lastLocation;
    final Map<String, Map<String, String>> modules = new LinkedHashMap<>();

    PlayerProfile(UUID id, String lastKnownName) {
        this.id = id;
        this.lastKnownName = lastKnownName;
        this.lastOnline = Instant.now();
    }
    PlayerDataSnapshot snapshot() {
        Map<String, Map<String, String>> copied = new LinkedHashMap<>();
        modules.forEach((name, values) -> copied.put(name, Map.copyOf(values)));
        return new PlayerDataSnapshot(id, lastKnownName, lastOnline, Optional.ofNullable(lastLocation), Map.copyOf(copied));
    }
}
