package org.yeyao.cornerstone;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** JSON-backed server settings. Comments are represented as _comment fields because standard JSON has no comments. */
public final class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FMLPaths.CONFIGDIR.get().resolve("cornerstone.json");
    private static final Set<String> MODULES = Set.of("core", "teleport", "warps", "social", "moderation", "operations", "economy");
    private static volatile Map<String, Boolean> modules = defaultModules();
    private static volatile int autoSaveTicks = 6_000, teleportDelayTicks = 60, teleportCooldownTicks = 100, tpaExpirySeconds = 60, maxHomes = 3, announcementIntervalTicks = 12_000;
    private static volatile boolean cancelOnMove = true, recordTeleportOrigin, joinLeaveMessages = true;
    private static volatile Set<String> blockedDimensions = Set.of(), blockedRtpBiomes = Set.of();
    private static volatile Map<String, RtpRule> rtpRules = Map.of("minecraft:overworld", new RtpRule("minecraft:overworld", 1000, 150, 32, 600));
    private static volatile Map<String, Long> warpCosts = Map.of();
    private static volatile String motd = "一个 Cornerstone 服务器";
    private static volatile List<String> welcomeMessages = List.of("欢迎你，{player}！"), rules = List.of("请友善地与其他玩家交流。"), announcements = List.of(), filteredWords = List.of();
    private static volatile long economyStartingBalance;
    private static volatile Map<String, List<String>> commandAliases = Map.of("gamemode", List.of("gm"));

    private Config() { }
    public static synchronized void load() {
        try {
            if (Files.notExists(FILE)) { Files.createDirectories(FILE.getParent()); Files.writeString(FILE, GSON.toJson(defaultJson()), StandardCharsets.UTF_8); }
            JsonElement parsed = JsonParser.parseString(Files.readString(FILE, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) throw new IllegalArgumentException("根节点必须是 JSON 对象");
            apply(parsed.getAsJsonObject());
            Cornerstone.LOGGER.info("已加载 Cornerstone JSON 配置：{}", FILE);
        } catch (IOException | RuntimeException exception) {
            Cornerstone.LOGGER.error("无法加载 Cornerstone 配置 {}，将继续使用安全默认值", FILE, exception);
            resetDefaults();
        }
    }
    public static Path path() { return FILE; }
    public static boolean moduleEnabled(String module) {
        if (!MODULES.contains(module)) return false;
        if (module.equals("core")) return modules.getOrDefault("core", true);
        if (!modules.getOrDefault("core", true)) return false;
        if (module.equals("warps")) return modules.getOrDefault("warps", true) && modules.getOrDefault("teleport", true);
        return modules.getOrDefault(module, true);
    }
    public static int autoSaveTicks() { return autoSaveTicks; }
    public static int teleportDelayTicks() { return teleportDelayTicks; }
    public static int teleportCooldownTicks() { return teleportCooldownTicks; }
    public static boolean cancelOnMove() { return cancelOnMove; }
    public static int tpaExpirySeconds() { return tpaExpirySeconds; }
    public static int maxHomes() { return maxHomes; }
    public static boolean recordTeleportOrigin() { return recordTeleportOrigin; }
    public static boolean isTeleportDimensionBlocked(String dimension) { return blockedDimensions.contains(dimension); }
    public static Optional<RtpRule> rtpRule(String dimension) { return Optional.ofNullable(rtpRules.get(dimension)); }
    public static boolean isRtpBiomeBlocked(String biome) { return blockedRtpBiomes.contains(biome); }
    public static long warpCost(String name) { return warpCosts.getOrDefault(name.toLowerCase(Locale.ROOT), 0L); }
    public static String motd() { return motd; }
    public static boolean joinLeaveMessages() { return joinLeaveMessages; }
    public static List<String> welcomeMessages() { return welcomeMessages; }
    public static List<String> rules() { return rules; }
    public static List<String> announcements() { return announcements; }
    public static int announcementIntervalTicks() { return announcementIntervalTicks; }
    public static List<String> filteredWords() { return filteredWords; }
    public static long economyStartingBalance() { return economyStartingBalance; }
    public static List<String> commandAliases(String command) { return commandAliases.getOrDefault(command, List.of()); }

    private static void apply(JsonObject root) {
        modules = moduleMap(object(root, "modules"));
        JsonObject storage = object(root, "storage"), teleport = object(root, "teleport"), social = object(root, "social"), economy = object(root, "economy"), commands = object(root, "commands");
        autoSaveTicks = integer(storage, "autoSaveTicks", 6_000, 200, 72_000); teleportDelayTicks = integer(teleport, "delayTicks", 60, 0, 1_200); teleportCooldownTicks = integer(teleport, "cooldownTicks", 100, 0, 72_000);
        cancelOnMove = bool(teleport, "cancelOnMove", true); tpaExpirySeconds = integer(teleport, "tpaExpirySeconds", 60, 5, 600); maxHomes = integer(teleport, "maxHomes", 3, 1, 100); recordTeleportOrigin = bool(teleport, "recordOriginForBack", false);
        blockedDimensions = strings(teleport, "blockedDimensions"); warpCosts = warpCosts(teleport); JsonObject rtp = object(teleport, "rtp"); rtpRules = rtpRules(strings(rtp, "dimensionRules")); blockedRtpBiomes = strings(rtp, "blockedBiomes");
        motd = string(social, "motd", "一个 Cornerstone 服务器"); joinLeaveMessages = bool(social, "joinLeaveMessages", true); welcomeMessages = list(social, "welcomeMessages", List.of("欢迎你，{player}！")); rules = list(social, "rules", List.of("请友善地与其他玩家交流。")); announcements = list(social, "announcements", List.of()); announcementIntervalTicks = integer(social, "announcementIntervalTicks", 12_000, 200, 72_000); filteredWords = list(social, "filteredWords", List.of());
        economyStartingBalance = longValue(economy, "startingBalance", 0L, 0L, Long.MAX_VALUE); commandAliases = aliases(strings(commands, "aliases"));
    }
    private static JsonObject defaultJson() {
        JsonObject root = new JsonObject(); root.addProperty("_comment", "Cornerstone 配置文件。修改 modules 后请重启服务器，使命令和事件重新注册。");
        JsonObject module = new JsonObject(); module.addProperty("_comment", "功能模块开关。关闭 core 会同时禁用所有依赖核心服务的模块。"); defaultModules().forEach(module::addProperty); root.add("modules", module);
        JsonObject storage = new JsonObject(); storage.addProperty("_comment", "数据自动保存间隔，单位为游戏刻（20 刻 = 1 秒）。"); storage.addProperty("autoSaveTicks", 6000); root.add("storage", storage);
        JsonObject teleport = new JsonObject(); teleport.addProperty("_comment", "传送与地标配置。"); teleport.addProperty("delayTicks", 60); teleport.addProperty("cooldownTicks", 100); teleport.addProperty("cancelOnMove", true); teleport.addProperty("tpaExpirySeconds", 60); teleport.addProperty("maxHomes", 3); teleport.addProperty("recordOriginForBack", false); teleport.add("blockedDimensions", array()); teleport.add("warpCosts", array());
        JsonObject rtp = new JsonObject(); rtp.addProperty("_comment", "随机传送规则格式：维度ID|最大半径|最小距离|尝试次数|冷却刻数。"); rtp.add("dimensionRules", array("minecraft:overworld|1000|150|32|600")); rtp.add("blockedBiomes", array()); teleport.add("rtp", rtp); root.add("teleport", teleport);
        JsonObject social = new JsonObject(); social.addProperty("_comment", "社交、公告和聊天过滤配置。"); social.addProperty("motd", "一个 Cornerstone 服务器"); social.addProperty("joinLeaveMessages", true); social.add("welcomeMessages", array("欢迎你，{player}！")); social.add("rules", array("请友善地与其他玩家交流。")); social.add("announcements", array()); social.addProperty("announcementIntervalTicks", 12000); social.add("filteredWords", array()); root.add("social", social);
        JsonObject economy = new JsonObject(); economy.addProperty("_comment", "新经济账户的初始余额，单位为服务器定义的最小货币单位。"); economy.addProperty("startingBalance", 0); root.add("economy", economy);
        JsonObject commands = new JsonObject(); commands.addProperty("_comment", "命令缩略名：原命令=别名1,别名2。当前支持 gamemode。"); commands.add("aliases", array("gamemode=gm")); root.add("commands", commands); return root;
    }
    private static void resetDefaults() { apply(defaultJson()); }
    private static Map<String, Boolean> defaultModules() { Map<String, Boolean> result = new LinkedHashMap<>(); MODULES.stream().sorted().forEach(key -> result.put(key, true)); return Map.copyOf(result); }
    private static JsonObject object(JsonObject parent, String key) { return parent.has(key) && parent.get(key).isJsonObject() ? parent.getAsJsonObject(key) : new JsonObject(); }
    private static boolean bool(JsonObject object, String key, boolean fallback) { try { return object.has(key) ? object.get(key).getAsBoolean() : fallback; } catch (RuntimeException ignored) { return fallback; } }
    private static String string(JsonObject object, String key, String fallback) { try { return object.has(key) ? object.get(key).getAsString() : fallback; } catch (RuntimeException ignored) { return fallback; } }
    private static int integer(JsonObject object, String key, int fallback, int min, int max) { try { return Math.clamp(object.has(key) ? object.get(key).getAsInt() : fallback, min, max); } catch (RuntimeException ignored) { return fallback; } }
    private static long longValue(JsonObject object, String key, long fallback, long min, long max) { try { return Math.clamp(object.has(key) ? object.get(key).getAsLong() : fallback, min, max); } catch (RuntimeException ignored) { return fallback; } }
    private static Set<String> strings(JsonObject object, String key) { if (!object.has(key) || !object.get(key).isJsonArray()) return Set.of(); LinkedHashSet<String> values = new LinkedHashSet<>(); for (JsonElement element : object.getAsJsonArray(key)) if (element.isJsonPrimitive()) values.add(element.getAsString()); return Set.copyOf(values); }
    private static List<String> list(JsonObject object, String key, List<String> fallback) { if (!object.has(key) || !object.get(key).isJsonArray()) return fallback; List<String> values = new ArrayList<>(); for (JsonElement element : object.getAsJsonArray(key)) if (element.isJsonPrimitive()) values.add(element.getAsString()); return List.copyOf(values); }
    private static Map<String, Boolean> moduleMap(JsonObject object) { Map<String, Boolean> values = new LinkedHashMap<>(defaultModules()); for (String module : MODULES) values.put(module, bool(object, module, true)); return Map.copyOf(values); }
    private static Map<String, RtpRule> rtpRules(Set<String> rules) { Map<String, RtpRule> values = new LinkedHashMap<>(); for (String rule : rules) { String[] p = rule.split("\\|", -1); try { if (p.length == 5) { int radius = Integer.parseInt(p[1]), minimum = Integer.parseInt(p[2]), attempts = Integer.parseInt(p[3]), cooldown = Integer.parseInt(p[4]); if (radius >= 16 && minimum >= 0 && minimum <= radius && attempts >= 1 && attempts <= 128 && cooldown >= 0) values.put(p[0], new RtpRule(p[0], radius, minimum, attempts, cooldown)); } } catch (NumberFormatException ignored) { } } return Map.copyOf(values); }
    private static Map<String, Long> warpCosts(JsonObject teleport) { Map<String, Long> values = new LinkedHashMap<>(); for (String entry : strings(teleport, "warpCosts")) { String[] p = entry.split("\\|", -1); try { if (p.length == 2 && Long.parseLong(p[1]) >= 0) values.put(p[0].toLowerCase(Locale.ROOT), Long.parseLong(p[1])); } catch (NumberFormatException ignored) { } } return Map.copyOf(values); }
    private static Map<String, List<String>> aliases(Set<String> entries) { Map<String, List<String>> values = new LinkedHashMap<>(); for (String entry : entries) { String[] p = entry.split("=", -1); if (p.length != 2 || !p[0].equals("gamemode")) continue; List<String> aliases = Arrays.stream(p[1].split(",", -1)).map(String::trim).filter(alias -> alias.matches("[a-z][a-z0-9_-]{0,31}") && !alias.equals("gamemode")).distinct().toList(); if (!aliases.isEmpty()) values.put("gamemode", aliases); } return values.isEmpty() ? Map.of() : Map.copyOf(values); }
    private static JsonArray array(String... values) { JsonArray array = new JsonArray(); for (String value : values) array.add(value); return array; }
    public record RtpRule(String dimension, int radius, int minimumDistance, int attempts, int cooldownTicks) { }
}
