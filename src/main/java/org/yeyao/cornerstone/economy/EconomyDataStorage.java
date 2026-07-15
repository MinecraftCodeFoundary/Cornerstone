package org.yeyao.cornerstone.economy;

import org.yeyao.cornerstone.storage.DataSerializer;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

final class EconomyDataStorage implements DataSerializer<EconomyData> {
    static final EconomyDataStorage INSTANCE = new EconomyDataStorage();
    private static final int MAX_ACCOUNTS = 1_000_000;
    private static final int MAX_TRANSACTIONS = 5_000_000;
    private EconomyDataStorage() { }
    @Override public void write(DataOutput out, EconomyData data) throws IOException {
        out.writeInt(data.balances.size()); for (var entry : data.balances.entrySet()) { uuid(out, entry.getKey()); out.writeLong(entry.getValue()); }
        out.writeInt(data.transactions.size());
        for (TransactionRecord record : data.transactions.values()) {
            uuid(out, record.id()); uuid(out, record.sourceAccount()); uuid(out, record.targetAccount()); out.writeLong(record.amount()); out.writeUTF(record.memo()); out.writeLong(record.createdAt().toEpochMilli()); out.writeByte(record.status().ordinal()); out.writeUTF(record.result());
        }
    }
    @Override public EconomyData read(DataInput in, int ignoredVersion) throws IOException {
        EconomyData data = new EconomyData(); int accounts = bounded(in.readInt(), MAX_ACCOUNTS, "account count");
        for (int i = 0; i < accounts; i++) { UUID id = uuid(in); long balance = in.readLong(); if (balance < 0) throw new IOException("Negative stored balance"); data.balances.put(id, balance); }
        int transactions = bounded(in.readInt(), MAX_TRANSACTIONS, "transaction count");
        for (int i = 0; i < transactions; i++) {
            UUID id = uuid(in); UUID source = uuid(in); UUID target = uuid(in); long amount = in.readLong(); String memo = in.readUTF(); Instant time = Instant.ofEpochMilli(in.readLong()); int status = in.readByte();
            if (status < 0 || status >= TransactionStatus.values().length) throw new IOException("Invalid transaction status"); data.transactions.put(id, new TransactionRecord(id, source, target, amount, memo, time, TransactionStatus.values()[status], in.readUTF()));
        }
        return data;
    }
    private static void uuid(DataOutput out, UUID value) throws IOException { out.writeLong(value.getMostSignificantBits()); out.writeLong(value.getLeastSignificantBits()); }
    private static UUID uuid(DataInput in) throws IOException { return new UUID(in.readLong(), in.readLong()); }
    private static int bounded(int value, int maximum, String field) throws IOException { if (value < 0 || value > maximum) throw new IOException("Invalid " + field); return value; }
}
