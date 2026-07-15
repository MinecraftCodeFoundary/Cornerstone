package org.yeyao.cornerstone.storage;

/** Converts a decoded value from one schema version to the next. */
@FunctionalInterface
public interface DataMigration<T> {
    T migrate(T value) throws Exception;
}
