package org.yeyao.cornerstone.operations;

import org.yeyao.cornerstone.storage.DataSerializer;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.UUID;

final class OperationsDataStorage implements DataSerializer<OperationsData> {
    static final OperationsDataStorage INSTANCE = new OperationsDataStorage();
    private static final int MAX_ALLOWLIST = 100_000;
    private OperationsDataStorage() { }
    @Override public void write(DataOutput out, OperationsData data) throws IOException {
        out.writeBoolean(data.maintenanceEnabled); out.writeInt(data.maintenanceAllowlist.size());
        for (UUID id : data.maintenanceAllowlist) { out.writeLong(id.getMostSignificantBits()); out.writeLong(id.getLeastSignificantBits()); }
    }
    @Override public OperationsData read(DataInput in, int ignoredVersion) throws IOException {
        OperationsData data = new OperationsData(); data.maintenanceEnabled = in.readBoolean(); int count = in.readInt();
        if (count < 0 || count > MAX_ALLOWLIST) throw new IOException("Invalid maintenance allowlist size");
        for (int i = 0; i < count; i++) data.maintenanceAllowlist.add(new UUID(in.readLong(), in.readLong()));
        return data;
    }
}
