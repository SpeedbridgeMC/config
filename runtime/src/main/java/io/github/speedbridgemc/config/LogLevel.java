package io.github.speedbridgemc.config;

import org.jetbrains.annotations.NotNull;

/**
 * Defines message log levels.
 */
public enum LogLevel {
    /**
     * Messages which can and should be ignored.
     */
    TRACE(0),
    /**
     * Messages that are useful for debugging, but can be ignored otherwise.
     */
    DEBUG(100),
    /**
     * Messages that contain useful information.
     */
    INFO(200),
    /**
     * Messages that contain warnings about imminent dangers.
     */
    WARN(300),
    /**
     * Messages that contain error information. Things will generally go wrong after seeing one of these!
     */
    ERROR(400),
    /**
     * Messages that contain fatal error information. This will most likely be the last thing you see
     * before the application crashes.
     */
    FATAL(500);

    private final int level;

    LogLevel(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

    public boolean isSameLevelOrHigher(@NotNull LogLevel other) {
        return other.level >= level;
    }
}
