package io.github.speedbridgemc.config;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.*;

/**
 * Specifies the name of an enum constant.<p>
 * This is in the runtime module (and is available at runtime via reflection) to allow using the enum name in code
 * without having to specify it twice.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface EnumName {
    @NotNull String value();
}
