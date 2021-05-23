package io.github.speedbridgemc.config.processor.api.util;

import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.ImmutableSet;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public final class CollectionUtils {
    private CollectionUtils() { }

    public static <T> @NotNull ImmutableClassToInstanceMap<T> toImmutableClassToInstanceMap(@NotNull ClassToInstanceMap<T> map) {
        if (map instanceof ImmutableClassToInstanceMap)
            return (ImmutableClassToInstanceMap<T>) map;
        return ImmutableClassToInstanceMap.copyOf(map);
    }

    public static <T> @NotNull ImmutableSet<T> toImmutableSet(@NotNull Set<T> set) {
        if (set instanceof ImmutableSet)
            return (ImmutableSet<T>) set;
        return ImmutableSet.copyOf(set);
    }
}
