package org.yeyao.cornerstone.operations;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/** Single cancellable shutdown/restart countdown. A restart relies on the surrounding process supervisor. */
public final class ServerScheduleService {
    private ScheduledOperation scheduled;
    public Optional<ScheduledOperation> scheduled() { return Optional.ofNullable(scheduled); }
    public ScheduledOperation schedule(OperationType type, Duration delay, long tick) {
        if (delay.isNegative() || delay.isZero()) throw new IllegalArgumentException("Delay must be positive");
        scheduled = new ScheduledOperation(UUID.randomUUID(), type, tick + delay.toSeconds() * 20L, -1); return scheduled;
    }
    public boolean cancel() { if (scheduled == null) return false; scheduled = null; return true; }
    public void tick(MinecraftServer server, long tick) {
        if (scheduled == null) return;
        long remainingTicks = Math.max(0, scheduled.executeAtTick() - tick); long remainingSeconds = (remainingTicks + 19) / 20;
        if (shouldAnnounce(remainingSeconds) && scheduled.lastAnnouncedSecond() != remainingSeconds) {
            broadcast(server, (scheduled.type() == OperationType.RESTART ? "Server restart" : "Server shutdown") + " in " + remainingSeconds + " seconds.");
            scheduled = new ScheduledOperation(scheduled.id(), scheduled.type(), scheduled.executeAtTick(), remainingSeconds);
        }
        if (remainingTicks == 0) { broadcast(server, scheduled.type() == OperationType.RESTART ? "Server restarting now." : "Server shutting down now."); scheduled = null; server.halt(false); }
    }
    private static boolean shouldAnnounce(long seconds) { return seconds <= 10 || seconds == 30 || seconds == 60 || seconds % 300 == 0; }
    private static void broadcast(MinecraftServer server, String message) { server.getPlayerList().getPlayers().forEach(player -> player.sendSystemMessage(Component.literal(message))); }
    public enum OperationType { RESTART, SHUTDOWN }
    public record ScheduledOperation(UUID id, OperationType type, long executeAtTick, long lastAnnouncedSecond) { }
}
