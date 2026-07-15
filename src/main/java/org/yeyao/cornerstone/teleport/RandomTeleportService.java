package org.yeyao.cornerstone.teleport;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;
import org.yeyao.cornerstone.Config;
import org.yeyao.cornerstone.data.StoredLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Bounded random-destination search that delegates final movement to TeleportService. */
public final class RandomTeleportService {
    private final TeleportService teleports;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    public RandomTeleportService(TeleportService teleports) { this.teleports = teleports; }
    public TeleportResult randomTeleport(ServerPlayer player, long tick) {
        String dimension = player.level().dimension().location().toString();
        Optional<Config.RtpRule> optionalRule = Config.rtpRule(dimension);
        if (optionalRule.isEmpty()) return TeleportResult.fail("Random teleport is not configured for this dimension.");
        Config.RtpRule rule = optionalRule.get(); Long cooldown = cooldowns.get(player.getUUID());
        if (cooldown != null && cooldown > tick) return TeleportResult.fail("You must wait " + ((cooldown - tick + 19) / 20) + " seconds before random teleporting again.");
        ServerLevel level = player.serverLevel(); BlockPos spawn = level.getSharedSpawnPos();
        for (int attempt = 1; attempt <= rule.attempts(); attempt++) {
            int x = spawn.getX() + ThreadLocalRandom.current().nextInt(-rule.radius(), rule.radius() + 1);
            int z = spawn.getZ() + ThreadLocalRandom.current().nextInt(-rule.radius(), rule.radius() + 1);
            long dx = (long) x - spawn.getX(); long dz = (long) z - spawn.getZ();
            if (dx * dx + dz * dz < (long) rule.minimumDistance() * rule.minimumDistance()) continue;
            BlockPos column = new BlockPos(x, level.getMinBuildHeight(), z);
            if (!level.getWorldBorder().isWithinBounds(column)) continue;
            try { level.getChunkAt(column); } catch (RuntimeException ignored) { continue; }
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos landing = new BlockPos(x, y, z);
            String biome = level.getBiome(landing).unwrapKey().map(key -> key.location().toString()).orElse("");
            if (Config.isRtpBiomeBlocked(biome)) continue;
            StoredLocation destination = new StoredLocation(dimension, x + 0.5D, y, z + 0.5D, player.getYRot(), player.getXRot());
            if (!teleports.validateDestination(player, destination).success()) continue;
            if (teleports.restrictionDenial(new TeleportContext(player, destination, "rtp", 0L)).isPresent()) continue;
            TeleportResult result = teleports.queue(player, destination, "rtp", tick);
            if (result.success()) cooldowns.put(player.getUUID(), tick + rule.cooldownTicks());
            return result;
        }
        return TeleportResult.fail("No safe random destination was found after " + rule.attempts() + " attempts.");
    }
}
