package org.yeyao.cornerstone.command;

import net.minecraft.commands.CommandSourceStack;
import org.yeyao.cornerstone.audit.AuditRecord;
import org.yeyao.cornerstone.audit.AuditService;
import org.yeyao.cornerstone.permission.PermissionService;

import java.time.Instant;
import java.util.*;

/** Applies authorization, validation failures, cooldowns and auditing consistently. */
public final class CommandFramework {
    private final PermissionService permissions;
    private final AuditService audit;
    private final Map<String, CommandDefinition> definitions = new HashMap<>();
    private final Map<UUID, Map<String, Instant>> cooldowns = new HashMap<>();
    public CommandFramework(PermissionService permissions, AuditService audit) { this.permissions = permissions; this.audit = audit; }
    public void register(CommandDefinition definition) {
        if (definitions.putIfAbsent(definition.id(), definition) != null) throw new IllegalStateException("Command already registered: " + definition.id());
    }
    public boolean canExecute(CommandSourceStack source, String id) {
        CommandDefinition definition = definitions.get(id);
        return definition != null && permissions.has(source, definition.permission());
    }
    public CommandResult execute(String id, CommandSourceStack source, List<String> arguments) {
        CommandDefinition definition = definitions.get(id);
        if (definition == null) return CommandResult.fail("Unknown command.");
        if (!permissions.has(source, definition.permission())) return audited(source, id, arguments, CommandResult.fail("You do not have permission to use this command."));
        UUID player = source.getEntity() == null ? null : source.getEntity().getUUID();
        if (player != null && coolingDown(player, id, definition)) return audited(source, id, arguments, CommandResult.fail("This command is cooling down."));
        CommandResult result;
        try { result = definition.handler().apply(new CoreCommandContext(source, List.copyOf(arguments))); }
        catch (IllegalArgumentException exception) { result = CommandResult.fail("Invalid command arguments: " + exception.getMessage()); }
        catch (Exception exception) { result = CommandResult.fail("Command failed safely. Check the server log."); }
        if (player != null && result.success() && !definition.cooldown().isZero()) cooldowns.computeIfAbsent(player, ignored -> new HashMap<>()).put(id, Instant.now().plus(definition.cooldown()));
        return audited(source, id, arguments, result);
    }
    private boolean coolingDown(UUID player, String id, CommandDefinition definition) {
        Instant expires = cooldowns.getOrDefault(player, Map.of()).get(id);
        return expires != null && expires.isAfter(Instant.now());
    }
    private CommandResult audited(CommandSourceStack source, String id, List<String> arguments, CommandResult result) {
        String actor = source.getTextName(); UUID actorId = source.getEntity() == null ? null : source.getEntity().getUUID();
        audit.record(new AuditRecord(Instant.now(), "command." + id, actor, Optional.ofNullable(actorId), auditTarget(id, arguments), source.getLevel().dimension().location().toString(), auditArguments(id, arguments), result.success(), result.message()));
        return result;
    }
    private static String auditArguments(String id, List<String> arguments) {
        if (id.equals("social.msg")) return arguments.isEmpty() ? "<redacted>" : arguments.getFirst() + " <redacted>";
        if (id.equals("social.reply")) return "<redacted>";
        return String.join(" ", arguments);
    }
    private static String auditTarget(String id, List<String> arguments) {
        if ((id.startsWith("moderation.") || id.equals("social.msg")) && !arguments.isEmpty()) return arguments.getFirst();
        if (id.startsWith("operations.")) {
            if (id.equals("operations.gamemode") && arguments.size() > 1) return arguments.get(1);
            if (id.equals("operations.maintenance") && arguments.size() > 1 && arguments.getFirst().equals("allow")) return arguments.get(1);
            if (!arguments.isEmpty() && !id.equals("operations.maintenance") && !id.equals("operations.restart") && !id.equals("operations.shutdown")) return arguments.getFirst();
        }
        if ((id.equals("pay") || id.equals("economy.history")) && !arguments.isEmpty()) return arguments.getFirst();
        return "";
    }
}
