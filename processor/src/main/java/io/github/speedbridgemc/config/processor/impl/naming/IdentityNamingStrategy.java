package io.github.speedbridgemc.config.processor.impl.naming;

import com.google.auto.service.AutoService;
import io.github.speedbridgemc.config.processor.api.naming.BaseNamingStrategy;
import io.github.speedbridgemc.config.processor.api.naming.NamingStrategy;
import io.github.speedbridgemc.config.processor.impl.ConfigProcessor;
import org.jetbrains.annotations.NotNull;

@AutoService(NamingStrategy.class)
public final class IdentityNamingStrategy extends BaseNamingStrategy {
    public IdentityNamingStrategy() {
        super(ConfigProcessor.id("identity"));
    }

    @Override
    public @NotNull String name(@NotNull String variant, @NotNull String originalName) {
        return originalName;
    }
}
