package org.yeyao.cornerstone.core;

import org.slf4j.Logger;
import org.yeyao.cornerstone.audit.AuditService;
import org.yeyao.cornerstone.command.CommandFramework;
import org.yeyao.cornerstone.data.PlayerDataService;
import org.yeyao.cornerstone.permission.PermissionService;
import org.yeyao.cornerstone.teleport.TeleportService;
import org.yeyao.cornerstone.teleport.RandomTeleportService;
import org.yeyao.cornerstone.social.SocialService;
import org.yeyao.cornerstone.moderation.ModerationService;
import org.yeyao.cornerstone.operations.MaintenanceService;
import org.yeyao.cornerstone.operations.ServerScheduleService;
import org.yeyao.cornerstone.operations.UtilityService;
import org.yeyao.cornerstone.economy.EconomyService;

/** Single owner for M1 services and their lifecycle. */
public final class CornerstoneServices {
    private final PlayerDataService players = new PlayerDataService();
    private final PermissionService permissions = new PermissionService();
    private final AuditService audit = new AuditService();
    private final CommandFramework commands = new CommandFramework(permissions, audit);
    private final TeleportService teleports = new TeleportService();
    private final RandomTeleportService randomTeleports = new RandomTeleportService(teleports);
    private final SocialService social = new SocialService(players);
    private final ModerationService moderation = new ModerationService();
    private final MaintenanceService maintenance = new MaintenanceService(permissions);
    private final ServerScheduleService schedules = new ServerScheduleService();
    private final UtilityService utilities = new UtilityService();
    private final EconomyService economy = new EconomyService();
    private final LifecycleService lifecycle;

    public CornerstoneServices(Logger logger) { lifecycle = new LifecycleService(this, logger); }
    public PlayerDataService players() { return players; }
    public PermissionService permissions() { return permissions; }
    public AuditService audit() { return audit; }
    public CommandFramework commands() { return commands; }
    public TeleportService teleports() { return teleports; }
    public RandomTeleportService randomTeleports() { return randomTeleports; }
    public SocialService social() { return social; }
    public ModerationService moderation() { return moderation; }
    public MaintenanceService maintenance() { return maintenance; }
    public ServerScheduleService schedules() { return schedules; }
    public UtilityService utilities() { return utilities; }
    public EconomyService economy() { return economy; }
    public LifecycleService lifecycle() { return lifecycle; }
}
