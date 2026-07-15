package org.yeyao.cornerstone.api;

import org.yeyao.cornerstone.core.CornerstoneServices;
import org.yeyao.cornerstone.data.PlayerDataService;
import org.yeyao.cornerstone.permission.PermissionService;
import org.yeyao.cornerstone.audit.AuditService;

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

    private static CornerstoneServices require() {
        CornerstoneServices value = services;
        if (value == null) throw new IllegalStateException("Cornerstone has not been initialized");
        return value;
    }
}
