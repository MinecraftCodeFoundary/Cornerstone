package org.yeyao.cornerstone.teleport;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.yeyao.cornerstone.Config;
import org.yeyao.cornerstone.data.StoredLocation;
import org.yeyao.cornerstone.storage.AtomicFileStorage;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/** Owns all M2 teleport state, including delayed execution and destination safety checks. */
public final class TeleportService {
    private TeleportData data = new TeleportData();
    private AtomicFileStorage<TeleportData> storage;
    private final Map<UUID, TeleportRequest> requestsByTarget = new HashMap<>();
    private final Map<UUID, PendingTeleport> pending = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final List<TeleportRestriction> restrictions = new CopyOnWriteArrayList<>();
    private volatile TeleportCostProvider costProvider = TeleportCostProvider.free();

    public void open(Path directory) {
        storage = new AtomicFileStorage<>(directory.resolve("teleports.dat"), 2, TeleportDataStorage.INSTANCE).addMigration(1, value -> value);
        data = storage.loadOrDefault(new TeleportData());
    }
    public void save() { if (storage != null) storage.save(data); }
    public void setGlobalSpawn(StoredLocation location) { data.globalSpawn = location; }
    public void setDimensionSpawn(StoredLocation location) { data.dimensionSpawns.put(location.dimension(), location); }
    public Optional<StoredLocation> spawnFor(String dimension) { return Optional.ofNullable(data.dimensionSpawns.getOrDefault(dimension, data.globalSpawn)); }
    public Optional<StoredLocation> home(UUID player, String name) { return Optional.ofNullable(data.homes.getOrDefault(player, Map.of()).get(normalizeHome(name))); }
    public List<String> homeNames(UUID player) { return data.homes.getOrDefault(player, Map.of()).keySet().stream().sorted().toList(); }
    public TeleportResult setHome(ServerPlayer player, String name) {
        String normalized = normalizeHome(name); Map<String, StoredLocation> homes = data.homes.computeIfAbsent(player.getUUID(), ignored -> new LinkedHashMap<>());
        if (!homes.containsKey(normalized) && homes.size() >= Config.maxHomes()) return TeleportResult.fail("You have reached the home limit of " + Config.maxHomes() + ".");
        homes.put(normalized, StoredLocation.from(player)); return TeleportResult.ok("Home '" + normalized + "' saved.");
    }
    public TeleportResult deleteHome(UUID player, String name) {
        String normalized = normalizeHome(name); Map<String, StoredLocation> homes = data.homes.get(player);
        if (homes == null || homes.remove(normalized) == null) return TeleportResult.fail("That home does not exist.");
        if (homes.isEmpty()) data.homes.remove(player); return TeleportResult.ok("Home '" + normalized + "' deleted.");
    }
    public Optional<Warp> warp(String name) { return Optional.ofNullable(data.warps.get(normalizeWarp(name))); }
    public List<Warp> warps() { return data.warps.values().stream().sorted(Comparator.comparing(Warp::name)).toList(); }
    public TeleportResult setWarp(ServerPlayer player, String name, Warp.Access access) {
        String normalized = normalizeWarp(name); data.warps.put(normalized, new Warp(normalized, StoredLocation.from(player), access));
        return TeleportResult.ok("Warp '" + normalized + "' saved as " + access.name().toLowerCase(Locale.ROOT) + ".");
    }
    public TeleportResult deleteWarp(String name) {
        String normalized = normalizeWarp(name); return data.warps.remove(normalized) == null ? TeleportResult.fail("That warp does not exist.") : TeleportResult.ok("Warp '" + normalized + "' deleted.");
    }
    public TeleportResult request(ServerPlayer requester, ServerPlayer target, long tick) {
        if (requester.getUUID().equals(target.getUUID())) return TeleportResult.fail("You cannot request to teleport to yourself.");
        requestsByTarget.put(target.getUUID(), new TeleportRequest(requester.getUUID(), target.getUUID(), tick + Config.tpaExpirySeconds() * 20L));
        target.sendSystemMessage(net.minecraft.network.chat.Component.literal(requester.getGameProfile().getName() + " requested to teleport to you. Use /tpaccept or /tpdeny."));
        return TeleportResult.ok("Teleport request sent to " + target.getGameProfile().getName() + ".");
    }
    public TeleportResult accept(ServerPlayer target, MinecraftServer server, long tick) {
        TeleportRequest request = requestFor(target.getUUID(), tick); if (request == null) return TeleportResult.fail("You have no pending teleport request.");
        requestsByTarget.remove(target.getUUID()); ServerPlayer requester = server.getPlayerList().getPlayer(request.requester());
        if (requester == null) return TeleportResult.fail("The requesting player is no longer online.");
        TeleportResult result = queue(requester, StoredLocation.from(target), "tpa", tick);
        if (result.success()) target.sendSystemMessage(net.minecraft.network.chat.Component.literal("Accepted " + requester.getGameProfile().getName() + "'s teleport request."));
        return result;
    }
    public TeleportResult deny(ServerPlayer target, long tick) {
        TeleportRequest request = requestFor(target.getUUID(), tick); if (request == null) return TeleportResult.fail("You have no pending teleport request.");
        requestsByTarget.remove(target.getUUID());
        ServerPlayer requester = target.getServer().getPlayerList().getPlayer(request.requester());
        if (requester != null) requester.sendSystemMessage(net.minecraft.network.chat.Component.literal(target.getGameProfile().getName() + " denied your teleport request."));
        return TeleportResult.ok("Teleport request denied.");
    }
    public TeleportResult cancelRequest(ServerPlayer requester) {
        boolean removed = requestsByTarget.values().removeIf(request -> request.requester().equals(requester.getUUID()));
        return removed ? TeleportResult.ok("Teleport request cancelled.") : TeleportResult.fail("You have no outgoing teleport request.");
    }
    public TeleportResult queue(ServerPlayer player, StoredLocation destination, String type, long tick) {
        if (Config.isTeleportDimensionBlocked(destination.dimension())) return TeleportResult.fail("Teleports to that dimension are disabled.");
        Long cooldown = cooldowns.get(player.getUUID()); if (cooldown != null && cooldown > tick) return TeleportResult.fail("You must wait " + ((cooldown - tick + 19) / 20) + " seconds before teleporting again.");
        TeleportContext context = new TeleportContext(player, destination, type, configuredCost(type));
        Optional<String> restrictionFailure = restrictionDenial(context);
        if (restrictionFailure.isPresent()) return TeleportResult.fail(restrictionFailure.get());
        Optional<String> costFailure = costProvider.cannotPay(context); if (costFailure.isPresent()) return TeleportResult.fail(costFailure.get());
        PendingTeleport task = new PendingTeleport(player.getUUID(), destination, StoredLocation.from(player), tick + Config.teleportDelayTicks(), type);
        pending.put(player.getUUID(), task);
        if (Config.teleportDelayTicks() == 0) return perform(player, task, tick);
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Teleporting in " + (Config.teleportDelayTicks() / 20.0) + " seconds. Do not move."));
        return TeleportResult.ok("Teleport queued.");
    }
    public TeleportResult back(ServerPlayer player, long tick) {
        StoredLocation location = data.backLocations.get(player.getUUID());
        return location == null ? TeleportResult.fail("You do not have a location to return to.") : queue(player, location, "back", tick);
    }
    public void recordDeath(ServerPlayer player) { data.backLocations.put(player.getUUID(), StoredLocation.from(player)); }
    public void tick(MinecraftServer server, long tick) {
        requestsByTarget.values().removeIf(request -> request.expiresAtTick() < tick);
        for (PendingTeleport task : List.copyOf(pending.values())) {
            ServerPlayer player = server.getPlayerList().getPlayer(task.player());
            if (player == null) { pending.remove(task.player()); continue; }
            if (Config.cancelOnMove() && moved(player, task.origin())) { pending.remove(task.player()); player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Teleport cancelled because you moved.")); continue; }
            if (task.executeAtTick() <= tick) perform(player, task, tick);
        }
    }
    public AutoCloseable addRestriction(TeleportRestriction restriction) { restrictions.add(Objects.requireNonNull(restriction)); return () -> restrictions.remove(restriction); }
    /** Alias for protection mods that only validate the landing position. */
    public AutoCloseable addLandingValidator(TeleportRestriction validator) { return addRestriction(validator); }
    public AutoCloseable installCostProvider(TeleportCostProvider provider) { TeleportCostProvider previous = costProvider; costProvider = Objects.requireNonNull(provider); return () -> { if (costProvider == provider) costProvider = previous; }; }
    public Optional<String> restrictionDenial(TeleportContext context) {
        for (TeleportRestriction restriction : restrictions) {
            Optional<String> denial = restriction.deny(context); if (denial.isPresent()) return denial;
        }
        return Optional.empty();
    }
    private TeleportResult perform(ServerPlayer player, PendingTeleport task, long tick) {
        pending.remove(player.getUUID());
        TeleportContext context = new TeleportContext(player, task.destination(), task.type(), configuredCost(task.type()));
        Optional<String> restrictionFailure = restrictionDenial(context); if (restrictionFailure.isPresent()) return fail(player, restrictionFailure.get());
        TeleportResult safety = validateDestination(player, task.destination()); if (!safety.success()) return fail(player, safety.message());
        if (!costProvider.withdraw(context)) return fail(player, "You can no longer pay for this teleport.");
        if (Config.recordTeleportOrigin()) data.backLocations.put(player.getUUID(), StoredLocation.from(player));
        ServerLevel level = resolveLevel(player.getServer(), task.destination());
        player.teleportTo(level, task.destination().x(), task.destination().y(), task.destination().z(), task.destination().yaw(), task.destination().pitch());
        cooldowns.put(player.getUUID(), tick + Config.teleportCooldownTicks()); player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Teleported."));
        return TeleportResult.ok("Teleported.");
    }
    public TeleportResult validateDestination(ServerPlayer player, StoredLocation location) {
        MinecraftServer server = player.getServer(); ServerLevel level = server == null ? null : resolveLevel(server, location);
        if (level == null) return TeleportResult.fail("The destination dimension is unavailable.");
        BlockPos feet = BlockPos.containing(location.x(), location.y(), location.z()); BlockPos head = feet.above(); BlockPos below = feet.below();
        try { level.getChunkAt(feet); }
        catch (RuntimeException exception) { return TeleportResult.fail("The destination chunk could not be loaded."); }
        if (!level.getWorldBorder().isWithinBounds(feet)) return TeleportResult.fail("The destination is outside the world border.");
        if (feet.getY() <= level.getMinBuildHeight() || head.getY() >= level.getMaxBuildHeight()) return TeleportResult.fail("The destination is outside the build height.");
        if (!level.getBlockState(feet).getCollisionShape(level, feet).isEmpty() || !level.getBlockState(head).getCollisionShape(level, head).isEmpty()) return TeleportResult.fail("The destination has no free space.");
        BlockState floor = level.getBlockState(below); if (floor.getCollisionShape(level, below).isEmpty()) return TeleportResult.fail("The destination has no safe floor.");
        if (hazardous(level.getBlockState(feet)) || hazardous(level.getBlockState(head)) || hazardous(floor)) return TeleportResult.fail("The destination is hazardous.");
        if (!level.noCollision(player, player.getDimensions(Pose.STANDING).makeBoundingBox(location.x(), location.y(), location.z()))) return TeleportResult.fail("The destination collides with the player.");
        return TeleportResult.ok("Safe.");
    }
    private static boolean hazardous(BlockState state) { return state.is(Blocks.LAVA) || state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE) || state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.CACTUS) || state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.POWDER_SNOW) || state.is(Blocks.WITHER_ROSE) || state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE); }
    private static boolean moved(ServerPlayer player, StoredLocation origin) { return !player.level().dimension().location().toString().equals(origin.dimension()) || player.position().distanceToSqr(origin.x(), origin.y(), origin.z()) > 1.0; }
    private static ServerLevel resolveLevel(MinecraftServer server, StoredLocation location) { return server.getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(location.dimension()))); }
    private TeleportRequest requestFor(UUID target, long tick) { TeleportRequest request = requestsByTarget.get(target); if (request != null && request.expiresAtTick() < tick) { requestsByTarget.remove(target); return null; } return request; }
    private static TeleportResult fail(ServerPlayer player, String message) { player.sendSystemMessage(net.minecraft.network.chat.Component.literal(message)); return TeleportResult.fail(message); }
    private static String normalizeHome(String name) { if (name == null || !name.matches("[A-Za-z0-9_-]{1,32}")) throw new IllegalArgumentException("Home names use 1-32 letters, numbers, '_' or '-'."); return name.toLowerCase(Locale.ROOT); }
    private static String normalizeWarp(String name) { if (name == null || !name.matches("[A-Za-z0-9_-]{1,32}")) throw new IllegalArgumentException("Warp names use 1-32 letters, numbers, '_' or '-'."); return name.toLowerCase(Locale.ROOT); }
    private static long configuredCost(String purpose) { return purpose.startsWith("warp:") ? Config.warpCost(purpose.substring("warp:".length())) : 0L; }
    private record TeleportRequest(UUID requester, UUID target, long expiresAtTick) { }
    private record PendingTeleport(UUID player, StoredLocation destination, StoredLocation origin, long executeAtTick, String type) { }
}
