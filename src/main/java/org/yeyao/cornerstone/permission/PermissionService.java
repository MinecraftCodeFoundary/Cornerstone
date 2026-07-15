package org.yeyao.cornerstone.permission;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;

import java.util.UUID;

/**
 * The sole authorization boundary for Cornerstone commands.
 * LuckPerms is mandatory; no vanilla OP-level fallback is deliberately provided.
 */
public final class PermissionService {
    public boolean has(CommandSourceStack source, String node) {
        if (source == null || node == null || !node.matches("[a-z0-9_.-]{1,128}")) return false;
        if (source.getEntity() == null) return true; // Dedicated console and RCON are server operators.
        if (!(source.getEntity() instanceof ServerPlayer player)) return false;
        try {
            LuckPerms luckPerms = LuckPermsProvider.get();
            User user = luckPerms.getUserManager().getUser(player.getUUID());
            return user != null && user.getCachedData().getPermissionData().checkPermission(node).asBoolean();
        } catch (IllegalStateException unavailable) {
            // Fail closed if a required service is not ready during startup or shutdown.
            return false;
        }
    }
    /** Permission check for lifecycle gates where no CommandSourceStack exists yet. */
    public boolean has(UUID playerId, String node) {
        if (playerId == null || node == null || !node.matches("[a-z0-9_.-]{1,128}")) return false;
        try {
            LuckPerms luckPerms = LuckPermsProvider.get(); User user = luckPerms.getUserManager().getUser(playerId);
            return user != null && user.getCachedData().getPermissionData().checkPermission(node).asBoolean();
        } catch (IllegalStateException unavailable) { return false; }
    }
}
