package org.yeyao.cornerstone.data;

import org.yeyao.cornerstone.api.CornerstoneEventBus;
import org.yeyao.cornerstone.api.PlayerDataUpdatedEvent;
import org.yeyao.cornerstone.storage.AtomicFileStorage;

import java.time.Instant;
import java.util.*;
import java.nio.file.Path;

/** Thread-confined to the server thread. It validates module values at the service boundary. */
public final class PlayerDataService {
    private final Map<UUID, PlayerProfile> profiles = new LinkedHashMap<>();
    private final CornerstoneEventBus<PlayerDataUpdatedEvent> updated = new CornerstoneEventBus<>();
    private AtomicFileStorage<Collection<PlayerProfile>> storage;

    public PlayerDataSnapshot getOrCreate(UUID playerId, String name) {
        return profile(playerId, name).snapshot();
    }
    public Optional<PlayerDataSnapshot> find(UUID playerId) {
        PlayerProfile profile = profiles.get(playerId);
        return profile == null ? Optional.empty() : Optional.of(profile.snapshot());
    }
    public void markOnline(UUID playerId, String name, StoredLocation location) {
        PlayerProfile profile = profile(playerId, name);
        profile.lastKnownName = name;
        profile.lastOnline = Instant.now();
        profile.lastLocation = location;
        updated.publish(new PlayerDataUpdatedEvent(playerId, "core", "lastOnline"));
    }
    public void updateLocation(UUID playerId, String name, StoredLocation location) {
        PlayerProfile profile = profile(playerId, name);
        profile.lastKnownName = name;
        profile.lastOnline = Instant.now();
        profile.lastLocation = location;
        updated.publish(new PlayerDataUpdatedEvent(playerId, "core", "lastLocation"));
    }
    public Optional<String> getModuleValue(UUID playerId, String module, String key) {
        validateModule(module); validateKey(key);
        PlayerProfile profile = profiles.get(playerId);
        return profile == null ? Optional.empty() : Optional.ofNullable(profile.modules.getOrDefault(module, Map.of()).get(key));
    }
    public void setModuleValue(UUID playerId, String name, String module, String key, String value) {
        validateModule(module); validateKey(key);
        if (value == null || value.length() > 8_192) throw new IllegalArgumentException("Module values must contain at most 8192 characters");
        profile(playerId, name).modules.computeIfAbsent(module, ignored -> new LinkedHashMap<>()).put(key, value);
        updated.publish(new PlayerDataUpdatedEvent(playerId, module, key));
    }
    public AutoCloseable onUpdated(java.util.function.Consumer<PlayerDataUpdatedEvent> listener) { return updated.subscribe(listener); }
    public Collection<PlayerDataSnapshot> snapshots() { return profiles.values().stream().map(PlayerProfile::snapshot).toList(); }
    void replaceAll(Collection<PlayerProfile> restored) { profiles.clear(); restored.forEach(profile -> profiles.put(profile.id, profile)); }
    Collection<PlayerProfile> internalProfiles() { return profiles.values(); }
    public void open(Path directory) {
        storage = new AtomicFileStorage<>(directory.resolve("players.dat"), 1, PlayerDataStorage.INSTANCE);
        replaceAll(storage.loadOrDefault(List.of()));
    }
    public void save() {
        if (storage != null) storage.save(List.copyOf(profiles.values()));
    }
    private PlayerProfile profile(UUID id, String name) { return profiles.computeIfAbsent(id, ignored -> new PlayerProfile(id, name == null ? "" : name)); }
    private static void validateModule(String value) { if (value == null || !value.matches("[a-z0-9_.-]{1,64}")) throw new IllegalArgumentException("Invalid module id"); }
    private static void validateKey(String value) { if (value == null || !value.matches("[a-zA-Z0-9_.-]{1,128}")) throw new IllegalArgumentException("Invalid data key"); }
}
