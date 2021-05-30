package io.github.speedbridgemc.config.processor.api.property;

import com.google.common.collect.ImmutableSet;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Set;

import static io.github.speedbridgemc.config.processor.api.util.CollectionUtils.toImmutableSet;

public final class SerializeExtension implements ConfigPropertyExtension {
    private final @NotNull ImmutableSet<String> aliases;

    public SerializeExtension(@NotNull Set<String> aliases) {
        this.aliases = toImmutableSet(aliases);
    }

    @Override
    public @NotNull String id() {
        return "speedbridge-config:serialize";
    }

    public @NotNull Collection<? extends String> aliases() {
        return aliases;
    }
}
