package org.yeyao.cornerstone.data;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Serializable position independent of a live world or entity. */
public record StoredLocation(String dimension, double x, double y, double z, float yaw, float pitch) {
    public StoredLocation {
        ResourceLocation.parse(dimension);
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) throw new IllegalArgumentException("Location coordinates must be finite");
    }
    public static StoredLocation from(ServerPlayer player) {
        return new StoredLocation(player.level().dimension().location().toString(), player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
    }
}
