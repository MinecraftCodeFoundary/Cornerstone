package org.yeyao.cornerstone.storage;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/** Payload codec used by versioned atomic storage. */
public interface DataSerializer<T> {
    void write(DataOutput output, T value) throws IOException;
    T read(DataInput input, int version) throws IOException;
}
