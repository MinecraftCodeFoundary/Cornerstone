package org.yeyao.cornerstone.command;

import net.minecraft.commands.CommandSourceStack;
import java.util.List;

public record CoreCommandContext(CommandSourceStack source, List<String> arguments) {
}
