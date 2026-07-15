package org.yeyao.cornerstone.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.yeyao.cornerstone.Cornerstone;
import org.yeyao.cornerstone.Config;
import org.yeyao.cornerstone.data.StoredLocation;
import org.yeyao.cornerstone.teleport.TeleportResult;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/** M2 player commands. Each Brigadier path delegates through CommandFramework for authorization and audit. */
@EventBusSubscriber(modid = Cornerstone.MODID)
public final class TeleportCommands {
    private static boolean registered;
    private TeleportCommands() { }
    @SubscribeEvent public static void register(RegisterCommandsEvent event) {
        if (!Config.moduleEnabled("teleport")) return;
        definitions();
        event.getDispatcher().register(Commands.literal("spawn").requires(source -> allowed(source, "spawn")).executes(context -> execute(context, "spawn", List.of())));
        event.getDispatcher().register(Commands.literal("setspawn").requires(source -> allowed(source, "setspawn"))
                .executes(context -> execute(context, "setspawn", List.of("dimension")))
                .then(Commands.literal("global").executes(context -> execute(context, "setspawn", List.of("global"))))
                .then(Commands.literal("dimension").executes(context -> execute(context, "setspawn", List.of("dimension")))));
        event.getDispatcher().register(Commands.literal("home").requires(source -> allowed(source, "home"))
                .executes(context -> execute(context, "home", List.of("home")))
                .then(Commands.argument("name", StringArgumentType.word()).executes(context -> execute(context, "home", List.of(StringArgumentType.getString(context, "name"))))));
        event.getDispatcher().register(Commands.literal("sethome").requires(source -> allowed(source, "sethome"))
                .executes(context -> execute(context, "sethome", List.of("home")))
                .then(Commands.argument("name", StringArgumentType.word()).executes(context -> execute(context, "sethome", List.of(StringArgumentType.getString(context, "name"))))));
        event.getDispatcher().register(Commands.literal("delhome").requires(source -> allowed(source, "delhome"))
                .then(Commands.argument("name", StringArgumentType.word()).executes(context -> execute(context, "delhome", List.of(StringArgumentType.getString(context, "name"))))));
        event.getDispatcher().register(Commands.literal("tpa").requires(source -> allowed(source, "tpa"))
                .then(Commands.argument("player", StringArgumentType.word()).executes(context -> execute(context, "tpa", List.of(StringArgumentType.getString(context, "player"))))));
        event.getDispatcher().register(Commands.literal("tpaccept").requires(source -> allowed(source, "tpaccept")).executes(context -> execute(context, "tpaccept", List.of())));
        event.getDispatcher().register(Commands.literal("tpdeny").requires(source -> allowed(source, "tpdeny")).executes(context -> execute(context, "tpdeny", List.of())));
        event.getDispatcher().register(Commands.literal("tpacancel").requires(source -> allowed(source, "tpacancel")).executes(context -> execute(context, "tpacancel", List.of())));
        event.getDispatcher().register(Commands.literal("back").requires(source -> allowed(source, "back")).executes(context -> execute(context, "back", List.of())));
    }
    private static synchronized void definitions() {
        if (registered) return;
        register("spawn", context -> player(context, player -> Cornerstone.services().teleports().spawnFor(player.level().dimension().location().toString())
                .map(location -> Cornerstone.services().teleports().queue(player, location, "spawn", tick(player))).orElse(TeleportResult.fail("No spawn is set for this dimension."))));
        register("setspawn", context -> player(context, player -> {
            String mode = context.arguments().getFirst(); if ("global".equals(mode)) Cornerstone.services().teleports().setGlobalSpawn(StoredLocation.from(player));
            else Cornerstone.services().teleports().setDimensionSpawn(StoredLocation.from(player));
            return TeleportResult.ok("Spawn set for " + mode + ".");
        }));
        register("home", context -> player(context, player -> Cornerstone.services().teleports().home(player.getUUID(), context.arguments().getFirst())
                .map(location -> Cornerstone.services().teleports().queue(player, location, "home", tick(player))).orElse(TeleportResult.fail("That home does not exist."))));
        register("sethome", context -> player(context, player -> Cornerstone.services().teleports().setHome(player, context.arguments().getFirst())));
        register("delhome", context -> player(context, player -> Cornerstone.services().teleports().deleteHome(player.getUUID(), context.arguments().getFirst())));
        register("tpa", context -> player(context, player -> {
            ServerPlayer target = player.getServer().getPlayerList().getPlayerByName(context.arguments().getFirst());
            return target == null ? TeleportResult.fail("That player is not online.") : Cornerstone.services().teleports().request(player, target, tick(player));
        }));
        register("tpaccept", context -> player(context, player -> Cornerstone.services().teleports().accept(player, player.getServer(), tick(player))));
        register("tpdeny", context -> player(context, player -> Cornerstone.services().teleports().deny(player, tick(player))));
        register("tpacancel", context -> player(context, player -> Cornerstone.services().teleports().cancelRequest(player)));
        register("back", context -> player(context, player -> Cornerstone.services().teleports().back(player, tick(player))));
        registered = true;
    }
    private static void register(String id, Function<CoreCommandContext, TeleportResult> handler) {
        Cornerstone.services().commands().register(new CommandDefinition("teleport." + id, "cornerstone.command." + id, Duration.ZERO,
                context -> { TeleportResult result = handler.apply(context); return new CommandResult(result.success(), result.message()); }));
    }
    private static boolean allowed(CommandSourceStack source, String id) { return Cornerstone.services().commands().canExecute(source, "teleport." + id); }
    private static int execute(CommandContext<CommandSourceStack> context, String id, List<String> arguments) {
        CommandResult result = Cornerstone.services().commands().execute("teleport." + id, context.getSource(), arguments);
        if (result.success()) context.getSource().sendSuccess(() -> Component.literal(result.message()), false); else context.getSource().sendFailure(Component.literal(result.message()));
        return result.success() ? 1 : 0;
    }
    private static TeleportResult player(CoreCommandContext context, Function<ServerPlayer, TeleportResult> action) {
        if (!(context.source().getEntity() instanceof ServerPlayer player)) return TeleportResult.fail("This command can only be used by a player.");
        return action.apply(player);
    }
    private static long tick(ServerPlayer player) { return player.getServer().getTickCount(); }
}
