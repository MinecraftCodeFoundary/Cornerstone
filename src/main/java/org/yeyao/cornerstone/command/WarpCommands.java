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
import org.yeyao.cornerstone.teleport.TeleportResult;
import org.yeyao.cornerstone.teleport.Warp;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/** M3 warp and random-teleport commands, all delegated through the shared command framework. */
@EventBusSubscriber(modid = Cornerstone.MODID)
public final class WarpCommands {
    private static boolean registered;
    private WarpCommands() { }
    @SubscribeEvent public static void register(RegisterCommandsEvent event) {
        if (!Config.moduleEnabled("warps")) return;
        definitions();
        event.getDispatcher().register(Commands.literal("warp").requires(source -> allowed(source, "warp"))
                .then(Commands.argument("name", StringArgumentType.word()).suggests(CommandSuggestions::warps).executes(context -> execute(context, "warp", List.of(StringArgumentType.getString(context, "name"))))));
        event.getDispatcher().register(Commands.literal("setwarp").requires(source -> allowed(source, "setwarp"))
                .then(Commands.argument("name", StringArgumentType.word()).executes(context -> execute(context, "setwarp", List.of(StringArgumentType.getString(context, "name"), "public")))
                        .then(Commands.argument("access", StringArgumentType.word()).suggests(CommandSuggestions::warpAccess).executes(context -> execute(context, "setwarp", List.of(StringArgumentType.getString(context, "name"), StringArgumentType.getString(context, "access")))))));
        event.getDispatcher().register(Commands.literal("delwarp").requires(source -> allowed(source, "delwarp"))
                .then(Commands.argument("name", StringArgumentType.word()).suggests(CommandSuggestions::warps).executes(context -> execute(context, "delwarp", List.of(StringArgumentType.getString(context, "name"))))));
        event.getDispatcher().register(Commands.literal("rtp").requires(source -> allowed(source, "rtp")).executes(context -> execute(context, "rtp", List.of())));
    }
    private static synchronized void definitions() {
        if (registered) return;
        register("warp", context -> player(context, player -> Cornerstone.services().teleports().warp(context.arguments().getFirst()).map(warp -> useWarp(context.source(), player, warp)).orElse(TeleportResult.fail("That warp does not exist."))));
        register("setwarp", context -> player(context, player -> {
            try { return Cornerstone.services().teleports().setWarp(player, context.arguments().getFirst(), Warp.Access.valueOf(context.arguments().get(1).toUpperCase(Locale.ROOT))); }
            catch (IllegalArgumentException exception) { return TeleportResult.fail("Warp access must be public, permission, or admin."); }
        }));
        register("delwarp", context -> Cornerstone.services().teleports().deleteWarp(context.arguments().getFirst()));
        register("rtp", context -> player(context, player -> Cornerstone.services().randomTeleports().randomTeleport(player, player.getServer().getTickCount())));
        registered = true;
    }
    private static TeleportResult useWarp(CommandSourceStack source, ServerPlayer player, Warp warp) {
        String node = switch (warp.access()) {
            case PUBLIC -> null;
            case PERMISSION -> "cornerstone.warp." + warp.name();
            case ADMIN -> "cornerstone.warp.admin." + warp.name();
        };
        if (node != null && !Cornerstone.services().permissions().has(source, node)) return TeleportResult.fail("You do not have permission to use this warp.");
        return Cornerstone.services().teleports().queue(player, warp.location(), "warp:" + warp.name(), player.getServer().getTickCount());
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
}
