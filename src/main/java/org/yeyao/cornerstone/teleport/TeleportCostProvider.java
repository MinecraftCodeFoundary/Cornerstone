package org.yeyao.cornerstone.teleport;

import net.minecraft.server.level.ServerPlayer;
import org.yeyao.cornerstone.data.StoredLocation;

import java.util.Optional;

/** Economy integration point. M2's default provider is free until an economy module is installed. */
public interface TeleportCostProvider {
    Optional<String> cannotPay(ServerPlayer player, StoredLocation destination);
    boolean withdraw(ServerPlayer player, StoredLocation destination);
    default Optional<String> cannotPay(TeleportContext context) { return cannotPay(context.player(), context.destination()); }
    default boolean withdraw(TeleportContext context) { return withdraw(context.player(), context.destination()); }
    static TeleportCostProvider free() {
        return new TeleportCostProvider() {
            @Override public Optional<String> cannotPay(ServerPlayer player, StoredLocation destination) { return Optional.empty(); }
            @Override public boolean withdraw(ServerPlayer player, StoredLocation destination) { return true; }
        };
    }
}
