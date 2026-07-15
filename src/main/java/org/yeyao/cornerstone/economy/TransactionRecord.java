package org.yeyao.cornerstone.economy;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Immutable ledger record. Transfer IDs are globally unique and are also idempotency keys. */
public record TransactionRecord(UUID id, UUID sourceAccount, UUID targetAccount, long amount,
                                String memo, Instant createdAt, TransactionStatus status, String result) {
    public TransactionRecord {
        if (amount <= 0) throw new IllegalArgumentException("Transaction amount must be positive");
        memo = truncate(memo, 256); result = truncate(result, 256);
    }
    private static String truncate(String value, int maximum) { return value == null ? "" : value.substring(0, Math.min(maximum, value.length())); }
}
