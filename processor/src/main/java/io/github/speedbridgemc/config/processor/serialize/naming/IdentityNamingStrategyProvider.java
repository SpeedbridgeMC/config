package io.github.speedbridgemc.config.processor.serialize.naming;

import com.google.auto.service.AutoService;
import io.github.speedbridgemc.config.processor.serialize.api.BaseNamingStrategyProvider;
import io.github.speedbridgemc.config.processor.serialize.api.NamingStrategyProvider;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.VariableElement;

@AutoService(NamingStrategyProvider.class)
public final class IdentityNamingStrategyProvider extends BaseNamingStrategyProvider {
    public IdentityNamingStrategyProvider() {
        super("speedbridge-config:identity");
    }

    @Override
    public @NotNull String translate(@NotNull String variant, @NotNull VariableElement field) {
        return field.getSimpleName().toString();
    }
}
