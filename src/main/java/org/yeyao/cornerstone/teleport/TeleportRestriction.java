package org.yeyao.cornerstone.teleport;

import net.minecraft.server.level.ServerPlayer;
import org.yeyao.cornerstone.data.StoredLocation;

import java.util.Optional;

/** Extension point for combat tags or protection mods to deny a teleport. */
@FunctionalInterface
public interface TeleportRestriction {
    Optional<String> deny(ServerPlayer player, StoredLocation destination);
    default Optional<String> deny(TeleportContext context) { return deny(context.player(), context.destination()); }
}
