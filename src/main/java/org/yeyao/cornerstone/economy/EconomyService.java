package org.yeyao.cornerstone.economy;

import org.yeyao.cornerstone.Config;
import org.yeyao.cornerstone.api.CornerstoneEventBus;
import org.yeyao.cornerstone.storage.AtomicFileStorage;
import org.yeyao.cornerstone.storage.StorageException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

/** Server-authoritative, integer-unit economy ledger with idempotent, atomic transfers. */
public final class EconomyService {
    private EconomyData data = new EconomyData();
    private AtomicFileStorage<EconomyData> storage;
    private final CornerstoneEventBus<BalanceChangedEvent> balanceEvents = new CornerstoneEventBus<>();
    public synchronized void open(Path directory) { storage = new AtomicFileStorage<>(directory.resolve("economy.dat"), 1, EconomyDataStorage.INSTANCE); data = storage.loadOrDefault(new EconomyData()); }
    public synchronized void save() { if (storage != null) storage.save(data); }
    public synchronized long ensureAccount(UUID accountId) {
        Long existing = data.balances.get(accountId); if (existing != null) return existing;
        long starting = Config.economyStartingBalance(); data.balances.put(accountId, starting); save(); return starting;
    }
    public synchronized long balance(UUID accountId) { return ensureAccount(accountId); }
    public synchronized EconomyTransferResult transfer(UUID transactionId, UUID source, UUID target, long amount, String memo) {
        if (transactionId == null || source == null || target == null || amount <= 0) return EconomyTransferResult.failed("Transaction id, accounts and a positive amount are required.");
        TransactionRecord previous = data.transactions.get(transactionId);
        if (previous != null) {
            if (!previous.sourceAccount().equals(source) || !previous.targetAccount().equals(target) || previous.amount() != amount) return EconomyTransferResult.failed("Transaction id was already used with different details.");
            return EconomyTransferResult.fromRecord(previous, true);
        }
        if (source.equals(target)) return persistFailure(transactionId, source, target, amount, memo, "Source and target accounts must differ.");
        long sourceBalance = ensureAccount(source); long targetBalance = ensureAccount(target);
        if (sourceBalance < amount) return persistFailure(transactionId, source, target, amount, memo, "Insufficient balance.");
        final long targetBalanceAfter;
        try { targetBalanceAfter = Math.addExact(targetBalance, amount); }
        catch (ArithmeticException exception) { return persistFailure(transactionId, source, target, amount, memo, "Target balance would overflow."); }
        TransactionRecord record = new TransactionRecord(transactionId, source, target, amount, memo, Instant.now(), TransactionStatus.COMPLETED, "Transfer completed.");
        data.balances.put(source, sourceBalance - amount); data.balances.put(target, targetBalanceAfter); data.transactions.put(transactionId, record);
        try { save(); }
        catch (StorageException exception) {
            data.balances.put(source, sourceBalance); data.balances.put(target, targetBalance); data.transactions.remove(transactionId);
            return EconomyTransferResult.failed("Transaction could not be persisted; balances were rolled back.");
        }
        balanceEvents.publish(new BalanceChangedEvent(source, sourceBalance, sourceBalance - amount, transactionId, memo));
        balanceEvents.publish(new BalanceChangedEvent(target, targetBalance, targetBalanceAfter, transactionId, memo));
        return EconomyTransferResult.fromRecord(record, false);
    }
    public synchronized Optional<TransactionRecord> transaction(UUID transactionId) { return Optional.ofNullable(data.transactions.get(transactionId)); }
    public synchronized List<TransactionRecord> history(UUID accountId) { return data.transactions.values().stream().filter(record -> record.sourceAccount().equals(accountId) || record.targetAccount().equals(accountId)).toList(); }
    public AutoCloseable onBalanceChanged(java.util.function.Consumer<BalanceChangedEvent> listener) { return balanceEvents.subscribe(listener); }
    public synchronized Path exportLedger(Path directory) {
        Path output = directory.resolve("economy-ledger.csv"); StringBuilder csv = new StringBuilder("id,source,target,amount,memo,time,status,result\n");
        for (TransactionRecord record : data.transactions.values()) csv.append(record.id()).append(',').append(record.sourceAccount()).append(',').append(record.targetAccount()).append(',').append(record.amount()).append(',').append(csv(record.memo())).append(',').append(record.createdAt()).append(',').append(record.status()).append(',').append(csv(record.result())).append('\n');
        try { Files.createDirectories(directory); Files.writeString(output, csv.toString(), StandardCharsets.UTF_8); return output; }
        catch (IOException exception) { throw new StorageException("Could not export economy ledger", exception); }
    }
    private EconomyTransferResult persistFailure(UUID id, UUID source, UUID target, long amount, String memo, String message) {
        TransactionRecord record = new TransactionRecord(id, source, target, amount, memo, Instant.now(), TransactionStatus.FAILED, message); data.transactions.put(id, record);
        try { save(); return EconomyTransferResult.fromRecord(record, false); }
        catch (StorageException exception) { data.transactions.remove(id); return EconomyTransferResult.failed("Transaction failure could not be persisted."); }
    }
    private static String csv(String value) { return '"' + value.replace("\"", "\"\"") + '"'; }
}
