package io.github.speedbridgemc.config.serialize;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
public @interface Aliases {
    String[] value();
}
