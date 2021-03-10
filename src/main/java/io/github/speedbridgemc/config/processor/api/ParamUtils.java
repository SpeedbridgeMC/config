package io.github.speedbridgemc.config.processor.api;

import com.google.common.collect.Multimap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public final class ParamUtils {
    private ParamUtils() { }

    public static @Nullable String allOrNothing(@NotNull Multimap<String, String> params, @NotNull String key) {
        Collection<String> c = params.get(key);
        if (c.isEmpty())
            return null;
        return String.join(",", c);
    }
}
