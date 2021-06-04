package io.github.speedbridgemc.config;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface Config {
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
        @NotNull ScanTarget @NotNull [] scanFor() default { ScanTarget.FIELDS, ScanTarget.PROPERTIES };

        @NotNull Class<?> constructorOwner() default Void.class;
        @NotNull Class<?>[] constructorParams() default { Void.class };
        @NotNull Class<?> factoryOwner() default Void.class;
        @NotNull String factoryName() default "";
        @NotNull Class<?>[] factoryParams() default { Void.class };
    }

    @Documented
    @Retention(RetentionPolicy.SOURCE)
    @Target(ElementType.PARAMETER)
    @interface BoundProperty {
        @NotNull String value();
    }
}
