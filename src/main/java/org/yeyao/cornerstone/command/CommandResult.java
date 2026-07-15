package org.yeyao.cornerstone.command;

/** Normalized command outcome, safe to show to a user. */
public record CommandResult(boolean success, String message) {
    public static CommandResult ok(String message) { return new CommandResult(true, message); }
    public static CommandResult fail(String message) { return new CommandResult(false, message); }
}
