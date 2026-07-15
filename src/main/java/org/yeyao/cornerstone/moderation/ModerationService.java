package org.yeyao.cornerstone.moderation;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.yeyao.cornerstone.data.StoredLocation;
import org.yeyao.cornerstone.storage.AtomicFileStorage;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/** Persistent bans/mutes/warnings plus transient freeze and vanish states. */
public final class ModerationService {
    private ModerationData data = new ModerationData();
    private AtomicFileStorage<ModerationData> storage;
    private final Map<UUID, StoredLocation> frozen = new HashMap<>();
    private final Set<UUID> vanished = new HashSet<>();
    public void open(Path directory) { storage = new AtomicFileStorage<>(directory.resolve("moderation.dat"), 1, ModerationDataStorage.INSTANCE); data = storage.loadOrDefault(new ModerationData()); }
    public void save() { if (storage != null) storage.save(data); }
    public Punishment issue(PunishmentType type, UUID targetId, String targetName, CommandSourceStack actor, Optional<Duration> duration, String reason) {
        Instant now = Instant.now(); Optional<Instant> expiry = duration.map(value -> now.plus(value));
        Punishment record = new Punishment(UUID.randomUUID(), type, targetId, targetName, Optional.ofNullable(actor.getEntity()).map(entity -> entity.getUUID()), actor.getTextName(), now, expiry, safeReason(reason), false);
        data.punishments.add(record); save(); return record;
    }
    public boolean revoke(PunishmentType type, UUID targetId) {
        Instant now = Instant.now();
        for (int i = data.punishments.size() - 1; i >= 0; i--) {
            Punishment current = data.punishments.get(i);
            if (current.type() == type && current.targetId().equals(targetId) && current.activeAt(now)) {
                data.punishments.set(i, new Punishment(current.id(), current.type(), current.targetId(), current.targetName(), current.actorId(), current.actorName(), current.issuedAt(), current.expiresAt(), current.reason(), true));
                save(); return true;
            }
        }
        return false;
    }
    public Optional<Punishment> active(PunishmentType type, UUID playerId) {
        Instant now = Instant.now();
        return data.punishments.stream().filter(record -> record.type() == type && record.targetId().equals(playerId) && record.activeAt(now)).reduce((first, second) -> second);
    }
    public boolean isMuted(UUID playerId) { return active(PunishmentType.MUTE, playerId).isPresent(); }
    public boolean isBanned(UUID playerId) { return active(PunishmentType.BAN, playerId).isPresent(); }
    public List<Punishment> history(UUID playerId) { return data.punishments.stream().filter(record -> record.targetId().equals(playerId)).toList(); }
    public boolean toggleFreeze(ServerPlayer player) {
        if (frozen.remove(player.getUUID()) != null) return false;
        frozen.put(player.getUUID(), StoredLocation.from(player)); return true;
    }
    public void enforceFreeze(ServerPlayer player) {
        StoredLocation location = frozen.get(player.getUUID()); if (location == null) return;
        player.setDeltaMovement(0, 0, 0);
        if (player.position().distanceToSqr(location.x(), location.y(), location.z()) > 0.01D) player.teleportTo(player.serverLevel(), location.x(), location.y(), location.z(), location.yaw(), location.pitch());
    }
    public boolean toggleVanish(ServerPlayer player) { boolean visible = vanished.remove(player.getUUID()); player.setInvisible(!visible); return !visible; }
    public boolean isVanished(UUID playerId) { return vanished.contains(playerId); }
    public void playerLeft(UUID playerId) { frozen.remove(playerId); vanished.remove(playerId); }
    public void clearTransient() { frozen.clear(); vanished.clear(); }
    public boolean disconnectIfBanned(ServerPlayer player) {
        Optional<Punishment> punishment = active(PunishmentType.BAN, player.getUUID());
        punishment.ifPresent(record -> player.connection.disconnect(Component.literal("You are banned: " + record.reason() + record.expiresAt().map(value -> " (until " + value + ")").orElse(""))));
        return punishment.isPresent();
    }
    public static Optional<Duration> parseDuration(String value) {
        if (value == null || !value.matches("[1-9][0-9]{0,8}[smhdw]")) return Optional.empty();
        long amount = Long.parseLong(value.substring(0, value.length() - 1));
        return Optional.of(switch (value.charAt(value.length() - 1)) { case 's' -> Duration.ofSeconds(amount); case 'm' -> Duration.ofMinutes(amount); case 'h' -> Duration.ofHours(amount); case 'd' -> Duration.ofDays(amount); case 'w' -> Duration.ofDays(Math.multiplyExact(amount, 7)); default -> throw new IllegalArgumentException("Invalid duration"); });
    }
    private static String safeReason(String reason) { String value = reason == null || reason.isBlank() ? "No reason provided." : reason.trim(); return value.substring(0, Math.min(value.length(), 512)); }
}
