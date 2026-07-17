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
import org.yeyao.cornerstone.Config;
import org.yeyao.cornerstone.moderation.Punishment;
import org.yeyao.cornerstone.moderation.PunishmentType;

import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/** M5 operator commands. Every mutation enters CommandFramework and its audit log. */
@EventBusSubscriber(modid = Cornerstone.MODID)
public final class ModerationCommands {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm z").withZone(ZoneId.systemDefault());
    private static boolean registered;
    private ModerationCommands() { }
    @SubscribeEvent public static void register(RegisterCommandsEvent event) {
        if (!Config.moduleEnabled("moderation")) return;
        definitions();
        event.getDispatcher().register(withTarget("kick", event, true));
        event.getDispatcher().register(withTarget("ban", event, true));
        event.getDispatcher().register(withTempTarget("tempban", event));
        event.getDispatcher().register(singleTarget("unban", event));
        event.getDispatcher().register(withTarget("mute", event, true));
        event.getDispatcher().register(withTempTarget("tempmute", event));
        event.getDispatcher().register(singleTarget("unmute", event));
        event.getDispatcher().register(withTarget("warn", event, true));
        event.getDispatcher().register(singleTarget("history", event));
        event.getDispatcher().register(Commands.literal("vanish").requires(source -> allowed(source, "vanish")).executes(context -> execute(context, "vanish", List.of())));
        event.getDispatcher().register(Commands.literal("freeze").requires(source -> allowed(source, "freeze"))
                .then(Commands.argument("player", StringArgumentType.word()).suggests(CommandSuggestions::onlinePlayers).executes(context -> execute(context, "freeze", List.of(StringArgumentType.getString(context, "player"))))));
    }
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> withTarget(String id, RegisterCommandsEvent ignored, boolean reason) {
        var root = Commands.literal(id).requires(source -> allowed(source, id));
        var target = Commands.argument("player", StringArgumentType.word()).suggests(CommandSuggestions::players).executes(context -> execute(context, id, List.of(StringArgumentType.getString(context, "player"), "No reason provided.")));
        if (reason) target.then(Commands.argument("reason", StringArgumentType.greedyString()).executes(context -> execute(context, id, List.of(StringArgumentType.getString(context, "player"), StringArgumentType.getString(context, "reason")))));
        return root.then(target);
    }
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> withTempTarget(String id, RegisterCommandsEvent ignored) {
        return Commands.literal(id).requires(source -> allowed(source, id)).then(Commands.argument("player", StringArgumentType.word()).suggests(CommandSuggestions::players)
                .then(Commands.argument("duration", StringArgumentType.word()).suggests(CommandSuggestions::durations).executes(context -> execute(context, id, List.of(StringArgumentType.getString(context, "player"), StringArgumentType.getString(context, "duration"), "No reason provided.")))
                        .then(Commands.argument("reason", StringArgumentType.greedyString()).executes(context -> execute(context, id, List.of(StringArgumentType.getString(context, "player"), StringArgumentType.getString(context, "duration"), StringArgumentType.getString(context, "reason")))))));
    }
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> singleTarget(String id, RegisterCommandsEvent ignored) {
        return Commands.literal(id).requires(source -> allowed(source, id)).then(Commands.argument("player", StringArgumentType.word()).suggests(CommandSuggestions::players).executes(context -> execute(context, id, List.of(StringArgumentType.getString(context, "player")))));
    }
    private static synchronized void definitions() {
        if (registered) return;
        register("kick", context -> { Target target = target(context); ServerPlayer online = online(context.source().getServer(), target.id()); if (online == null) throw new IllegalArgumentException("That player is not online."); online.connection.disconnect(Component.literal(context.arguments().get(1))); return "Kicked " + target.name() + "."; });
        register("ban", context -> punish(context, PunishmentType.BAN, Optional.empty(), true));
        register("tempban", context -> punish(context, PunishmentType.BAN, duration(context.arguments().get(1)), true));
        register("unban", context -> revoke(context, PunishmentType.BAN));
        register("mute", context -> punish(context, PunishmentType.MUTE, Optional.empty(), false));
        register("tempmute", context -> punish(context, PunishmentType.MUTE, duration(context.arguments().get(1)), false));
        register("unmute", context -> revoke(context, PunishmentType.MUTE));
        register("warn", context -> punish(context, PunishmentType.WARN, Optional.empty(), false));
        register("history", ModerationCommands::history);
        register("vanish", context -> player(context, player -> Cornerstone.services().moderation().toggleVanish(player) ? "You are now vanished." : "You are now visible."));
        register("freeze", context -> { Target target = target(context); ServerPlayer online = online(context.source().getServer(), target.id()); if (online == null) throw new IllegalArgumentException("That player is not online."); boolean frozen = Cornerstone.services().moderation().toggleFreeze(online); online.sendSystemMessage(Component.literal(frozen ? "You have been frozen." : "You have been unfrozen.")); return frozen ? "Froze " + target.name() + "." : "Unfroze " + target.name() + "."; });
        registered = true;
    }
    private static String punish(CoreCommandContext context, PunishmentType type, Optional<Duration> duration, boolean disconnect) {
        Target target = target(context); String reason = context.arguments().getLast();
        Punishment record = Cornerstone.services().moderation().issue(type, target.id(), target.name(), context.source(), duration, reason);
        ServerPlayer online = online(context.source().getServer(), target.id());
        if (disconnect && online != null) online.connection.disconnect(Component.literal("You are banned: " + record.reason()));
        if (!disconnect && online != null) online.sendSystemMessage(Component.literal("You received a " + type.name().toLowerCase() + ": " + record.reason()));
        return type.name().substring(0, 1) + type.name().substring(1).toLowerCase() + " applied to " + target.name() + record.expiresAt().map(expiry -> " until " + TIME.format(expiry)).orElse("") + ".";
    }
    private static String revoke(CoreCommandContext context, PunishmentType type) { Target target = target(context); return Cornerstone.services().moderation().revoke(type, target.id()) ? type.name().toLowerCase() + " removed from " + target.name() + "." : "That player has no active " + type.name().toLowerCase() + "."; }
    private static String history(CoreCommandContext context) {
        Target target = target(context); List<Punishment> history = Cornerstone.services().moderation().history(target.id());
        if (history.isEmpty()) return "No moderation history for " + target.name() + ".";
        context.source().sendSuccess(() -> Component.literal("History for " + target.name() + " (" + history.size() + "):"), false);
        history.forEach(record -> context.source().sendSuccess(() -> Component.literal(record.type() + " | " + TIME.format(record.issuedAt()) + " | by " + record.actorName() + " | " + record.reason() + (record.revoked() ? " | revoked" : record.expiresAt().map(expiry -> " | expires " + TIME.format(expiry)).orElse(""))), false));
        return "Displayed " + history.size() + " record(s).";
    }
    private static Optional<Duration> duration(String value) { return org.yeyao.cornerstone.moderation.ModerationService.parseDuration(value).or(() -> { throw new IllegalArgumentException("Duration must use a positive number followed by s, m, h, d, or w."); }); }
    private static Target target(CoreCommandContext context) {
        String input = context.arguments().getFirst(); MinecraftServer server = context.source().getServer(); ServerPlayer online = server.getPlayerList().getPlayerByName(input);
        if (online != null) return new Target(online.getUUID(), online.getGameProfile().getName());
        try { UUID id = UUID.fromString(input); return Cornerstone.services().players().find(id).map(profile -> new Target(id, profile.lastKnownName())).orElseThrow(() -> new IllegalArgumentException("No player data exists for " + input + ".")); }
        catch (IllegalArgumentException ignored) { }
        return Cornerstone.services().players().snapshots().stream().filter(profile -> profile.lastKnownName().equalsIgnoreCase(input)).findFirst().map(profile -> new Target(profile.id(), profile.lastKnownName())).orElseThrow(() -> new IllegalArgumentException("No player data exists for " + input + "."));
    }
    private static ServerPlayer online(MinecraftServer server, UUID id) { return server.getPlayerList().getPlayer(id); }
    private static void register(String id, Function<CoreCommandContext, String> handler) { Cornerstone.services().commands().register(new CommandDefinition("moderation." + id, "cornerstone.command." + id, Duration.ZERO, context -> CommandResult.ok(handler.apply(context)))); }
    private static boolean allowed(CommandSourceStack source, String id) { return Cornerstone.services().commands().canExecute(source, "moderation." + id); }
    private static int execute(CommandContext<CommandSourceStack> context, String id, List<String> arguments) { CommandResult result = Cornerstone.services().commands().execute("moderation." + id, context.getSource(), arguments); if (result.success()) context.getSource().sendSuccess(() -> Component.literal(result.message()), false); else context.getSource().sendFailure(Component.literal(result.message())); return result.success() ? 1 : 0; }
    private static String player(CoreCommandContext context, Function<ServerPlayer, String> action) { if (!(context.source().getEntity() instanceof ServerPlayer player)) throw new IllegalArgumentException("This command can only be used by a player."); return action.apply(player); }
    private record Target(UUID id, String name) { }
}
