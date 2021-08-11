package io.github.speedbridgemc.config.test;

import org.jetbrains.annotations.Nullable;

public final class Identifier {
    private static final String DEFAULT_NAMESPACE = "speedbrige-config";
    private final String path, namespace;

    public @Nullable Identifier tryParse(String string) {
        String[] split = string.split(":");
        if (split.length == 1)
            return new Identifier(DEFAULT_NAMESPACE, split[0]);
        else if (split.length == 2)
            return new Identifier(split[0], split[1]);
        return null;
    }

    public Identifier(String path, String namespace) {
        this.path = path;
        this.namespace = namespace;
    }

    public String getPath() {
        return path;
    }

    public String getNamespace() {
        return namespace;
    }

    @Override
    public String toString() {
        return path + ':' + namespace;
    }
}
