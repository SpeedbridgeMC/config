package io.github.speedbridgemc.config.test;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class Identifier {
    private static final @NotNull String DEFAULT_NAMESPACE = "speedbrige-config";
    private final @NotNull String path, namespace;

    public @Nullable Identifier tryParse(@NotNull String value) {
        String[] split = value.split(":");
        if (split.length == 1)
            return new Identifier(DEFAULT_NAMESPACE, split[0]);
        else if (split.length == 2)
            return new Identifier(split[0], split[1]);
        return null;
    }

    public Identifier(@NotNull String path, @NotNull String namespace) {
        this.path = path;
        this.namespace = namespace;
    }

    public @NotNull String getPath() {
        return path;
    }

    public @NotNull String getNamespace() {
        return namespace;
    }

    @Override
    public String toString() {
        return path + ':' + namespace;
    }
}
