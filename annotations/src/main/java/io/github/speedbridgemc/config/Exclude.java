package io.github.speedbridgemc.config;

import java.lang.annotation.*;

/**
 * Excludes a (normally qualifying) field from being processed entirely.
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface Exclude { }
