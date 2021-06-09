package io.github.speedbridgemc.config.processor.api.type;

import io.github.speedbridgemc.config.Config;
import io.github.speedbridgemc.config.processor.api.property.ConfigPropertyExtension;
import io.github.speedbridgemc.config.processor.api.property.ConfigPropertyExtensionFinder;
import io.github.speedbridgemc.config.processor.api.util.MirrorElementPair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.DeclaredType;
import java.util.Optional;

public interface StructFactory {
    interface Context {
        /**
         * Returns a {@link ConfigTypeProvider}.
         * @return type provider
         */
        @NotNull ConfigTypeProvider typeProvider();

        /**
         * Converts a naming using the current naming strategy.
         * @param originalName original name
         * @return converted name
         */
        @NotNull String name(@NotNull String originalName);

        /**
         * Finds {@link ConfigPropertyExtension}s using the specified mirror-element pairs.
         * @param callback callback
         * @param pairs mirror-element pairs
         */
        void findExtensions(@NotNull ConfigPropertyExtensionFinder.Callback callback,
                            @NotNull MirrorElementPair @NotNull ... pairs);
    }

    void init(@NotNull ProcessingEnvironment processingEnv);
    @NotNull Optional<ConfigType> createStruct(@NotNull Context ctx,
                                               @NotNull DeclaredType mirror, @Nullable Config.StructOverride structOverride);
}
