package io.github.speedbridgemc.config.test;

import io.github.speedbridgemc.config.LogLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;

public interface TestConfigHandler {
    TestConfigHandler INSTANCE = new TestConfigHandlerImpl();

    @NotNull TestConfig get();
    void reset();
    void load();
    void save();
    void setRemote(@Nullable TestConfig remoteConfig);

    default void log(@NotNull LogLevel level, @NotNull String msg, @Nullable Exception e) {
        PrintStream out = System.out;
        if (LogLevel.ERROR.isSameLevelOrHigher(level))
            out = System.err;
        out.println(msg);
        if (e != null)
            e.printStackTrace(out);
    }

    default @NotNull Path resolvePath(@NotNull String name) {
        return Paths.get(".", name + ".json").normalize();
    }

    default @NotNull TestConfig postLoad(@NotNull TestConfig config) {
        return config;
    }
    default void postSave(@NotNull TestConfig config) {

    }
}
