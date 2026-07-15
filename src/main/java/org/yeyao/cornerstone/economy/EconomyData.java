package org.yeyao.cornerstone.economy;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

final class EconomyData {
    final Map<UUID, Long> balances = new LinkedHashMap<>();
    final Map<UUID, TransactionRecord> transactions = new LinkedHashMap<>();
}
