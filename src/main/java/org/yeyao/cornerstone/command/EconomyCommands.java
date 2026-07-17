package org.yeyao.cornerstone.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.yeyao.cornerstone.Cornerstone;
import org.yeyao.cornerstone.Config;
import org.yeyao.cornerstone.economy.EconomyTransferResult;
import org.yeyao.cornerstone.economy.TransactionRecord;

import java.nio.file.Path;
import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/** M7 balance, payment and ledger administration commands. */
@EventBusSubscriber(modid = Cornerstone.MODID)
public final class EconomyCommands {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm z").withZone(ZoneId.systemDefault());
    private static boolean registered;
    private EconomyCommands() { }
    @SubscribeEvent public static void register(RegisterCommandsEvent event) {
        if (!Config.moduleEnabled("economy")) return;
        definitions();
        event.getDispatcher().register(Commands.literal("balance").requires(source -> allowed(source, "balance")).executes(context -> execute(context, "balance", List.of())));
        event.getDispatcher().register(Commands.literal("pay").requires(source -> allowed(source, "pay"))
                .then(Commands.argument("player", StringArgumentType.word()).suggests(CommandSuggestions::players)
                        .then(Commands.argument("amount", StringArgumentType.word())
                                .executes(context -> execute(context, "pay", List.of(
                                        StringArgumentType.getString(context, "player"),
                                        StringArgumentType.getString(context, "amount")))))));
        event.getDispatcher().register(Commands.literal("economy")
                .then(Commands.literal("history").requires(source -> allowed(source, "economy.history")).then(Commands.argument("player", StringArgumentType.word()).suggests(CommandSuggestions::players).executes(context -> execute(context, "economy.history", List.of(StringArgumentType.getString(context, "player"))))))
                .then(Commands.literal("export").requires(source -> allowed(source, "economy.export")).executes(context -> execute(context, "economy.export", List.of()))));
    }
    private static synchronized void definitions() {
        if (registered) return;
        register("balance", context -> { ServerPlayer player = player(context); return "Balance: " + Cornerstone.services().economy().balance(player.getUUID()) + "."; });
        register("pay", context -> {
            ServerPlayer sender = player(context); Target target = target(sender.getServer(), context.arguments().getFirst()); long amount = amount(context.arguments().get(1));
            EconomyTransferResult result = Cornerstone.services().economy().transfer(UUID.randomUUID(), sender.getUUID(), target.id(), amount, "player payment");
            if (!result.success()) throw new IllegalArgumentException(result.message());
            ServerPlayer online = sender.getServer().getPlayerList().getPlayer(target.id()); if (online != null) online.sendSystemMessage(Component.literal("You received " + amount + " from " + sender.getGameProfile().getName() + "."));
            return "Paid " + target.name() + " " + amount + ".";
        });
        register("economy.history", context -> {
            Target target = target(context.source().getServer(), context.arguments().getFirst()); List<TransactionRecord> history = Cornerstone.services().economy().history(target.id());
            if (history.isEmpty()) return "No transactions for " + target.name() + ".";
            context.source().sendSuccess(() -> Component.literal("Economy history for " + target.name() + " (" + history.size() + "):"), false);
            history.forEach(record -> context.source().sendSuccess(() -> Component.literal(TIME.format(record.createdAt()) + " | " + record.status() + " | " + record.sourceAccount() + " -> " + record.targetAccount() + " | " + record.amount() + " | " + record.memo()), false));
            return "Displayed " + history.size() + " transaction(s).";
        });
        register("economy.export", context -> { Path directory = context.source().getServer().getWorldPath(LevelResource.ROOT).resolve("cornerstone"); Path output = Cornerstone.services().economy().exportLedger(directory); return "Exported ledger to " + output.getFileName() + "."; });
        registered = true;
    }
    private static long amount(String input) { try { long value = Long.parseLong(input); if (value <= 0) throw new NumberFormatException(); return value; } catch (NumberFormatException exception) { throw new IllegalArgumentException("Amount must be a positive whole number."); } }
    private static Target target(MinecraftServer server, String input) {
        ServerPlayer online = server.getPlayerList().getPlayerByName(input); if (online != null) return new Target(online.getUUID(), online.getGameProfile().getName());
        try { UUID id = UUID.fromString(input); return Cornerstone.services().players().find(id).map(profile -> new Target(id, profile.lastKnownName())).orElseThrow(() -> new IllegalArgumentException("No player data exists for " + input + ".")); }
        catch (IllegalArgumentException ignored) { }
        return Cornerstone.services().players().snapshots().stream().filter(profile -> profile.lastKnownName().equalsIgnoreCase(input)).findFirst().map(profile -> new Target(profile.id(), profile.lastKnownName())).orElseThrow(() -> new IllegalArgumentException("No player data exists for " + input + "."));
    }
    private static ServerPlayer player(CoreCommandContext context) { if (!(context.source().getEntity() instanceof ServerPlayer player)) throw new IllegalArgumentException("This command can only be used by a player."); return player; }
    private static void register(String id, Function<CoreCommandContext, String> handler) { Cornerstone.services().commands().register(new CommandDefinition(id, "cornerstone.command." + id, Duration.ZERO, context -> CommandResult.ok(handler.apply(context)))); }
    private static boolean allowed(CommandSourceStack source, String id) { return Cornerstone.services().commands().canExecute(source, id); }
    private static int execute(CommandContext<CommandSourceStack> context, String id, List<String> arguments) { CommandResult result = Cornerstone.services().commands().execute(id, context.getSource(), arguments); if (result.success()) context.getSource().sendSuccess(() -> Component.literal(result.message()), false); else context.getSource().sendFailure(Component.literal(result.message())); return result.success() ? 1 : 0; }
    private record Target(UUID id, String name) { }
}
