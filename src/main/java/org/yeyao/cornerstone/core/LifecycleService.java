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
        if (Config.moduleEnabled("core")) { services.players().open(directory); services.audit().open(directory, logger); }
        if (Config.moduleEnabled("teleport")) services.teleports().open(directory);
        if (Config.moduleEnabled("moderation")) services.moderation().open(directory);
        if (Config.moduleEnabled("operations")) services.maintenance().open(directory);
        if (Config.moduleEnabled("economy")) services.economy().open(directory);
        if (Config.moduleEnabled("social")) services.social().start(server);
        running = true; logger.info("Cornerstone core services loaded {} player profiles", services.players().snapshots().size());
    }
    public void playerJoined(ServerPlayer player) { if (running) { if (Config.moduleEnabled("core")) services.players().markOnline(player.getUUID(), player.getGameProfile().getName(), StoredLocation.from(player)); if (Config.moduleEnabled("economy")) services.economy().ensureAccount(player.getUUID()); if (Config.moduleEnabled("social")) services.social().joined(player); } }
    public void playerLeft(ServerPlayer player) { if (running) { if (Config.moduleEnabled("core")) services.players().updateLocation(player.getUUID(), player.getGameProfile().getName(), StoredLocation.from(player)); if (Config.moduleEnabled("moderation")) services.moderation().playerLeft(player.getUUID()); if (Config.moduleEnabled("social")) services.social().left(player); } }
    public void tick(MinecraftServer server) {
        if (running) { if (Config.moduleEnabled("teleport")) services.teleports().tick(server, server.getTickCount()); if (Config.moduleEnabled("social")) services.social().tick(server, server.getTickCount()); if (Config.moduleEnabled("operations")) services.schedules().tick(server, server.getTickCount()); }
        if (running && server.getTickCount() - lastSaveTick >= Config.autoSaveTicks()) { save(); lastSaveTick = server.getTickCount(); }
    }
    public void save() { if (running) { if (Config.moduleEnabled("core")) services.players().save(); if (Config.moduleEnabled("teleport")) services.teleports().save(); if (Config.moduleEnabled("moderation")) services.moderation().save(); if (Config.moduleEnabled("operations")) services.maintenance().save(); if (Config.moduleEnabled("economy")) services.economy().save(); } }
    public void stop() { if (!running) return; save(); if (Config.moduleEnabled("moderation")) services.moderation().clearTransient(); running = false; logger.info("Cornerstone core services saved"); }
    public boolean isRunning() { return running; }
    public void playerDied(ServerPlayer player) { if (running && Config.moduleEnabled("teleport")) services.teleports().recordDeath(player); }
}
