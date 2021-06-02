package io.github.speedbridgemc.config;

import org.jetbrains.annotations.ApiStatus;
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
    @interface Property {
        @NotNull String name() default "";
        @NotNull String getter() default "";
        @NotNull String setter() default "";
    }

    @Documented
    @Retention(RetentionPolicy.SOURCE)
    @Target(ElementType.TYPE)
    @interface Struct {
        @NotNull Class<?> constructorOwner() default Config.None.class;
        @NotNull Class<?>[] constructorParams() default { Config.None.class };
        @NotNull Class<?> factoryOwner() default Config.None.class;
        @NotNull String factoryName() default "";
        @NotNull Class<?>[] factoryParams() default { Config.None.class };

    }

    @Documented
    @Retention(RetentionPolicy.SOURCE)
    @Target(ElementType.PARAMETER)
    @interface Field {
        @NotNull String value();
    }

    @Documented
    @Retention(RetentionPolicy.SOURCE)
    @Target(ElementType.PARAMETER)
    @interface Getter {
        @NotNull String value();
    }

    // marker class used for unspecified Class<?> and Class<?>[] attributes
    @ApiStatus.Internal
    final class None { }
}
