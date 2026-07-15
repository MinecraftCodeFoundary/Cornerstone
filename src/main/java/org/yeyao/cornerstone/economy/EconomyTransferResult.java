package org.yeyao.cornerstone.economy;

import java.util.Optional;

/** Result of a transfer; repeated IDs return the original ledger result without moving money again. */
public record EconomyTransferResult(boolean success, boolean idempotentReplay, String message, Optional<TransactionRecord> transaction) {
    public static EconomyTransferResult fromRecord(TransactionRecord record, boolean replay) { return new EconomyTransferResult(record.status() == TransactionStatus.COMPLETED, replay, record.result(), Optional.of(record)); }
    public static EconomyTransferResult failed(String message) { return new EconomyTransferResult(false, false, message, Optional.empty()); }
}
