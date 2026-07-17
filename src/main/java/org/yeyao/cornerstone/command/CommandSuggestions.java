package org.yeyao.cornerstone.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.level.ServerPlayer;
import org.yeyao.cornerstone.Cornerstone;
import org.yeyao.cornerstone.teleport.Warp;

import java.util.Collection;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;

/** Dynamic Brigadier suggestions shared by Cornerstone commands. */
final class CommandSuggestions {
    private static final List<String> DURATIONS = List.of("30s", "5m", "1h", "1d");
    private static final List<String> GAME_MODES = List.of("survival", "creative", "adventure", "spectator");
    private static final List<String> WARP_ACCESS = List.of("public", "permission", "admin");

    private CommandSuggestions() {
    }

    static CompletableFuture<Suggestions> players(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        Collection<String> names = Cornerstone.services().players().snapshots().stream()
                .map(profile -> profile.lastKnownName())
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(TreeSet::new));
        context.getSource().getServer().getPlayerList().getPlayers().stream()
                .map(player -> player.getGameProfile().getName())
                .forEach(names::add);
        return SharedSuggestionProvider.suggest(names, builder);
    }

    static CompletableFuture<Suggestions> onlinePlayers(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(context.getSource().getServer().getPlayerList().getPlayers().stream()
                .map(player -> player.getGameProfile().getName()), builder);
    }

    static CompletableFuture<Suggestions> homes(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) return builder.buildFuture();
        return SharedSuggestionProvider.suggest(Cornerstone.services().teleports().homeNames(player.getUUID()), builder);
    }

    static CompletableFuture<Suggestions> warps(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        CommandSourceStack source = context.getSource();
        return SharedSuggestionProvider.suggest(Cornerstone.services().teleports().warps().stream()
                .filter(warp -> canUseWarp(source, warp))
                .map(Warp::name), builder);
    }

    static CompletableFuture<Suggestions> durations(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(DURATIONS, builder);
    }

    static CompletableFuture<Suggestions> gameModes(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(GAME_MODES, builder);
    }

    static CompletableFuture<Suggestions> warpAccess(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(WARP_ACCESS, builder);
    }

    private static boolean canUseWarp(CommandSourceStack source, Warp warp) {
        return switch (warp.access()) {
            case PUBLIC -> true;
            case PERMISSION -> Cornerstone.services().permissions().has(source, "cornerstone.warp." + warp.name());
            case ADMIN -> Cornerstone.services().permissions().has(source, "cornerstone.warp.admin." + warp.name());
        };
    }
}
