package org.yeyao.cornerstone.api;

import java.util.UUID;

/** Published after a player profile or one of its module values changes. */
public record PlayerDataUpdatedEvent(UUID playerId, String module, String key) {
}
