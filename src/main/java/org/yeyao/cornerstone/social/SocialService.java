package org.yeyao.cornerstone.social;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.yeyao.cornerstone.Config;
import org.yeyao.cornerstone.data.PlayerDataService;
import org.yeyao.cornerstone.data.PlayerDataSnapshot;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** M4 social state. Persistent ignore relationships use the M1 player-data module store. */
public final class SocialService {
    private static final String MODULE = "social";
    private static final DateTimeFormatter SEEN_TIME = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm z").withZone(ZoneId.systemDefault());
    private final PlayerDataService players;
    private final Map<UUID, UUID> lastContacts = new HashMap<>();
    private final Map<UUID, String> afkPlayers = new HashMap<>();
    private final Map<UUID, Vec3> lastPositions = new HashMap<>();
    private volatile ChatFormatter chatFormatter = ChatFormatter.defaultFormatter();
    private long nextAnnouncementTick;
    private int announcementIndex;

    public SocialService(PlayerDataService players) { this.players = players; }
    public void start(MinecraftServer server) { server.setMotd(Config.motd()); nextAnnouncementTick = server.getTickCount() + Config.announcementIntervalTicks(); }
    public void tick(MinecraftServer server, long tick) {
        server.setMotd(Config.motd());
        if (!Config.announcements().isEmpty() && tick >= nextAnnouncementTick) {
            broadcast(server, "[Announcement] " + Config.announcements().get(announcementIndex++ % Config.announcements().size()));
            nextAnnouncementTick = tick + Config.announcementIntervalTicks();
        }
    }
    public void joined(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (Config.joinLeaveMessages()) broadcast(server, "+ " + player.getGameProfile().getName() + " joined the server.");
        for (String line : Config.welcomeMessages()) player.sendSystemMessage(Component.literal(line.replace("{player}", player.getGameProfile().getName())));
        lastPositions.put(player.getUUID(), player.position());
    }
    public void left(ServerPlayer player) {
        if (Config.joinLeaveMessages()) broadcast(player.getServer(), "- " + player.getGameProfile().getName() + " left the server.");
        afkPlayers.remove(player.getUUID()); lastPositions.remove(player.getUUID()); lastContacts.remove(player.getUUID());
        lastContacts.values().removeIf(id -> id.equals(player.getUUID()));
    }
    public void playerTick(ServerPlayer player) {
        Vec3 previous = lastPositions.put(player.getUUID(), player.position());
        if (previous != null && previous.distanceToSqr(player.position()) > 0.01D) clearAfk(player, true);
    }
    public void activity(ServerPlayer player) { clearAfk(player, true); }
    public Component formatChat(ServerPlayer player, String rawText) { activity(player); return chatFormatter.format(player, filter(rawText)); }
    public AutoCloseable installChatFormatter(ChatFormatter formatter) {
        ChatFormatter previous = chatFormatter; chatFormatter = Objects.requireNonNull(formatter);
        return () -> { if (chatFormatter == formatter) chatFormatter = previous; };
    }
    public String directMessage(ServerPlayer sender, ServerPlayer target, String message) {
        if (sender.getUUID().equals(target.getUUID())) throw new IllegalArgumentException("You cannot message yourself.");
        if (isIgnoring(target.getUUID(), sender.getUUID())) throw new IllegalArgumentException("That player is ignoring you.");
        String clean = filter(message.trim()); if (clean.isBlank()) throw new IllegalArgumentException("Message cannot be empty.");
        sender.sendSystemMessage(Component.literal("[To " + target.getGameProfile().getName() + "] " + clean));
        target.sendSystemMessage(Component.literal("[From " + sender.getGameProfile().getName() + "] " + clean));
        lastContacts.put(sender.getUUID(), target.getUUID()); lastContacts.put(target.getUUID(), sender.getUUID());
        return "Message sent to " + target.getGameProfile().getName() + ".";
    }
    public String reply(ServerPlayer sender, MinecraftServer server, String message) {
        UUID targetId = lastContacts.get(sender.getUUID()); if (targetId == null) throw new IllegalArgumentException("You have nobody to reply to.");
        ServerPlayer target = server.getPlayerList().getPlayer(targetId); if (target == null) throw new IllegalArgumentException("That player is no longer online.");
        return directMessage(sender, target, message);
    }
    public String toggleIgnore(ServerPlayer player, ServerPlayer target) {
        if (player.getUUID().equals(target.getUUID())) throw new IllegalArgumentException("You cannot ignore yourself.");
        boolean ignored = isIgnoring(player.getUUID(), target.getUUID());
        players.setModuleValue(player.getUUID(), player.getGameProfile().getName(), MODULE, ignoreKey(target.getUUID()), Boolean.toString(!ignored));
        return ignored ? "You are no longer ignoring " + target.getGameProfile().getName() + "." : "You are now ignoring " + target.getGameProfile().getName() + ".";
    }
    public String toggleAfk(ServerPlayer player, String reason) {
        if (afkPlayers.remove(player.getUUID()) != null) { broadcast(player.getServer(), player.getGameProfile().getName() + " is no longer AFK."); return "AFK disabled."; }
        String value = reason == null || reason.isBlank() ? "" : reason.trim().substring(0, Math.min(128, reason.trim().length()));
        afkPlayers.put(player.getUUID(), value); broadcast(player.getServer(), player.getGameProfile().getName() + " is now AFK" + (value.isEmpty() ? "." : ": " + value)); return "AFK enabled.";
    }
    public String seen(MinecraftServer server, String name) {
        ServerPlayer online = server.getPlayerList().getPlayerByName(name); if (online != null) return online.getGameProfile().getName() + " is online" + (afkPlayers.containsKey(online.getUUID()) ? " (AFK)." : ".");
        return players.snapshots().stream().filter(profile -> profile.lastKnownName().equalsIgnoreCase(name)).findFirst()
                .map(profile -> profile.lastKnownName() + " was last seen " + SEEN_TIME.format(profile.lastOnline()) + ".").orElse("No player data exists for " + name + ".");
    }
    public List<String> onlineList(MinecraftServer server) { return server.getPlayerList().getPlayers().stream().map(player -> player.getGameProfile().getName() + (afkPlayers.containsKey(player.getUUID()) ? " [AFK]" : "")).toList(); }
    public boolean isAfk(UUID player) { return afkPlayers.containsKey(player); }
    private boolean isIgnoring(UUID player, UUID other) { return players.getModuleValue(player, MODULE, ignoreKey(other)).map(Boolean::parseBoolean).orElse(false); }
    private void clearAfk(ServerPlayer player, boolean announce) { if (afkPlayers.remove(player.getUUID()) != null && announce) broadcast(player.getServer(), player.getGameProfile().getName() + " is no longer AFK."); }
    private String filter(String input) {
        char[] characters = input.toCharArray(); String lower = input.toLowerCase(Locale.ROOT);
        for (String word : Config.filteredWords()) {
            String needle = word.toLowerCase(Locale.ROOT); int index = lower.indexOf(needle);
            while (index >= 0) { Arrays.fill(characters, index, index + needle.length(), '*'); index = lower.indexOf(needle, index + needle.length()); }
        }
        return new String(characters);
    }
    private static String ignoreKey(UUID player) { return "ignore." + player; }
    private static void broadcast(MinecraftServer server, String message) { if (server != null) server.getPlayerList().getPlayers().forEach(player -> player.sendSystemMessage(Component.literal(message))); }
}
