package org.yeyao.cornerstone.social;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Extension point for chat presentation. The raw text has already passed the word filter. */
@FunctionalInterface
public interface ChatFormatter {
    Component format(ServerPlayer player, String filteredText);
    static ChatFormatter defaultFormatter() { return (player, text) -> Component.literal("<" + player.getGameProfile().getName() + "> ").append(Component.literal(text)); }
}
