package io.github.speedbridgemc.config;

/**
 * Defines message log levels.
 */
public enum LogLevel implements Comparable<LogLevel> {
    /**
     * Messages which can and should be ignored.
     */
    TRACE,
    /**
     * Messages that are useful for debugging, but can be ignored otherwise.
     */
    DEBUG,
    /**
     * Messages that contain useful information.
     */
    INFO,
    /**
     * Messages that contain warnings about imminent dangers.
     */
    WARN,
    /**
     * Messages that contain error information. Things will generally go wrong after seeing one of these!
     */
    ERROR,
    /**
     * Messages that contain fatal error information. This will most likely be the last thing you see
     * before the application crashes.
     */
    FATAL
}
