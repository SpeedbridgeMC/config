package io.github.speedbridgemc.config.processor.impl.property;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableSet;
import io.github.speedbridgemc.config.processor.api.property.BaseConfigPropertyExtensionFinder;
import io.github.speedbridgemc.config.processor.api.property.ConfigPropertyExtensionFinder;
import io.github.speedbridgemc.config.processor.api.property.SerializeExtension;
import io.github.speedbridgemc.config.processor.api.util.MirrorElementPair;
import io.github.speedbridgemc.config.seralize.Aliases;
import org.jetbrains.annotations.NotNull;

@AutoService(ConfigPropertyExtensionFinder.class)
public final class StandardConfigPropertyExtensionFinder extends BaseConfigPropertyExtensionFinder {
    @Override
    public void findExtensions(@NotNull Callback callback, @NotNull MirrorElementPair @NotNull ... pairs) {
        final ImmutableSet.Builder<String> aliasBuilder = ImmutableSet.builder();
        for (MirrorElementPair pair : pairs) {
            Aliases aliases = pair.element().getAnnotation(Aliases.class);
            if (aliases == null)
                continue;
            for (String alias : aliases.value())
                aliasBuilder.add(alias);
        }
        callback.putExtension(SerializeExtension.class, new SerializeExtension(aliasBuilder.build()));
    }
}
