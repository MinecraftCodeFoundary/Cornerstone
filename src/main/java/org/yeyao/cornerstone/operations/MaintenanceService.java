package org.yeyao.cornerstone.operations;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.yeyao.cornerstone.permission.PermissionService;
import org.yeyao.cornerstone.storage.AtomicFileStorage;

import java.nio.file.Path;
import java.util.UUID;

/** Persistent maintenance switch with explicit player allowlist and LuckPerms bypass. */
public final class MaintenanceService {
    public static final String BYPASS_PERMISSION = "cornerstone.maintenance.bypass";
    private final PermissionService permissions;
    private OperationsData data = new OperationsData();
    private AtomicFileStorage<OperationsData> storage;
    public MaintenanceService(PermissionService permissions) { this.permissions = permissions; }
    public void open(Path directory) { storage = new AtomicFileStorage<>(directory.resolve("operations.dat"), 1, OperationsDataStorage.INSTANCE); data = storage.loadOrDefault(new OperationsData()); }
    public void save() { if (storage != null) storage.save(data); }
    public boolean enabled() { return data.maintenanceEnabled; }
    public void setEnabled(boolean enabled) { data.maintenanceEnabled = enabled; save(); }
    public boolean toggleAllowlist(UUID player) { boolean allowed = data.maintenanceAllowlist.remove(player) ? false : data.maintenanceAllowlist.add(player); save(); return allowed; }
    public boolean canJoin(ServerPlayer player) { return !data.maintenanceEnabled || data.maintenanceAllowlist.contains(player.getUUID()) || permissions.has(player.getUUID(), BYPASS_PERMISSION); }
    public boolean disconnectIfBlocked(ServerPlayer player) {
        if (canJoin(player)) return false;
        player.connection.disconnect(Component.literal("The server is currently in maintenance mode.")); return true;
    }
}
