package io.github.speedbridgemc.config;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface Config {
    @NotNull ScanTarget @NotNull [] scanFor() default { ScanTarget.FIELDS, ScanTarget.PROPERTIES };

    enum ScanTarget {
        FIELDS, PROPERTIES
    }

    @Documented
    @Retention(RetentionPolicy.SOURCE)
    @Target({ ElementType.FIELD, ElementType.METHOD })
    @interface Exclude { }

    @Documented
    @Retention(RetentionPolicy.SOURCE)
    @Target({ ElementType.FIELD, ElementType.METHOD })
    @interface Value {
        @NotNull String name() default "";
        @NotNull String getter() default "";
        @NotNull String setter() default "";
    }
}
