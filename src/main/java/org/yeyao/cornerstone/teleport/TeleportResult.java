package org.yeyao.cornerstone.teleport;

/** User-safe result for teleport actions and extension points. */
public record TeleportResult(boolean success, String message) {
    public static TeleportResult ok(String message) { return new TeleportResult(true, message); }
    public static TeleportResult fail(String message) { return new TeleportResult(false, message); }
}
