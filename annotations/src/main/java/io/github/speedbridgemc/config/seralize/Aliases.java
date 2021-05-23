package io.github.speedbridgemc.config.seralize;

import org.jetbrains.annotations.NotNull;

public @interface Aliases {
    @NotNull String @NotNull [] value() default { };
}
