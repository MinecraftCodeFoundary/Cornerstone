package org.yeyao.cornerstone.teleport;

import net.minecraft.server.level.ServerPlayer;
import org.yeyao.cornerstone.data.StoredLocation;

/** Immutable context for teleport protection and economy integrations. */
public record TeleportContext(ServerPlayer player, StoredLocation destination, String purpose, long configuredCost) {
}
