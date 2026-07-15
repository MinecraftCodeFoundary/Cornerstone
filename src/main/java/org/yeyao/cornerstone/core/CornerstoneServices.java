package org.yeyao.cornerstone.core;

import org.slf4j.Logger;
import org.yeyao.cornerstone.audit.AuditService;
import org.yeyao.cornerstone.command.CommandFramework;
import org.yeyao.cornerstone.data.PlayerDataService;
import org.yeyao.cornerstone.permission.PermissionService;

/** Single owner for M1 services and their lifecycle. */
public final class CornerstoneServices {
    private final PlayerDataService players = new PlayerDataService();
    private final PermissionService permissions = new PermissionService();
    private final AuditService audit = new AuditService();
    private final CommandFramework commands = new CommandFramework(permissions, audit);
    private final LifecycleService lifecycle;

    public CornerstoneServices(Logger logger) { lifecycle = new LifecycleService(this, logger); }
    public PlayerDataService players() { return players; }
    public PermissionService permissions() { return permissions; }
    public AuditService audit() { return audit; }
    public CommandFramework commands() { return commands; }
    public LifecycleService lifecycle() { return lifecycle; }
}
