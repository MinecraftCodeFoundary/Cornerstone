package org.yeyao.cornerstone.audit;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Immutable audit entry; never store complete raw command input or secrets here. */
public record AuditRecord(Instant time, String action, String actor, Optional<UUID> actorId,
                          String target, String dimension, String arguments, boolean success, String result) {
    public AuditRecord {
        if (action == null || action.isBlank()) throw new IllegalArgumentException("Audit action is required");
        arguments = truncate(arguments, 512); result = truncate(result, 512); target = truncate(target, 128); dimension = truncate(dimension, 128);
    }
    private static String truncate(String value, int maximum) { return value == null ? "" : value.substring(0, Math.min(value.length(), maximum)); }
}
