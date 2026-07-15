package org.yeyao.cornerstone.storage;

import java.io.*;
import java.nio.file.*;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.CRC32;

/** Checksummed, versioned file storage with temp-file writes and last-good-file recovery. */
public final class AtomicFileStorage<T> {
    private static final int MAGIC = 0x43534442; // CSDB
    private static final int MAX_PAYLOAD_BYTES = 16 * 1024 * 1024;
    private final Path file;
    private final int currentVersion;
    private final DataSerializer<T> serializer;
    private final Map<Integer, DataMigration<T>> migrations = new TreeMap<>();

    public AtomicFileStorage(Path file, int currentVersion, DataSerializer<T> serializer) {
        this.file = file; this.currentVersion = currentVersion; this.serializer = serializer;
    }
    public AtomicFileStorage<T> addMigration(int sourceVersion, DataMigration<T> migration) {
        migrations.put(sourceVersion, migration); return this;
    }
    public synchronized void save(T value) {
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        Path backup = file.resolveSibling(file.getFileName() + ".bak");
        try {
            Files.createDirectories(file.getParent());
            byte[] payload;
            try (ByteArrayOutputStream bytes = new ByteArrayOutputStream(); DataOutputStream output = new DataOutputStream(bytes)) {
                serializer.write(output, value); output.flush(); payload = bytes.toByteArray();
            }
            CRC32 crc = new CRC32(); crc.update(payload);
            try (DataOutputStream output = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(temporary, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)))) {
                output.writeInt(MAGIC); output.writeInt(currentVersion); output.writeInt(payload.length); output.writeLong(crc.getValue()); output.write(payload);
            }
            if (Files.exists(file)) Files.move(file, backup, StandardCopyOption.REPLACE_EXISTING);
            try { Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException ignored) { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING); }
        } catch (IOException exception) { throw new StorageException("Could not atomically save " + file, exception); }
    }
    public synchronized T loadOrDefault(T fallback) {
        if (!Files.exists(file)) return fallback;
        try { return read(file); }
        catch (Exception primary) {
            Path backup = file.resolveSibling(file.getFileName() + ".bak");
            if (!Files.exists(backup)) throw new StorageException("Could not load " + file, primary);
            try { return read(backup); }
            catch (Exception recovered) { recovered.addSuppressed(primary); throw new StorageException("Could not recover " + file, recovered); }
        }
    }
    private T read(Path source) throws Exception {
        try (DataInputStream input = new DataInputStream(new BufferedInputStream(Files.newInputStream(source)))) {
            if (input.readInt() != MAGIC) throw new IOException("Unrecognized data header");
            int version = input.readInt(); int length = input.readInt(); long checksum = input.readLong();
            if (length < 0 || length > MAX_PAYLOAD_BYTES) throw new IOException("Invalid payload length " + length);
            byte[] payload = input.readNBytes(length);
            if (payload.length != length) throw new EOFException("Unexpected end of payload");
            CRC32 crc = new CRC32(); crc.update(payload);
            if (crc.getValue() != checksum) throw new IOException("Payload checksum mismatch");
            T value;
            try (DataInputStream payloadInput = new DataInputStream(new ByteArrayInputStream(payload))) { value = serializer.read(payloadInput, version); }
            while (version < currentVersion) {
                DataMigration<T> migration = migrations.get(version);
                if (migration == null) throw new StorageException("No migration from data version " + version);
                value = migration.migrate(value); version++;
            }
            if (version != currentVersion) throw new StorageException("Data version " + version + " is newer than supported version " + currentVersion);
            return value;
        }
    }
}
