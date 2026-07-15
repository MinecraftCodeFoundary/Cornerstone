package org.yeyao.cornerstone.moderation;

import org.yeyao.cornerstone.storage.DataSerializer;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Version-one codec for append-only punishment history. */
final class ModerationDataStorage implements DataSerializer<ModerationData> {
    static final ModerationDataStorage INSTANCE = new ModerationDataStorage();
    private static final int MAX_RECORDS = 1_000_000;
    private ModerationDataStorage() { }
    @Override public void write(DataOutput out, ModerationData data) throws IOException {
        out.writeInt(data.punishments.size());
        for (Punishment value : data.punishments) {
            uuid(out, value.id()); out.writeByte(value.type().ordinal()); uuid(out, value.targetId()); out.writeUTF(value.targetName());
            out.writeBoolean(value.actorId().isPresent()); if (value.actorId().isPresent()) uuid(out, value.actorId().get());
            out.writeUTF(value.actorName()); out.writeLong(value.issuedAt().toEpochMilli()); out.writeBoolean(value.expiresAt().isPresent());
            if (value.expiresAt().isPresent()) out.writeLong(value.expiresAt().get().toEpochMilli()); out.writeUTF(value.reason()); out.writeBoolean(value.revoked());
        }
    }
    @Override public ModerationData read(DataInput in, int ignoredVersion) throws IOException {
        int count = in.readInt(); if (count < 0 || count > MAX_RECORDS) throw new IOException("Invalid punishment count"); ModerationData data = new ModerationData();
        for (int i = 0; i < count; i++) {
            UUID id = uuid(in); int type = in.readByte(); if (type < 0 || type >= PunishmentType.values().length) throw new IOException("Invalid punishment type");
            UUID target = uuid(in); String targetName = in.readUTF(); Optional<UUID> actor = in.readBoolean() ? Optional.of(uuid(in)) : Optional.empty(); String actorName = in.readUTF();
            Instant issued = Instant.ofEpochMilli(in.readLong()); Optional<Instant> expires = in.readBoolean() ? Optional.of(Instant.ofEpochMilli(in.readLong())) : Optional.empty();
            data.punishments.add(new Punishment(id, PunishmentType.values()[type], target, targetName, actor, actorName, issued, expires, in.readUTF(), in.readBoolean()));
        }
        return data;
    }
    private static void uuid(DataOutput out, UUID value) throws IOException { out.writeLong(value.getMostSignificantBits()); out.writeLong(value.getLeastSignificantBits()); }
    private static UUID uuid(DataInput in) throws IOException { return new UUID(in.readLong(), in.readLong()); }
}
