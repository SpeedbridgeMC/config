package io.github.speedbridgemc.config.processor.api.type;

import io.github.speedbridgemc.config.Config;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.DeclaredType;
import java.util.Optional;

public interface StructFactory {
    void init(@NotNull ProcessingEnvironment processingEnv, @NotNull ConfigTypeProvider typeProvider);
    @NotNull Optional<ConfigType> createStruct(@NotNull DeclaredType mirror, @Nullable Config.StructOverride structOverride);
}
