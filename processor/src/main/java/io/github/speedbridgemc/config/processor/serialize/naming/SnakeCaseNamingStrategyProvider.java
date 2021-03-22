package io.github.speedbridgemc.config.processor.serialize.naming;

import com.google.auto.service.AutoService;
import io.github.speedbridgemc.config.processor.api.StringUtils;
import io.github.speedbridgemc.config.processor.serialize.api.BaseNamingStrategyProvider;
import io.github.speedbridgemc.config.processor.serialize.api.NamingStrategyProvider;
import org.jetbrains.annotations.NotNull;

@AutoService(NamingStrategyProvider.class)
public final class SnakeCaseNamingStrategyProvider extends BaseNamingStrategyProvider {
    public SnakeCaseNamingStrategyProvider() {
        super("speedbridge-config:snake_case");
    }

    @Override
    public @NotNull String translate(@NotNull String name) {
        return StringUtils.camelCaseToSnakeCase(name);
    }
}
