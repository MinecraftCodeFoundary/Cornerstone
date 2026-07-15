package org.yeyao.cornerstone;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.yeyao.cornerstone.api.CornerstoneApi;
import org.yeyao.cornerstone.core.CornerstoneServices;

/** Bootstrap for the server-only Cornerstone service layer. */
@Mod(Cornerstone.MODID)
public final class Cornerstone {
    public static final String MODID = "cornerstone";
    public static final Logger LOGGER = LogUtils.getLogger();
    private static final CornerstoneServices SERVICES = new CornerstoneServices(LOGGER);

    public Cornerstone(IEventBus modEventBus, ModContainer modContainer) {
        Config.load();
        CornerstoneApi.install(SERVICES);
    }

    public static CornerstoneServices services() {
        return SERVICES;
    }
}
