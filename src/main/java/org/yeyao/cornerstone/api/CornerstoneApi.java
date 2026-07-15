package org.yeyao.cornerstone.api;

import org.yeyao.cornerstone.core.CornerstoneServices;
import org.yeyao.cornerstone.data.PlayerDataService;
import org.yeyao.cornerstone.permission.PermissionService;
import org.yeyao.cornerstone.audit.AuditService;
import org.yeyao.cornerstone.teleport.TeleportService;
import org.yeyao.cornerstone.teleport.RandomTeleportService;
import org.yeyao.cornerstone.social.SocialService;
import org.yeyao.cornerstone.moderation.ModerationService;
import org.yeyao.cornerstone.operations.MaintenanceService;
import org.yeyao.cornerstone.operations.ServerScheduleService;
import org.yeyao.cornerstone.economy.EconomyService;
import org.yeyao.cornerstone.Config;

/** Stable entry point for companion mods. Internal storage is never exposed. */
public final class CornerstoneApi {
    private static volatile CornerstoneServices services;

    private CornerstoneApi() {
    }

    public static void install(CornerstoneServices value) {
        if (services != null) throw new IllegalStateException("Cornerstone API was already installed");
        services = value;
    }

    public static PlayerDataService players() { requireModule("core"); return require().players(); }
    public static PermissionService permissions() { requireModule("core"); return require().permissions(); }
    public static AuditService audit() { requireModule("core"); return require().audit(); }
    public static TeleportService teleports() { requireModule("teleport"); return require().teleports(); }
    public static RandomTeleportService randomTeleports() { requireModule("warps"); return require().randomTeleports(); }
    public static SocialService social() { requireModule("social"); return require().social(); }
    public static ModerationService moderation() { requireModule("moderation"); return require().moderation(); }
    public static MaintenanceService maintenance() { requireModule("operations"); return require().maintenance(); }
    public static ServerScheduleService schedules() { requireModule("operations"); return require().schedules(); }
    public static EconomyService economy() { requireModule("economy"); return require().economy(); }

    private static CornerstoneServices require() {
        CornerstoneServices value = services;
        if (value == null) throw new IllegalStateException("Cornerstone has not been initialized");
        return value;
    }
    private static void requireModule(String module) {
        if (!Config.moduleEnabled(module)) throw new IllegalStateException("Cornerstone module is disabled: " + module);
    }
}
