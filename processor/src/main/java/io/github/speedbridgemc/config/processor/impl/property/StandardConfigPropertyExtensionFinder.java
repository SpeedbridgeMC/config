package io.github.speedbridgemc.config.processor.impl.property;

import com.google.common.collect.ImmutableSet;
import io.github.speedbridgemc.config.processor.api.property.BaseConfigPropertyExtensionFinder;
import io.github.speedbridgemc.config.processor.api.property.SerializeExtension;
import io.github.speedbridgemc.config.processor.api.util.MirrorElementPair;
import io.github.speedbridgemc.config.serialize.Aliases;
import io.github.speedbridgemc.config.serialize.SerializedName;
import org.jetbrains.annotations.NotNull;

public final class StandardConfigPropertyExtensionFinder extends BaseConfigPropertyExtensionFinder {
    @Override
    public void findExtensions(@NotNull Callback callback, @NotNull MirrorElementPair @NotNull ... pairs) {
        extSerialize(callback, pairs);
    }

    private void extSerialize(@NotNull Callback callback, @NotNull MirrorElementPair @NotNull ... pairs) {
        String serializedName = null;
        final ImmutableSet.Builder<String> aliasBuilder = ImmutableSet.builder();
        for (MirrorElementPair pair : pairs) {
            SerializedName serializedName1 = pair.element().getAnnotation(SerializedName.class);
            if (serializedName1 != null)
                serializedName = serializedName1.value();
            Aliases aliases = pair.element().getAnnotation(Aliases.class);
            if (aliases == null)
                continue;
            for (String alias : aliases.value())
                aliasBuilder.add(alias);
        }
        callback.putExtension(SerializeExtension.class, new SerializeExtension(serializedName, aliasBuilder.build()));
    }
}
