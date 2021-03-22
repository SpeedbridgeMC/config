package io.github.speedbridgemc.config.processor.serialize.naming;

import io.github.speedbridgemc.config.processor.api.StringUtils;
import io.github.speedbridgemc.config.processor.serialize.api.BaseNamingStrategyProvider;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.VariableElement;
import java.util.Locale;

public final class CamelCaseNamingStrategyProvider extends BaseNamingStrategyProvider {
    public CamelCaseNamingStrategyProvider() {
        super("speedbridge-config:camel_case");
    }

    @Override
    public @NotNull String translate(@NotNull String variant, @NotNull VariableElement field) {
        String s = StringUtils.snakeCaseToCamelCase(field.getSimpleName().toString());
        if ("upper".equalsIgnoreCase(variant))
            s = s.toUpperCase(Locale.ROOT);
        return s;
    }

}
