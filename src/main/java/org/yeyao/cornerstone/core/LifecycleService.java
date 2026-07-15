package org.yeyao.cornerstone.core;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.yeyao.cornerstone.Config;
import org.yeyao.cornerstone.data.StoredLocation;

import java.nio.file.Path;

/** Opens storage only after a server exists and owns all server lifecycle persistence. */
public final class LifecycleService {
    private final CornerstoneServices services;
    private final Logger logger;
    private boolean running;
    private long lastSaveTick;
    public LifecycleService(CornerstoneServices services, Logger logger) { this.services = services; this.logger = logger; }
    public void start(MinecraftServer server) {
        if (running) return;
        Path directory = server.getWorldPath(LevelResource.ROOT).resolve("cornerstone");
        services.players().open(directory); services.teleports().open(directory); services.moderation().open(directory); services.maintenance().open(directory); services.economy().open(directory); services.audit().open(directory, logger); services.social().start(server);
        running = true; logger.info("Cornerstone core services loaded {} player profiles", services.players().snapshots().size());
    }
    public void playerJoined(ServerPlayer player) { if (running) { services.players().markOnline(player.getUUID(), player.getGameProfile().getName(), StoredLocation.from(player)); services.economy().ensureAccount(player.getUUID()); services.social().joined(player); } }
    public void playerLeft(ServerPlayer player) { if (running) { services.players().updateLocation(player.getUUID(), player.getGameProfile().getName(), StoredLocation.from(player)); services.moderation().playerLeft(player.getUUID()); services.social().left(player); } }
    public void tick(MinecraftServer server) {
        if (running) { services.teleports().tick(server, server.getTickCount()); services.social().tick(server, server.getTickCount()); services.schedules().tick(server, server.getTickCount()); }
        if (running && server.getTickCount() - lastSaveTick >= Config.autoSaveTicks()) { save(); lastSaveTick = server.getTickCount(); }
    }
    public void save() { if (running) { services.players().save(); services.teleports().save(); services.moderation().save(); services.maintenance().save(); services.economy().save(); } }
    public void stop() { if (!running) return; save(); services.moderation().clearTransient(); running = false; logger.info("Cornerstone core services saved"); }
    public boolean isRunning() { return running; }
    public void playerDied(ServerPlayer player) { if (running) services.teleports().recordDeath(player); }
}
