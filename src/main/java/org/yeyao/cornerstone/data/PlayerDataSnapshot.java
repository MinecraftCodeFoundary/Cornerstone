package org.yeyao.cornerstone.data;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Read-only profile view for feature modules and external integrations. */
public record PlayerDataSnapshot(UUID id, String lastKnownName, Instant lastOnline,
                                 Optional<StoredLocation> lastLocation,
                                 Map<String, Map<String, String>> moduleData) {
    public Optional<String> value(String module, String key) {
        return Optional.ofNullable(moduleData.getOrDefault(module, Map.of()).get(key));
    }
}
