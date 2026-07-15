package org.yeyao.cornerstone.command;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.yeyao.cornerstone.Cornerstone;
import org.yeyao.cornerstone.Config;

import java.time.Duration;
import java.util.List;

/** The first concrete consumer of the command framework and a diagnostic admin surface. */
@EventBusSubscriber(modid = Cornerstone.MODID)
public final class CornerstoneCommands {
    private static boolean registered;
    private CornerstoneCommands() { }
    @SubscribeEvent public static void register(RegisterCommandsEvent event) {
        if (!Config.moduleEnabled("core")) return;
        registerDefinitions();
        event.getDispatcher().register(Commands.literal("cornerstone")
                .then(Commands.literal("status").requires(source -> Cornerstone.services().commands().canExecute(source, "cornerstone.status")).executes(context -> execute(context, "cornerstone.status")))
                .then(Commands.literal("save").requires(source -> Cornerstone.services().commands().canExecute(source, "cornerstone.save")).executes(context -> execute(context, "cornerstone.save"))));
    }
    private static synchronized void registerDefinitions() {
        if (registered) return;
        var commands = Cornerstone.services().commands();
        commands.register(new CommandDefinition("cornerstone.status", "cornerstone.command.status", Duration.ZERO,
                context -> CommandResult.ok("Cornerstone core services are " + (Cornerstone.services().lifecycle().isRunning() ? "running" : "starting") + ".")));
        commands.register(new CommandDefinition("cornerstone.save", "cornerstone.command.save", Duration.ZERO,
                context -> { Cornerstone.services().lifecycle().save(); return CommandResult.ok("Cornerstone data saved."); }));
        registered = true;
    }
    private static int execute(CommandContext<CommandSourceStack> context, String id) {
        CommandResult result = Cornerstone.services().commands().execute(id, context.getSource(), List.of());
        if (result.success()) context.getSource().sendSuccess(() -> Component.literal(result.message()), false);
        else context.getSource().sendFailure(Component.literal(result.message()));
        return result.success() ? 1 : 0;
    }
}
