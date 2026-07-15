package org.yeyao.cornerstone.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.yeyao.cornerstone.Config;

import java.util.function.Function;

/** Registers configured aliases from the same Brigadier builder so aliases cannot bypass command policy. */
public final class CommandAliasRegistrar {
    private CommandAliasRegistrar() { }
    public static void register(RegisterCommandsEvent event, String canonicalName, Function<String, LiteralArgumentBuilder<CommandSourceStack>> builder) {
        event.getDispatcher().register(builder.apply(canonicalName));
        for (String alias : Config.commandAliases(canonicalName)) event.getDispatcher().register(builder.apply(alias));
    }
}
