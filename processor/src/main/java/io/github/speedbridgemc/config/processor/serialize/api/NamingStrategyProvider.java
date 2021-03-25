package io.github.speedbridgemc.config.processor.serialize.api;

import io.github.speedbridgemc.config.processor.api.IdentifiedProvider;
import io.github.speedbridgemc.config.processor.api.Provider;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.VariableElement;

public interface NamingStrategyProvider extends IdentifiedProvider {
    @NotNull String translate(@NotNull String variant, @NotNull VariableElement field);
}
