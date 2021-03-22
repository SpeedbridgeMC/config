package io.github.speedbridgemc.config.processor.serialize.naming;

import com.google.auto.service.AutoService;
import io.github.speedbridgemc.config.processor.api.StringUtils;
import io.github.speedbridgemc.config.processor.serialize.api.BaseNamingStrategyProvider;
import io.github.speedbridgemc.config.processor.serialize.api.NamingStrategyProvider;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.VariableElement;
import java.util.Locale;

@AutoService(NamingStrategyProvider.class)
public final class SnakeCaseNamingStrategyProvider extends BaseNamingStrategyProvider {
    public SnakeCaseNamingStrategyProvider() {
        super("speedbridge-config:snake_case");
    }

    @Override
    public @NotNull String translate(@NotNull String variant, @NotNull VariableElement field) {
        String n = StringUtils.camelCaseToSnakeCase(field.getSimpleName().toString());
        if ("screaming".equalsIgnoreCase(variant))
            n = n.toUpperCase(Locale.ROOT);
        return n;
    }
}
