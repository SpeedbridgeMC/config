package io.github.speedbridgemc.config.processor.api;

import org.jetbrains.annotations.NotNull;

public interface IdentifiedProvider extends Provider {
    @NotNull String getId();
}
