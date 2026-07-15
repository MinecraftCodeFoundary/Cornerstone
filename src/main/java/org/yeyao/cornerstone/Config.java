package org.yeyao.cornerstone;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Server settings that are deliberately small and safe to reload. */
@EventBusSubscriber(modid = Cornerstone.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    private static final ModConfigSpec.IntValue AUTO_SAVE_TICKS = BUILDER
            .comment("Interval between Cornerstone data saves, in ticks.")
            .defineInRange("storage.autoSaveTicks", 6_000, 200, 72_000);
    public static final ModConfigSpec SPEC = BUILDER.build();

    private static volatile int autoSaveTicks = 6_000;

    private Config() {
    }

    public static int autoSaveTicks() {
        return autoSaveTicks;
    }

    @SubscribeEvent
    static void onConfigLoad(ModConfigEvent event) {
        if (event.getConfig().getSpec() == SPEC) {
            autoSaveTicks = AUTO_SAVE_TICKS.get();
        }
    }
}
