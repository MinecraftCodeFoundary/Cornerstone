package org.yeyao.cornerstone.command;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Function;

/** Framework description used by every future user-facing command. */
public record CommandDefinition(String id, String permission, Duration cooldown,
                                Function<CoreCommandContext, CommandResult> handler) {
    public CommandDefinition {
        if (id == null || !id.matches("[a-z0-9_.-]{1,64}")) throw new IllegalArgumentException("Invalid command id");
        if (permission == null || !permission.matches("[a-z0-9_.-]{1,128}")) throw new IllegalArgumentException("Invalid permission node");
        cooldown = cooldown == null ? Duration.ZERO : cooldown;
        if (cooldown.isNegative()) throw new IllegalArgumentException("Cooldown cannot be negative");
        Objects.requireNonNull(handler, "handler");
    }
}
