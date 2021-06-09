package io.github.speedbridgemc.config.processor.api.util;

import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * Represents a <em>lazy-loading</em> supplier - a supplier that only evaluates its result once and caches it for later invocations.
 * @param <T> result type
 */
@FunctionalInterface
public interface Lazy<T> extends Supplier<T> {
    @Override
    T get();

    /**
     * Wraps a standard {@link Supplier} in lazy-loading behavior.
     * @param supplier base supplier
     * @param <T> result type
     * @return wrapped supplier
     */
    static <T> @NotNull Lazy<T> wrap(@NotNull Supplier<T> supplier) {
        return new Lazy<T>() {
            private final @NotNull Supplier<T> _supplier = supplier;
            private boolean present = false;
            private T value;

            @Override
            public T get() {
                if (!present) {
                    present = true;
                    value = _supplier.get();
                }
                return value;
            }
        };
    }

    /**
     * Wraps an already-evaluated result value in a {@code Lazy}.
     * @param value value to wrap
     * @param <T> result type
     * @return wrapped value
     */
    static <T> @NotNull Lazy<T> of(@NotNull T value) {
        return new Lazy<T>() {
            private final T _value = value;

            @Override
            public T get() {
                return _value;
            }
        };
    }
}
