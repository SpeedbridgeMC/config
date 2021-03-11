package io.github.speedbridgemc.config.test;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.nio.file.Paths;

public interface TestConfigHandler {
    TestConfigHandler INSTANCE = new TestConfigHandlerImpl();

    static @NotNull TestConfigHandler instance() {
        return INSTANCE;
    }

    @NotNull TestConfig get();
    void save();
    void setRemote(@Nullable TestConfig remoteConfig);

    default void log(@NotNull String message, @Nullable Exception e) {
        System.err.println(message);
        if (e != null)
            e.printStackTrace();
    }

    default @NotNull Path resolvePath(@NotNull String name) {
        return Paths.get(".", name + ".json5");
    }
}
