package org.yeyao.cornerstone.economy;

import java.util.UUID;

/** Published only after a balance-changing transaction has been atomically persisted. */
public record BalanceChangedEvent(UUID accountId, long previousBalance, long newBalance, UUID transactionId, String cause) {
}
