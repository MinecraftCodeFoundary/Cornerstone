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

import java.time.Duration;
import java.util.List;
import java.util.function.Function;

/** M4 social commands. Private-message commands use raw text only as validated command data. */
@EventBusSubscriber(modid = Cornerstone.MODID)
public final class SocialCommands {
    private static boolean registered;
    private SocialCommands() { }
    @SubscribeEvent public static void register(RegisterCommandsEvent event) {
        definitions();
        event.getDispatcher().register(Commands.literal("msg").requires(source -> allowed(source, "msg"))
                .then(Commands.argument("player", StringArgumentType.word())
                        .then(Commands.argument("message", StringArgumentType.greedyString())
                                .executes(context -> execute(context, "msg", List.of(
                                        StringArgumentType.getString(context, "player"),
                                        StringArgumentType.getString(context, "message")))))));
        event.getDispatcher().register(Commands.literal("reply").requires(source -> allowed(source, "reply"))
                .then(Commands.argument("message", StringArgumentType.greedyString()).executes(context -> execute(context, "reply", List.of(StringArgumentType.getString(context, "message"))))));
        event.getDispatcher().register(Commands.literal("ignore").requires(source -> allowed(source, "ignore"))
                .then(Commands.argument("player", StringArgumentType.word()).executes(context -> execute(context, "ignore", List.of(StringArgumentType.getString(context, "player"))))));
        event.getDispatcher().register(Commands.literal("afk").requires(source -> allowed(source, "afk"))
                .executes(context -> execute(context, "afk", List.of("")))
                .then(Commands.argument("reason", StringArgumentType.greedyString()).executes(context -> execute(context, "afk", List.of(StringArgumentType.getString(context, "reason"))))));
        event.getDispatcher().register(Commands.literal("seen").requires(source -> allowed(source, "seen"))
                .then(Commands.argument("player", StringArgumentType.word()).executes(context -> execute(context, "seen", List.of(StringArgumentType.getString(context, "player"))))));
        event.getDispatcher().register(Commands.literal("list").requires(source -> allowed(source, "list")).executes(context -> execute(context, "list", List.of())));
        event.getDispatcher().register(Commands.literal("rules").requires(source -> allowed(source, "rules")).executes(context -> execute(context, "rules", List.of())));
    }
    private static synchronized void definitions() {
        if (registered) return;
        register("msg", context -> player(context, sender -> {
            if (Cornerstone.services().moderation().isMuted(sender.getUUID())) throw new IllegalArgumentException("You are muted.");
            ServerPlayer target = sender.getServer().getPlayerList().getPlayerByName(context.arguments().getFirst());
            if (target == null) throw new IllegalArgumentException("That player is not online.");
            return Cornerstone.services().social().directMessage(sender, target, context.arguments().get(1));
        }));
        register("reply", context -> player(context, sender -> {
            if (Cornerstone.services().moderation().isMuted(sender.getUUID())) throw new IllegalArgumentException("You are muted.");
            return Cornerstone.services().social().reply(sender, sender.getServer(), context.arguments().getFirst());
        }));
        register("ignore", context -> player(context, sender -> {
            ServerPlayer target = sender.getServer().getPlayerList().getPlayerByName(context.arguments().getFirst());
            if (target == null) throw new IllegalArgumentException("That player is not online.");
            return Cornerstone.services().social().toggleIgnore(sender, target);
        }));
        register("afk", context -> player(context, player -> Cornerstone.services().social().toggleAfk(player, context.arguments().getFirst())));
        register("seen", context -> Cornerstone.services().social().seen(context.source().getServer(), context.arguments().getFirst()));
        register("list", context -> {
            var visible = context.source().getServer().getPlayerList().getPlayers().stream().filter(player -> !Cornerstone.services().moderation().isVanished(player.getUUID())).toList();
            return "Online (" + visible.size() + "): " + String.join(", ", visible.stream().map(player -> player.getGameProfile().getName() + (Cornerstone.services().social().isAfk(player.getUUID()) ? " [AFK]" : "")).toList());
        });
        register("rules", context -> {
            List<String> rules = org.yeyao.cornerstone.Config.rules();
            if (rules.isEmpty()) return "No rules have been configured.";
            rules.forEach(line -> context.source().sendSuccess(() -> Component.literal("[Rules] " + line), false));
            return "Displayed " + rules.size() + " rule(s).";
        });
        registered = true;
    }
    private static void register(String id, Function<CoreCommandContext, String> handler) {
        Cornerstone.services().commands().register(new CommandDefinition("social." + id, "cornerstone.command." + id, Duration.ZERO,
                context -> CommandResult.ok(handler.apply(context))));
    }
    private static boolean allowed(CommandSourceStack source, String id) { return Cornerstone.services().commands().canExecute(source, "social." + id); }
    private static int execute(CommandContext<CommandSourceStack> context, String id, List<String> arguments) {
        CommandResult result = Cornerstone.services().commands().execute("social." + id, context.getSource(), arguments);
        if (result.success()) context.getSource().sendSuccess(() -> Component.literal(result.message()), false); else context.getSource().sendFailure(Component.literal(result.message()));
        return result.success() ? 1 : 0;
    }
    private static String player(CoreCommandContext context, Function<ServerPlayer, String> action) {
        if (!(context.source().getEntity() instanceof ServerPlayer player)) throw new IllegalArgumentException("This command can only be used by a player.");
        return action.apply(player);
    }
}
