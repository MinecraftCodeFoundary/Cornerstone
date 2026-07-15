package org.yeyao.cornerstone.moderation;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Immutable, append-only moderation record. Revocation preserves queryable history. */
public record Punishment(UUID id, PunishmentType type, UUID targetId, String targetName,
                         Optional<UUID> actorId, String actorName, Instant issuedAt,
                         Optional<Instant> expiresAt, String reason, boolean revoked) {
    public boolean activeAt(Instant time) { return !revoked && expiresAt.map(expiry -> expiry.isAfter(time)).orElse(true); }
}
