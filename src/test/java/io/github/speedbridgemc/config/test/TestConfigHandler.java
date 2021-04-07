package io.github.speedbridgemc.config.test;

import io.github.speedbridgemc.config.LogLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

public interface TestConfigHandler {
    TestConfigHandler INSTANCE = new TestConfigHandlerImpl();

    @NotNull TestConfig get();
    void set(@NotNull TestConfig config);
    void reset();
    void load();
    void save();
    void addListener(@NotNull Consumer<TestConfig> listener);
    void removeListener(@NotNull Consumer<TestConfig> listener);
    void notifyChanged(@NotNull TestConfig config);
    void stopWatching();
    void startWatching();
    void setRemote(@Nullable TestConfig remoteConfig);

    default void log(@NotNull LogLevel level, @NotNull String msg, @Nullable Exception e) {
        PrintStream out = System.out;
        if (LogLevel.ERROR.isSameLevelOrHigher(level))
            out = System.err;
        out.println("[" + level + "/" + Thread.currentThread().getName() + "] " + msg);
        if (e != null)
            e.printStackTrace(out);
    }

    default @NotNull Path resolvePath(@NotNull String name) {
        return Paths.get("test", name + ".json5").normalize().toAbsolutePath();
    }

    default void runOnMainThread(@NotNull Runnable command) {
        Test.executor.execute(command);
    }

    default @NotNull TestConfig postLoad(@NotNull TestConfig config) {
        log(LogLevel.INFO, "Config loaded!", null);
        return config;
    }
    default void postSave(@NotNull TestConfig config) {
        log(LogLevel.INFO, "Config saved!", null);
    }
}
