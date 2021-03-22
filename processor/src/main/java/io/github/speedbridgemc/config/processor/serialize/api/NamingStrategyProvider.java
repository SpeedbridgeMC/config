package io.github.speedbridgemc.config.processor.serialize.api;

import io.github.speedbridgemc.config.processor.api.Provider;
import org.jetbrains.annotations.NotNull;

public interface NamingStrategyProvider extends Provider {
    @NotNull String translate(@NotNull String name);
}
