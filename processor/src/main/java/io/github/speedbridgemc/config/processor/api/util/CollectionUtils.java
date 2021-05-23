package io.github.speedbridgemc.config.processor.api.util;

import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap;
import org.jetbrains.annotations.NotNull;

public final class CollectionUtils {
    private CollectionUtils() { }

    public static <T> @NotNull ImmutableClassToInstanceMap<T> toImmutableClassToInstanceMap(@NotNull ClassToInstanceMap<T> map) {
        if (map instanceof ImmutableClassToInstanceMap)
            return (ImmutableClassToInstanceMap<T>) map;
        return ImmutableClassToInstanceMap.copyOf(map);
    }
}
