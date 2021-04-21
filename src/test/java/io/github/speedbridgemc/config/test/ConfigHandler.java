package io.github.speedbridgemc.config.test;

import io.github.speedbridgemc.config.LogLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

public interface ConfigHandler<T> {
    ConfigHandler<TestConfig> TEST_CONFIG = new TestConfigHandlerImpl();

    @NotNull T get();
    void set(@NotNull T config);
    void reset();
    void load();
    void save();
    void addListener(@NotNull Consumer<T> listener);
    void removeListener(@NotNull Consumer<T> listener);
    void notifyChanged(@NotNull T config);
    void stopWatching();
    void startWatching();
    void setRemote(@Nullable T remoteConfig);

    default void log(@NotNull LogLevel level, @NotNull String msg, @Nullable Exception e) {
        PrintStream out = System.out;
        if (LogLevel.ERROR.isSameLevelOrHigher(level))
            out = System.err;
        out.println("[" + level + "/" + Thread.currentThread().getName() + "] " + msg);
        if (e != null)
            e.printStackTrace(out);
    }

    default @NotNull Path resolvePath(@NotNull String name) {
        return Paths.get("test_config", name + ".json5").normalize().toAbsolutePath();
    }

    default void runOnMainThread(@NotNull Runnable command) {
        Test.executor.execute(command);
    }

    default @NotNull T postLoad(@NotNull T config) {
        log(LogLevel.INFO, "Config loaded!", null);
        return config;
    }
    default void postSave(@NotNull T config) {
        log(LogLevel.INFO, "Config saved!", null);
    }
}
