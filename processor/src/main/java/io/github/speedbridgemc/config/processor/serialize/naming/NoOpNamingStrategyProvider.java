package io.github.speedbridgemc.config.processor.serialize.naming;

import com.google.auto.service.AutoService;
import io.github.speedbridgemc.config.processor.serialize.api.BaseNamingStrategyProvider;
import io.github.speedbridgemc.config.processor.serialize.api.NamingStrategyProvider;
import org.jetbrains.annotations.NotNull;

@AutoService(NamingStrategyProvider.class)
public final class NoOpNamingStrategyProvider extends BaseNamingStrategyProvider {
    public NoOpNamingStrategyProvider() {
        super("speedbridge-config:noop");
    }

    @Override
    public @NotNull String translate(@NotNull String name) {
        return name;
    }
}
