package org.yeyao.cornerstone;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.neoforged.fml.loading.FMLPaths;
import org.yeyao.codefoundryapi.api.YamlFiles;
import org.yeyao.codefoundryapi.config.ConfigModel;
import org.yeyao.codefoundryapi.config.ConfigNode;
import org.yeyao.codefoundryapi.config.ReloadResult;
import org.yeyao.codefoundryapi.config.YamlConfiguration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Server settings backed by CodeFoundryAPI's safe YAML service. */
public final class Config {
    private static final Path FILE = FMLPaths.CONFIGDIR.get().resolve("cornerstone.yml");
    private static final Path LEGACY_FILE = FMLPaths.CONFIGDIR.get().resolve("cornerstone.json");
    private static final Set<String> MODULES = Set.of("core", "teleport", "warps", "social", "moderation", "operations", "economy");
    private static final String TEMPLATE = """
            # Cornerstone 配置文件。修改 modules 后请重启服务器，使命令和事件重新注册。
            configVersion: 1
            modules:
              # 关闭 core 会同时禁用所有依赖核心服务的模块。
              core: true
              teleport: true
              warps: true
              social: true
              moderation: true
              operations: true
              economy: true
            storage:
              # 数据自动保存间隔，单位为游戏刻（20 刻 = 1 秒）。
              autoSaveTicks: 6000
            teleport:
              delayTicks: 60
              cooldownTicks: 100
              cancelOnMove: true
              tpaExpirySeconds: 60
              maxHomes: 3
              recordOriginForBack: false
              blockedDimensions: []
              # 格式：地标名|费用。
              warpCosts: []
              rtp:
                # 格式：维度ID|最大半径|最小距离|尝试次数|冷却刻数。
                dimensionRules: ["minecraft:overworld|1000|150|32|600"]
                blockedBiomes: []
            social:
              motd: "一个 Cornerstone 服务器"
              joinLeaveMessages: true
              welcomeMessages: ["欢迎你，{player}！"]
              rules: ["请友善地与其他玩家交流。"]
              announcements: []
              announcementIntervalTicks: 12000
              filteredWords: []
            economy:
              startingBalance: 0
            commands:
              # 格式：原命令=别名1,别名2。
              aliases: ["gamemode=gm"]
            """;
    private static final YamlConfiguration<Settings> CONFIGURATION = new YamlConfiguration<>(FILE, new SettingsModel(), TEMPLATE);
    private static volatile Settings settings = Settings.defaults();

    private Config() {
    }

    public static synchronized void load() {
        migrateLegacyJson();
        ReloadResult<Settings> result = CONFIGURATION.reload();
        settings = result.value();
        if (result.success()) {
            Cornerstone.LOGGER.info("Loaded Cornerstone YAML configuration: {}", FILE);
        } else {
            Cornerstone.LOGGER.error("Unable to reload Cornerstone configuration {}; retaining the last valid values: {}", FILE, result.message());
        }
    }

    public static Path path() { return FILE; }
    public static boolean moduleEnabled(String module) {
        if (!MODULES.contains(module)) return false;
        if (!settings.modules().getOrDefault("core", true)) return false;
        if (module.equals("core")) return true;
        if (!settings.modules().getOrDefault(module, true)) return false;
        return !module.equals("warps") || settings.modules().getOrDefault("teleport", true);
    }
    public static int autoSaveTicks() { return settings.autoSaveTicks(); }
    public static int teleportDelayTicks() { return settings.teleportDelayTicks(); }
    public static int teleportCooldownTicks() { return settings.teleportCooldownTicks(); }
    public static boolean cancelOnMove() { return settings.cancelOnMove(); }
    public static int tpaExpirySeconds() { return settings.tpaExpirySeconds(); }
    public static int maxHomes() { return settings.maxHomes(); }
    public static boolean recordTeleportOrigin() { return settings.recordTeleportOrigin(); }
    public static boolean isTeleportDimensionBlocked(String dimension) { return settings.blockedDimensions().contains(dimension); }
    public static Optional<RtpRule> rtpRule(String dimension) { return Optional.ofNullable(settings.rtpRules().get(dimension)); }
    public static boolean isRtpBiomeBlocked(String biome) { return settings.blockedRtpBiomes().contains(biome); }
    public static long warpCost(String name) { return settings.warpCosts().getOrDefault(name.toLowerCase(Locale.ROOT), 0L); }
    public static String motd() { return settings.motd(); }
    public static boolean joinLeaveMessages() { return settings.joinLeaveMessages(); }
    public static List<String> welcomeMessages() { return settings.welcomeMessages(); }
    public static List<String> rules() { return settings.rules(); }
    public static List<String> announcements() { return settings.announcements(); }
    public static int announcementIntervalTicks() { return settings.announcementIntervalTicks(); }
    public static List<String> filteredWords() { return settings.filteredWords(); }
    public static long economyStartingBalance() { return settings.economyStartingBalance(); }
    public static List<String> commandAliases(String command) { return settings.commandAliases().getOrDefault(command, List.of()); }

