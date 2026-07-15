package org.yeyao.cornerstone.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.yeyao.cornerstone.Cornerstone;
import org.yeyao.cornerstone.operations.ServerScheduleService;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/** M6 maintenance, lifecycle scheduling and safe utility commands. */
@EventBusSubscriber(modid = Cornerstone.MODID)
public final class OperationsCommands {
    private static boolean registered;
    private OperationsCommands() { }
    @SubscribeEvent public static void register(RegisterCommandsEvent event) {
        definitions();
        event.getDispatcher().register(Commands.literal("maintenance").requires(source -> allowed(source, "maintenance"))
                .executes(context -> execute(context, "maintenance", List.of("status")))
                .then(Commands.literal("on").executes(context -> execute(context, "maintenance", List.of("on"))))
                .then(Commands.literal("off").executes(context -> execute(context, "maintenance", List.of("off"))))
                .then(Commands.literal("allow").then(Commands.argument("player", StringArgumentType.word()).executes(context -> execute(context, "maintenance", List.of("allow", StringArgumentType.getString(context, "player")))))));
        event.getDispatcher().register(scheduleCommand("restart", ServerScheduleService.OperationType.RESTART));
        event.getDispatcher().register(scheduleCommand("shutdown", ServerScheduleService.OperationType.SHUTDOWN));
        event.getDispatcher().register(Commands.literal("cancelshutdown").requires(source -> allowed(source, "cancelshutdown")).executes(context -> execute(context, "cancelshutdown", List.of())));
        event.getDispatcher().register(onlineTarget("invsee"));
        event.getDispatcher().register(onlineTarget("endersee"));
        event.getDispatcher().register(onlineTarget("clear"));
        event.getDispatcher().register(onlineTarget("heal"));
        event.getDispatcher().register(onlineTarget("feed"));
        event.getDispatcher().register(Commands.literal("gamemode").requires(source -> allowed(source, "gamemode"))
                .then(Commands.argument("mode", StringArgumentType.word()).then(Commands.argument("player", StringArgumentType.word()).executes(context -> execute(context, "gamemode", List.of(StringArgumentType.getString(context, "mode"), StringArgumentType.getString(context, "player")))))));
    }
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> scheduleCommand(String id, ServerScheduleService.OperationType type) {
        return Commands.literal(id).requires(source -> allowed(source, id)).then(Commands.argument("duration", StringArgumentType.word()).executes(context -> execute(context, id, List.of(StringArgumentType.getString(context, "duration")))));
    }
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> onlineTarget(String id) {
        return Commands.literal(id).requires(source -> allowed(source, id)).then(Commands.argument("player", StringArgumentType.word()).executes(context -> execute(context, id, List.of(StringArgumentType.getString(context, "player")))));
    }
    private static synchronized void definitions() {
        if (registered) return;
        register("maintenance", context -> {
            String action = context.arguments().getFirst();
            if (action.equals("on") || action.equals("off")) { Cornerstone.services().maintenance().setEnabled(action.equals("on")); return "Maintenance mode " + action + "."; }
            if (action.equals("allow")) { Target target = target(context.source().getServer(), context.arguments().get(1)); boolean allowed = Cornerstone.services().maintenance().toggleAllowlist(target.id()); return target.name() + (allowed ? " added to" : " removed from") + " the maintenance allowlist."; }
            return "Maintenance mode is " + (Cornerstone.services().maintenance().enabled() ? "enabled" : "disabled") + ".";
        });
        register("restart", context -> schedule(context, ServerScheduleService.OperationType.RESTART));
        register("shutdown", context -> schedule(context, ServerScheduleService.OperationType.SHUTDOWN));
        register("cancelshutdown", context -> Cornerstone.services().schedules().cancel() ? "Scheduled shutdown/restart cancelled." : "No shutdown or restart is scheduled.");
        register("invsee", context -> { ServerPlayer viewer = player(context); ServerPlayer target = online(context.source().getServer(), context.arguments().getFirst()); Cornerstone.services().utilities().openInventory(viewer, target); return "Opened read-only inventory for " + target.getGameProfile().getName() + "."; });
        register("endersee", context -> { ServerPlayer viewer = player(context); ServerPlayer target = online(context.source().getServer(), context.arguments().getFirst()); Cornerstone.services().utilities().openEnderChest(viewer, target); return "Opened read-only ender chest for " + target.getGameProfile().getName() + "."; });
        register("clear", context -> { ServerPlayer target = online(context.source().getServer(), context.arguments().getFirst()); Cornerstone.services().utilities().clearInventory(target); return "Cleared " + target.getGameProfile().getName() + "'s inventory."; });
        register("heal", context -> { ServerPlayer target = online(context.source().getServer(), context.arguments().getFirst()); Cornerstone.services().utilities().heal(target); return "Healed " + target.getGameProfile().getName() + "."; });
        register("feed", context -> { ServerPlayer target = online(context.source().getServer(), context.arguments().getFirst()); Cornerstone.services().utilities().feed(target); return "Fed " + target.getGameProfile().getName() + "."; });
        register("gamemode", context -> { ServerPlayer target = online(context.source().getServer(), context.arguments().get(1)); Cornerstone.services().utilities().setGameMode(target, context.arguments().getFirst()); return "Set " + target.getGameProfile().getName() + " to " + context.arguments().getFirst() + "."; });
        registered = true;
    }
    private static String schedule(CoreCommandContext context, ServerScheduleService.OperationType type) { Duration delay = duration(context.arguments().getFirst()); Cornerstone.services().schedules().schedule(type, delay, context.source().getServer().getTickCount()); return (type == ServerScheduleService.OperationType.RESTART ? "Restart" : "Shutdown") + " scheduled in " + delay.toSeconds() + " seconds."; }
    private static Duration duration(String value) { return org.yeyao.cornerstone.moderation.ModerationService.parseDuration(value).orElseThrow(() -> new IllegalArgumentException("Duration must use a positive number followed by s, m, h, d, or w.")); }
    private static Target target(MinecraftServer server, String input) {
        ServerPlayer online = server.getPlayerList().getPlayerByName(input); if (online != null) return new Target(online.getUUID(), online.getGameProfile().getName());
        try { UUID id = UUID.fromString(input); return Cornerstone.services().players().find(id).map(profile -> new Target(id, profile.lastKnownName())).orElseThrow(() -> new IllegalArgumentException("No player data exists for " + input + ".")); }
        catch (IllegalArgumentException ignored) { }
        return Cornerstone.services().players().snapshots().stream().filter(profile -> profile.lastKnownName().equalsIgnoreCase(input)).findFirst().map(profile -> new Target(profile.id(), profile.lastKnownName())).orElseThrow(() -> new IllegalArgumentException("No player data exists for " + input + "."));
    }
    private static ServerPlayer online(MinecraftServer server, String name) { ServerPlayer player = server.getPlayerList().getPlayerByName(name); if (player == null) throw new IllegalArgumentException("That player is not online."); return player; }
    private static ServerPlayer player(CoreCommandContext context) { if (!(context.source().getEntity() instanceof ServerPlayer player)) throw new IllegalArgumentException("This command can only be used by a player."); return player; }
    private static void register(String id, Function<CoreCommandContext, String> handler) { Cornerstone.services().commands().register(new CommandDefinition("operations." + id, "cornerstone.command." + id, Duration.ZERO, context -> CommandResult.ok(handler.apply(context)))); }
    private static boolean allowed(CommandSourceStack source, String id) { return Cornerstone.services().commands().canExecute(source, "operations." + id); }
    private static int execute(CommandContext<CommandSourceStack> context, String id, List<String> arguments) { CommandResult result = Cornerstone.services().commands().execute("operations." + id, context.getSource(), arguments); if (result.success()) context.getSource().sendSuccess(() -> Component.literal(result.message()), false); else context.getSource().sendFailure(Component.literal(result.message())); return result.success() ? 1 : 0; }
    private record Target(UUID id, String name) { }
}
