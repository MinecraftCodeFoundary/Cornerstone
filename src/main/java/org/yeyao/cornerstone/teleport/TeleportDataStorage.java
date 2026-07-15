package org.yeyao.cornerstone.teleport;

import org.yeyao.cornerstone.data.StoredLocation;
import org.yeyao.cornerstone.storage.DataSerializer;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Version-one binary codec for spawn, home and back locations. */
final class TeleportDataStorage implements DataSerializer<TeleportData> {
    static final TeleportDataStorage INSTANCE = new TeleportDataStorage();
    private static final int MAX_ENTRIES = 100_000;
    private TeleportDataStorage() { }
    @Override public void write(DataOutput out, TeleportData data) throws IOException {
        out.writeBoolean(data.globalSpawn != null); if (data.globalSpawn != null) writeLocation(out, data.globalSpawn);
        writeLocationMap(out, data.dimensionSpawns);
        out.writeInt(data.homes.size());
        for (var playerHomes : data.homes.entrySet()) {
            writeUuid(out, playerHomes.getKey()); writeLocationMap(out, playerHomes.getValue());
        }
        out.writeInt(data.backLocations.size());
        for (var entry : data.backLocations.entrySet()) { writeUuid(out, entry.getKey()); writeLocation(out, entry.getValue()); }
        out.writeInt(data.warps.size());
        for (Warp warp : data.warps.values()) { out.writeUTF(warp.name()); out.writeByte(warp.access().ordinal()); writeLocation(out, warp.location()); }
    }
    @Override public TeleportData read(DataInput in, int version) throws IOException {
        TeleportData data = new TeleportData();
        if (in.readBoolean()) data.globalSpawn = readLocation(in);
        data.dimensionSpawns.putAll(readLocationMap(in));
        int homePlayers = bounded(in.readInt());
        for (int i = 0; i < homePlayers; i++) data.homes.put(readUuid(in), readLocationMap(in));
        int backs = bounded(in.readInt());
        for (int i = 0; i < backs; i++) data.backLocations.put(readUuid(in), readLocation(in));
        if (version >= 2) {
            int warps = bounded(in.readInt());
            for (int i = 0; i < warps; i++) {
                String name = in.readUTF(); int ordinal = in.readByte();
                if (ordinal < 0 || ordinal >= Warp.Access.values().length) throw new IOException("Invalid warp access type");
                data.warps.put(name, new Warp(name, readLocation(in), Warp.Access.values()[ordinal]));
            }
        }
        return data;
    }
    private static void writeLocationMap(DataOutput out, Map<String, StoredLocation> values) throws IOException {
        out.writeInt(values.size()); for (var entry : values.entrySet()) { out.writeUTF(entry.getKey()); writeLocation(out, entry.getValue()); }
    }
    private static Map<String, StoredLocation> readLocationMap(DataInput in) throws IOException {
        int count = bounded(in.readInt()); Map<String, StoredLocation> result = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) result.put(in.readUTF(), readLocation(in)); return result;
    }
    private static void writeUuid(DataOutput out, UUID id) throws IOException { out.writeLong(id.getMostSignificantBits()); out.writeLong(id.getLeastSignificantBits()); }
    private static UUID readUuid(DataInput in) throws IOException { return new UUID(in.readLong(), in.readLong()); }
    private static void writeLocation(DataOutput out, StoredLocation value) throws IOException { out.writeUTF(value.dimension()); out.writeDouble(value.x()); out.writeDouble(value.y()); out.writeDouble(value.z()); out.writeFloat(value.yaw()); out.writeFloat(value.pitch()); }
    private static StoredLocation readLocation(DataInput in) throws IOException { return new StoredLocation(in.readUTF(), in.readDouble(), in.readDouble(), in.readDouble(), in.readFloat(), in.readFloat()); }
    private static int bounded(int value) throws IOException { if (value < 0 || value > MAX_ENTRIES) throw new IOException("Invalid teleport data entry count"); return value; }
}
