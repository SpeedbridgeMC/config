package io.github.speedbridgemc.config.processor.api.property;

import com.google.common.collect.ImmutableSet;
import io.github.speedbridgemc.config.processor.impl.ConfigProcessor;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import static io.github.speedbridgemc.config.processor.api.util.CollectionUtils.toImmutableSet;

public final class SerializeExtension extends BaseConfigPropertyExtension {
    private final @Nullable String serializedName;
    private final ImmutableSet<String> aliases;

    public SerializeExtension(@Nullable String serializedName, Set<String> aliases) {
        super(ConfigProcessor.id("serialize"));
        this.serializedName = serializedName;
        this.aliases = toImmutableSet(aliases);
    }

    public Optional<String> serializedName() {
        return Optional.ofNullable(serializedName);
    }

    public Collection<? extends String> aliases() {
        return aliases;
    }
}
