package org.yeyao.cornerstone.api;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/** Small Java event bus for integration code; listeners never receive mutable storage objects. */
public final class CornerstoneEventBus<E> {
    private final List<Consumer<E>> listeners = new CopyOnWriteArrayList<>();

    public AutoCloseable subscribe(Consumer<E> listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    public void publish(E event) {
        for (Consumer<E> listener : listeners) listener.accept(event);
    }
}