    private static void migrateLegacyJson() {
        if (Files.exists(FILE) || Files.notExists(LEGACY_FILE)) return;
        try {
            JsonElement legacy = JsonParser.parseString(Files.readString(LEGACY_FILE, StandardCharsets.UTF_8));
            if (!legacy.isJsonObject()) throw new IllegalArgumentException("Legacy configuration root is not an object");
            Map<String, Object> values = objectToMap(legacy.getAsJsonObject());
            values.put("configVersion", 1);
            YamlFiles.save(FILE, values);
            Files.copy(LEGACY_FILE, LEGACY_FILE.resolveSibling("cornerstone.json.bak"), StandardCopyOption.REPLACE_EXISTING);
            Cornerstone.LOGGER.info("Migrated legacy Cornerstone JSON configuration to {}; original retained at {}", FILE, LEGACY_FILE);
        } catch (IOException | RuntimeException exception) {
            Cornerstone.LOGGER.error("Unable to migrate legacy Cornerstone JSON configuration {}; defaults will be used", LEGACY_FILE, exception);
        }
    }

    private static Map<String, Object> objectToMap(JsonObject object) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) result.put(entry.getKey(), jsonValue(entry.getValue()));
        return result;
    }
    private static Object jsonValue(JsonElement element) {
        if (element.isJsonObject()) return objectToMap(element.getAsJsonObject());
        if (element.isJsonArray()) { List<Object> values = new ArrayList<>(); for (JsonElement child : element.getAsJsonArray()) values.add(jsonValue(child)); return values; }
        if (element.isJsonNull()) return null;
        if (element.getAsJsonPrimitive().isBoolean()) return element.getAsBoolean();
        if (element.getAsJsonPrimitive().isNumber()) return element.getAsNumber();
        return element.getAsString();
    }

    private static final class SettingsModel implements ConfigModel<Settings> {
        @Override public int version() { return 1; }
        @Override public Map<String, Object> defaults() { return defaultValues(); }
        @Override public Settings decode(ConfigNode root) {
            ConfigNode modules = root.child("modules"), storage = root.child("storage"), teleport = root.child("teleport"), social = root.child("social"), economy = root.child("economy"), commands = root.child("commands");
            Map<String, Boolean> moduleValues = new LinkedHashMap<>();
            for (String module : MODULES) moduleValues.put(module, modules.bool(module, true));
            ConfigNode rtp = teleport.child("rtp");
            return new Settings(Map.copyOf(moduleValues), storage.integer("autoSaveTicks", 6_000, 200, 72_000), teleport.integer("delayTicks", 60, 0, 1_200), teleport.integer("cooldownTicks", 100, 0, 72_000), teleport.bool("cancelOnMove", true), teleport.integer("tpaExpirySeconds", 60, 5, 600), teleport.integer("maxHomes", 3, 1, 100), teleport.bool("recordOriginForBack", false), Set.copyOf(teleport.strings("blockedDimensions", List.of())), rtpRules(rtp.strings("dimensionRules", List.of("minecraft:overworld|1000|150|32|600"))), Set.copyOf(rtp.strings("blockedBiomes", List.of())), warpCosts(teleport.strings("warpCosts", List.of())), social.string("motd", "一个 Cornerstone 服务器"), social.bool("joinLeaveMessages", true), social.strings("welcomeMessages", List.of("欢迎你，{player}！")), social.strings("rules", List.of("请友善地与其他玩家交流。")), social.strings("announcements", List.of()), social.integer("announcementIntervalTicks", 12_000, 200, 72_000), social.strings("filteredWords", List.of()), economy.longValue("startingBalance", 0, 0, Long.MAX_VALUE), aliases(commands.strings("aliases", List.of("gamemode=gm"))));
        }
    }

    private static Map<String, Object> defaultValues() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("configVersion", 1); root.put("modules", Map.of("core", true, "teleport", true, "warps", true, "social", true, "moderation", true, "operations", true, "economy", true));
        root.put("storage", Map.of("autoSaveTicks", 6000)); root.put("teleport", Map.of("delayTicks", 60, "cooldownTicks", 100, "cancelOnMove", true, "tpaExpirySeconds", 60, "maxHomes", 3, "recordOriginForBack", false, "blockedDimensions", List.of(), "warpCosts", List.of(), "rtp", Map.of("dimensionRules", List.of("minecraft:overworld|1000|150|32|600"), "blockedBiomes", List.of())));
        root.put("social", Map.of("motd", "一个 Cornerstone 服务器", "joinLeaveMessages", true, "welcomeMessages", List.of("欢迎你，{player}！"), "rules", List.of("请友善地与其他玩家交流。"), "announcements", List.of(), "announcementIntervalTicks", 12000, "filteredWords", List.of()));
        root.put("economy", Map.of("startingBalance", 0)); root.put("commands", Map.of("aliases", List.of("gamemode=gm")));
        return root;
    }
    private static Map<String, RtpRule> rtpRules(List<String> rules) {
        Map<String, RtpRule> values = new LinkedHashMap<>();
        for (String rule : rules) { String[] p = rule.split("\\|", -1); try { if (p.length == 5) { int radius = Integer.parseInt(p[1]), minimum = Integer.parseInt(p[2]), attempts = Integer.parseInt(p[3]), cooldown = Integer.parseInt(p[4]); if (radius >= 16 && minimum >= 0 && minimum <= radius && attempts >= 1 && attempts <= 128 && cooldown >= 0) values.put(p[0], new RtpRule(p[0], radius, minimum, attempts, cooldown)); } } catch (NumberFormatException ignored) { } }
        return Map.copyOf(values);
    }
    private static Map<String, Long> warpCosts(List<String> entries) {
        Map<String, Long> values = new LinkedHashMap<>();
        for (String entry : entries) { String[] p = entry.split("\\|", -1); try { if (p.length == 2 && Long.parseLong(p[1]) >= 0) values.put(p[0].toLowerCase(Locale.ROOT), Long.parseLong(p[1])); } catch (NumberFormatException ignored) { } }
        return Map.copyOf(values);
    }
    private static Map<String, List<String>> aliases(List<String> entries) {
        Map<String, List<String>> values = new LinkedHashMap<>();
        for (String entry : entries) { String[] p = entry.split("=", -1); if (p.length != 2 || !p[0].equals("gamemode")) continue; List<String> aliases = Arrays.stream(p[1].split(",", -1)).map(String::trim).filter(alias -> alias.matches("[a-z][a-z0-9_-]{0,31}") && !alias.equals("gamemode")).distinct().toList(); if (!aliases.isEmpty()) values.put("gamemode", aliases); }
        return values.isEmpty() ? Map.of() : Map.copyOf(values);
    }

    private record Settings(Map<String, Boolean> modules, int autoSaveTicks, int teleportDelayTicks, int teleportCooldownTicks, boolean cancelOnMove, int tpaExpirySeconds, int maxHomes, boolean recordTeleportOrigin, Set<String> blockedDimensions, Map<String, RtpRule> rtpRules, Set<String> blockedRtpBiomes, Map<String, Long> warpCosts, String motd, boolean joinLeaveMessages, List<String> welcomeMessages, List<String> rules, List<String> announcements, int announcementIntervalTicks, List<String> filteredWords, long economyStartingBalance, Map<String, List<String>> commandAliases) {
        private static Settings defaults() { return new SettingsModel().decode(new ConfigNode(defaultValues())); }
    }
    public record RtpRule(String dimension, int radius, int minimumDistance, int attempts, int cooldownTicks) { }
}
