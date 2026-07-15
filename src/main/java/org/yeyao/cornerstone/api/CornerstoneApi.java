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

/** Stable entry point for companion mods. Internal storage is never exposed. */
public final class CornerstoneApi {
    private static volatile CornerstoneServices services;

    private CornerstoneApi() {
    }

    public static void install(CornerstoneServices value) {
        if (services != null) throw new IllegalStateException("Cornerstone API was already installed");
        services = value;
    }

    public static PlayerDataService players() { return require().players(); }
    public static PermissionService permissions() { return require().permissions(); }
    public static AuditService audit() { return require().audit(); }
    public static TeleportService teleports() { return require().teleports(); }
    public static RandomTeleportService randomTeleports() { return require().randomTeleports(); }
    public static SocialService social() { return require().social(); }
    public static ModerationService moderation() { return require().moderation(); }
    public static MaintenanceService maintenance() { return require().maintenance(); }
    public static ServerScheduleService schedules() { return require().schedules(); }
    public static EconomyService economy() { return require().economy(); }

    private static CornerstoneServices require() {
        CornerstoneServices value = services;
        if (value == null) throw new IllegalStateException("Cornerstone has not been initialized");
        return value;
    }
}
