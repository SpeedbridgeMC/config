package io.github.speedbridgemc.config.processor.api.ext;

import io.github.speedbridgemc.config.processor.api.ConfigValueExtension;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Set;

import static io.github.speedbridgemc.config.processor.api.util.CollectionUtils.toImmutableSet;

public final class SerializeExtension implements ConfigValueExtension {
    private final @NotNull Set<String> aliases;

    public SerializeExtension(@NotNull Set<String> aliases) {
        this.aliases = toImmutableSet(aliases);
    }

    @Override
    public @NotNull String id() {
        return "speedbridge-config:serialization";
    }

    public @NotNull Collection<? extends String> aliases() {
        return aliases;
    }

    @Override
    public String toString() {
        return "SerializeExtension{" +
                "aliases=" + aliases +
                '}';
    }
}
