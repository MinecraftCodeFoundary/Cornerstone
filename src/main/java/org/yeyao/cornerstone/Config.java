package org.yeyao.cornerstone;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Server settings that are deliberately small and safe to reload. */
@EventBusSubscriber(modid = Cornerstone.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    private static final ModConfigSpec.IntValue AUTO_SAVE_TICKS = BUILDER
            .comment("Interval between Cornerstone data saves, in ticks.")
            .defineInRange("storage.autoSaveTicks", 6_000, 200, 72_000);
    private static final ModConfigSpec.IntValue TELEPORT_DELAY_TICKS = BUILDER
            .comment("Delay before a teleport is performed, in ticks.")
            .defineInRange("teleport.delayTicks", 60, 0, 1_200);
    private static final ModConfigSpec.IntValue TELEPORT_COOLDOWN_TICKS = BUILDER
            .comment("Cooldown after a successful teleport, in ticks.")
            .defineInRange("teleport.cooldownTicks", 100, 0, 72_000);
    private static final ModConfigSpec.BooleanValue CANCEL_ON_MOVE = BUILDER
            .comment("Cancel delayed teleports when the player moves more than one block.")
            .define("teleport.cancelOnMove", true);
    private static final ModConfigSpec.IntValue TPA_EXPIRY_SECONDS = BUILDER
            .comment("How long a /tpa request remains valid, in seconds.")
            .defineInRange("teleport.tpaExpirySeconds", 60, 5, 600);
    private static final ModConfigSpec.IntValue MAX_HOMES = BUILDER
            .comment("Maximum named homes per player.")
            .defineInRange("teleport.maxHomes", 3, 1, 100);
    private static final ModConfigSpec.BooleanValue RECORD_TELEPORT_ORIGIN = BUILDER
            .comment("Whether /back also records the position before a successful teleport.")
            .define("teleport.recordOriginForBack", false);
    private static final ModConfigSpec.ConfigValue<List<? extends String>> BLOCKED_DIMENSIONS = BUILDER
            .comment("Dimension ids where Cornerstone teleport destinations are forbidden.")
            .defineListAllowEmpty("teleport.blockedDimensions", List.of(), value -> value instanceof String id && id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+"));
    private static final ModConfigSpec.ConfigValue<List<? extends String>> RTP_DIMENSION_RULES = BUILDER
            .comment("RTP rules: dimension|radius|minDistance|attempts|cooldownTicks. Add one rule for each allowed dimension.")
            .defineListAllowEmpty("teleport.rtp.dimensionRules", List.of("minecraft:overworld|1000|150|32|600"), Config::validRtpRule);
    private static final ModConfigSpec.ConfigValue<List<? extends String>> RTP_BLOCKED_BIOMES = BUILDER
            .comment("Biome ids excluded from random teleport destinations.")
            .defineListAllowEmpty("teleport.rtp.blockedBiomes", List.of(), value -> value instanceof String id && id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+"));
    private static final ModConfigSpec.ConfigValue<List<? extends String>> WARP_COSTS = BUILDER
            .comment("Optional warp prices: warpName|amount. Amounts are passed to the installed economy provider.")
            .defineListAllowEmpty("teleport.warpCosts", List.of(), Config::validWarpCost);
    private static final ModConfigSpec.ConfigValue<String> MOTD = BUILDER
            .comment("Server list MOTD managed by Cornerstone.")
            .define("social.motd", "A Cornerstone server");
    private static final ModConfigSpec.BooleanValue JOIN_LEAVE_MESSAGES = BUILDER
            .comment("Broadcast Cornerstone join and leave messages.")
            .define("social.joinLeaveMessages", true);
    private static final ModConfigSpec.ConfigValue<List<? extends String>> WELCOME_MESSAGES = BUILDER
            .comment("Lines sent to a player when they join. Use {player} as a placeholder.")
            .defineListAllowEmpty("social.welcomeMessages", List.of("Welcome, {player}!"), Config::validMessageLine);
    private static final ModConfigSpec.ConfigValue<List<? extends String>> RULES = BUILDER
            .comment("Lines displayed by /rules.")
            .defineListAllowEmpty("social.rules", List.of("Be respectful."), Config::validMessageLine);
    private static final ModConfigSpec.ConfigValue<List<? extends String>> ANNOUNCEMENTS = BUILDER
            .comment("Announcements broadcast in rotation. Empty disables announcements.")
            .defineListAllowEmpty("social.announcements", List.of(), Config::validMessageLine);
    private static final ModConfigSpec.IntValue ANNOUNCEMENT_INTERVAL_TICKS = BUILDER
            .comment("Ticks between announcements.")
            .defineInRange("social.announcementIntervalTicks", 12_000, 200, 72_000);
    private static final ModConfigSpec.ConfigValue<List<? extends String>> FILTERED_WORDS = BUILDER
            .comment("Case-insensitive words replaced in chat. Empty disables the filter.")
            .defineListAllowEmpty("social.filteredWords", List.of(), Config::validFilteredWord);
    private static final ModConfigSpec.LongValue ECONOMY_STARTING_BALANCE = BUILDER
            .comment("Balance assigned when an account is first created, in the economy's smallest unit.")
            .defineInRange("economy.startingBalance", 0L, 0L, Long.MAX_VALUE);
    public static final ModConfigSpec SPEC = BUILDER.build();

    private static volatile int autoSaveTicks = 6_000;
    private static volatile int teleportDelayTicks = 60;
    private static volatile int teleportCooldownTicks = 100;
    private static volatile boolean cancelOnMove = true;
    private static volatile int tpaExpirySeconds = 60;
    private static volatile int maxHomes = 3;
    private static volatile boolean recordTeleportOrigin;
    private static volatile Set<String> blockedDimensions = Set.of();
    private static volatile Map<String, RtpRule> rtpRules = Map.of();
    private static volatile Set<String> blockedRtpBiomes = Set.of();
    private static volatile Map<String, Long> warpCosts = Map.of();
    private static volatile String motd = "A Cornerstone server";
    private static volatile boolean joinLeaveMessages = true;
    private static volatile List<String> welcomeMessages = List.of("Welcome, {player}!");
    private static volatile List<String> rules = List.of("Be respectful.");
    private static volatile List<String> announcements = List.of();
    private static volatile int announcementIntervalTicks = 12_000;
    private static volatile List<String> filteredWords = List.of();
    private static volatile long economyStartingBalance;

    private Config() {
    }

    public static int autoSaveTicks() {
        return autoSaveTicks;
    }
    public static int teleportDelayTicks() { return teleportDelayTicks; }
    public static int teleportCooldownTicks() { return teleportCooldownTicks; }
    public static boolean cancelOnMove() { return cancelOnMove; }
    public static int tpaExpirySeconds() { return tpaExpirySeconds; }
    public static int maxHomes() { return maxHomes; }
    public static boolean recordTeleportOrigin() { return recordTeleportOrigin; }
    public static boolean isTeleportDimensionBlocked(String dimension) { return blockedDimensions.contains(dimension); }
    public static Optional<RtpRule> rtpRule(String dimension) { return Optional.ofNullable(rtpRules.get(dimension)); }
    public static boolean isRtpBiomeBlocked(String biome) { return blockedRtpBiomes.contains(biome); }
    public static long warpCost(String name) { return warpCosts.getOrDefault(name.toLowerCase(java.util.Locale.ROOT), 0L); }
    public static String motd() { return motd; }
    public static boolean joinLeaveMessages() { return joinLeaveMessages; }
    public static List<String> welcomeMessages() { return welcomeMessages; }
    public static List<String> rules() { return rules; }
    public static List<String> announcements() { return announcements; }
    public static int announcementIntervalTicks() { return announcementIntervalTicks; }
    public static List<String> filteredWords() { return filteredWords; }
    public static long economyStartingBalance() { return economyStartingBalance; }

    @SubscribeEvent
    static void onConfigLoad(ModConfigEvent event) {
        if (event.getConfig().getSpec() == SPEC) {
            autoSaveTicks = AUTO_SAVE_TICKS.get();
            teleportDelayTicks = TELEPORT_DELAY_TICKS.get();
            teleportCooldownTicks = TELEPORT_COOLDOWN_TICKS.get();
            cancelOnMove = CANCEL_ON_MOVE.get();
            tpaExpirySeconds = TPA_EXPIRY_SECONDS.get();
            maxHomes = MAX_HOMES.get();
            recordTeleportOrigin = RECORD_TELEPORT_ORIGIN.get();
            blockedDimensions = Set.copyOf(BLOCKED_DIMENSIONS.get());
            Map<String, RtpRule> loadedRules = new LinkedHashMap<>();
            for (String rule : RTP_DIMENSION_RULES.get()) {
                RtpRule parsed = parseRtpRule(rule); loadedRules.put(parsed.dimension(), parsed);
            }
            rtpRules = Map.copyOf(loadedRules);
            blockedRtpBiomes = Set.copyOf(RTP_BLOCKED_BIOMES.get());
            Map<String, Long> loadedCosts = new LinkedHashMap<>();
            for (String entry : WARP_COSTS.get()) {
                String[] parts = entry.split("\\|", -1); loadedCosts.put(parts[0].toLowerCase(java.util.Locale.ROOT), Long.parseLong(parts[1]));
            }
            warpCosts = Map.copyOf(loadedCosts);
            motd = MOTD.get();
            joinLeaveMessages = JOIN_LEAVE_MESSAGES.get();
            welcomeMessages = List.copyOf(WELCOME_MESSAGES.get());
            rules = List.copyOf(RULES.get());
            announcements = List.copyOf(ANNOUNCEMENTS.get());
            announcementIntervalTicks = ANNOUNCEMENT_INTERVAL_TICKS.get();
            filteredWords = List.copyOf(FILTERED_WORDS.get());
            economyStartingBalance = ECONOMY_STARTING_BALANCE.get();
        }
    }
    private static boolean validRtpRule(Object value) {
        if (!(value instanceof String rule)) return false;
        try { parseRtpRule(rule); return true; } catch (IllegalArgumentException ignored) { return false; }
    }
    private static RtpRule parseRtpRule(String value) {
        String[] parts = value.split("\\|", -1);
        if (parts.length != 5 || !parts[0].matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) throw new IllegalArgumentException("Invalid RTP rule");
        try {
            int radius = Integer.parseInt(parts[1]); int minimum = Integer.parseInt(parts[2]); int attempts = Integer.parseInt(parts[3]); int cooldown = Integer.parseInt(parts[4]);
            if (radius < 16 || radius > 29_000_000 || minimum < 0 || minimum > radius || attempts < 1 || attempts > 128 || cooldown < 0 || cooldown > 72_000) throw new IllegalArgumentException("Invalid RTP rule range");
            return new RtpRule(parts[0], radius, minimum, attempts, cooldown);
        } catch (NumberFormatException exception) { throw new IllegalArgumentException("Invalid RTP rule number", exception); }
    }
    private static boolean validWarpCost(Object value) {
        if (!(value instanceof String entry)) return false;
        String[] parts = entry.split("\\|", -1);
        if (parts.length != 2 || !parts[0].matches("[A-Za-z0-9_-]{1,32}")) return false;
        try { return Long.parseLong(parts[1]) >= 0; } catch (NumberFormatException ignored) { return false; }
    }
    private static boolean validMessageLine(Object value) { return value instanceof String line && !line.isBlank() && line.length() <= 512; }
    private static boolean validFilteredWord(Object value) { return value instanceof String word && !word.isBlank() && word.length() <= 64; }
    public record RtpRule(String dimension, int radius, int minimumDistance, int attempts, int cooldownTicks) { }
}
