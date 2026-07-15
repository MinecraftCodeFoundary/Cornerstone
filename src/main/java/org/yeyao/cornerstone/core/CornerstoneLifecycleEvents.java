package org.yeyao.cornerstone.core;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.yeyao.cornerstone.Cornerstone;

/** Wires the services into NeoForge without leaking game events into feature modules. */
@EventBusSubscriber(modid = Cornerstone.MODID)
public final class CornerstoneLifecycleEvents {
    private CornerstoneLifecycleEvents() { }
    @SubscribeEvent public static void onStarted(ServerStartedEvent event) { Cornerstone.services().lifecycle().start(event.getServer()); }
    @SubscribeEvent public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) Cornerstone.services().lifecycle().playerJoined(player);
    }
    @SubscribeEvent public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) Cornerstone.services().lifecycle().playerLeft(player);
    }
    @SubscribeEvent public static void onServerTick(ServerTickEvent.Post event) { Cornerstone.services().lifecycle().tick(event.getServer()); }
    @SubscribeEvent public static void onStopping(ServerStoppingEvent event) { Cornerstone.services().lifecycle().stop(); }
}
