package org.yeyao.cornerstone.audit;

import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

/** Append-only operational audit log with a bounded in-memory query window. */
public final class AuditService {
    private static final int MEMORY_LIMIT = 2_000;
    private final Deque<AuditRecord> records = new ArrayDeque<>();
    private Path file;
    private Logger logger;
    public synchronized void open(Path directory, Logger value) {
        file = directory.resolve("audit.log"); logger = value;
        try { Files.createDirectories(directory); } catch (IOException exception) { throw new IllegalStateException("Could not create audit directory", exception); }
    }
    public synchronized void record(AuditRecord record) {
        records.addLast(record); while (records.size() > MEMORY_LIMIT) records.removeFirst();
        if (file == null) return;
        String line = "%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n".formatted(record.time(), clean(record.action()), clean(record.actor()), clean(record.target()), clean(record.dimension()), clean(record.arguments()), record.success(), clean(record.result()));
        try { Files.writeString(file, line, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND); }
        catch (IOException exception) { logger.warn("Could not write Cornerstone audit record", exception); }
    }
    public synchronized List<AuditRecord> recent(int maximum) { return records.stream().skip(Math.max(0, records.size() - Math.max(0, maximum))).toList(); }
    private static String clean(String value) { return value == null ? "" : value.replace('\t', ' ').replace('\n', ' ').replace('\r', ' '); }
}
