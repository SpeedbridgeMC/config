package io.github.speedbridgemc.config.processor.impl.naming;

import com.google.auto.service.AutoService;
import io.github.speedbridgemc.config.processor.api.naming.BaseNamingStrategy;
import io.github.speedbridgemc.config.processor.api.naming.NamingStrategy;
import io.github.speedbridgemc.config.processor.api.util.StringUtils;
import io.github.speedbridgemc.config.processor.impl.ConfigProcessor;

import java.util.Locale;

@AutoService(NamingStrategy.class)
public final class SnakeCaseNamingStrategy extends BaseNamingStrategy {
    public SnakeCaseNamingStrategy() {
        super(ConfigProcessor.id("snake_case"));
    }

    @Override
    public String name(String variant, String originalName) {
        String s = StringUtils.camelCaseToSnakeCase(originalName);
        if ("screaming".equalsIgnoreCase(variant))
            s = s.toUpperCase(Locale.ROOT);
        return s;
    }
}
