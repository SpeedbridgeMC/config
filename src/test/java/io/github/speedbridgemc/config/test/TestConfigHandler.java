package io.github.speedbridgemc.config.test;

import io.github.speedbridgemc.config.LogLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public interface TestConfigHandler extends ConfigHandler {
    TestConfigHandler INSTANCE = new TestConfigHandlerImpl();

    @NotNull TestConfig get();
    void set(@NotNull TestConfig config);
    void addListener(@NotNull Consumer<TestConfig> listener);
    void removeListener(@NotNull Consumer<TestConfig> listener);
    void notifyChanged(@NotNull TestConfig config);
    void setRemote(@Nullable TestConfig remoteConfig);

    default @NotNull TestConfig postLoad(@NotNull TestConfig config) {
        log(LogLevel.INFO, "Config loaded!", null);
        return config;
    }
    default void postSave(@NotNull TestConfig config) {
        log(LogLevel.INFO, "Config saved!", null);
    }
}
