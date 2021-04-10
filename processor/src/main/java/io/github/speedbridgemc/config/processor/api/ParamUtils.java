package io.github.speedbridgemc.config.processor.api;

import com.google.common.collect.Multimap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Utilities for working with {@linkplain ComponentContext#params component parameters}.
 */
public final class ParamUtils {
    private ParamUtils() { }

    /**
     * Gets the unspilt value of a parameter, or {@code null} if the parameter wasn't specified.
     * @param params parameter multimap
     * @param key parameter key
     * @return unsplit parameter value, or {@code null} if parameter wasn't specified
     */
    public static @Nullable String allOrNothing(@NotNull Multimap<String, String> params, @NotNull String key) {
        Collection<String> c = params.get(key);
        if (c.isEmpty())
            return null;
        return String.join(",", c);
    }
}
