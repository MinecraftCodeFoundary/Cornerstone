package org.yeyao.cornerstone.data;

import org.yeyao.cornerstone.storage.DataSerializer;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.UUID;

/** M1 schema codec. The enclosing storage owns file versioning and recovery. */
final class PlayerDataStorage implements DataSerializer<Collection<PlayerProfile>> {
    static final PlayerDataStorage INSTANCE = new PlayerDataStorage();
    private static final int MAX_PROFILES = 100_000;
    private static final int MAX_MODULES_PER_PROFILE = 256;
    private PlayerDataStorage() { }
    @Override public void write(DataOutput out, Collection<PlayerProfile> profiles) throws IOException {
        out.writeInt(profiles.size());
        for (PlayerProfile profile : profiles) {
            out.writeLong(profile.id.getMostSignificantBits()); out.writeLong(profile.id.getLeastSignificantBits());
            out.writeUTF(profile.lastKnownName); out.writeLong(profile.lastOnline.toEpochMilli());
            out.writeBoolean(profile.lastLocation != null);
            if (profile.lastLocation != null) writeLocation(out, profile.lastLocation);
            out.writeInt(profile.modules.size());
            for (var module : profile.modules.entrySet()) {
                out.writeUTF(module.getKey()); out.writeInt(module.getValue().size());
                for (var entry : module.getValue().entrySet()) { out.writeUTF(entry.getKey()); out.writeUTF(entry.getValue()); }
            }
        }
    }
    @Override public Collection<PlayerProfile> read(DataInput in, int ignoredVersion) throws IOException {
        int count = bounded(in.readInt(), MAX_PROFILES, "profile count"); var profiles = new ArrayList<PlayerProfile>(count);
        for (int i = 0; i < count; i++) {
            UUID id = new UUID(in.readLong(), in.readLong()); PlayerProfile profile = new PlayerProfile(id, in.readUTF());
            profile.lastOnline = Instant.ofEpochMilli(in.readLong()); if (in.readBoolean()) profile.lastLocation = readLocation(in);
            int modules = bounded(in.readInt(), MAX_MODULES_PER_PROFILE, "module count");
            for (int m = 0; m < modules; m++) {
                String module = in.readUTF(); int entries = bounded(in.readInt(), 1_024, "module entry count"); var values = new LinkedHashMap<String, String>();
                for (int e = 0; e < entries; e++) values.put(in.readUTF(), in.readUTF());
                profile.modules.put(module, values);
            }
            profiles.add(profile);
        }
        return profiles;
    }
    private static void writeLocation(DataOutput out, StoredLocation value) throws IOException { out.writeUTF(value.dimension()); out.writeDouble(value.x()); out.writeDouble(value.y()); out.writeDouble(value.z()); out.writeFloat(value.yaw()); out.writeFloat(value.pitch()); }
    private static StoredLocation readLocation(DataInput in) throws IOException { return new StoredLocation(in.readUTF(), in.readDouble(), in.readDouble(), in.readDouble(), in.readFloat(), in.readFloat()); }
    private static int bounded(int value, int maximum, String field) throws IOException { if (value < 0 || value > maximum) throw new IOException("Invalid " + field); return value; }
}
