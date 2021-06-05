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
        boolean optional() default false;
    }

    @Documented
    @Retention(RetentionPolicy.SOURCE)
    @Target(ElementType.TYPE)
    @interface Struct {
        @NotNull ScanTarget @NotNull [] scanFor() default { ScanTarget.FIELDS, ScanTarget.PROPERTIES };

        @NotNull Class<?> constructorOwner() default Void.class;
        // type parameters aren't necessary here - can't have 2 methods with the same signatures post-erasure
        @NotNull Class<?>[] constructorParams() default { Void.class };
        @NotNull Class<?> factoryOwner() default Void.class;
        @NotNull String factoryName() default "";
        // factory return type is implied to be the annotated struct
        @NotNull Class<?>[] factoryParams() default { Void.class };
        // each parameter's bound property, in order
        // priority is @BoundProperty > boundProperties > auto naming
        //  (if boundProperties has an empty string at that parameter's index, or isn't long enough)
        @NotNull String[] boundProperties() default { };
    }

    @Documented
    @Retention(RetentionPolicy.SOURCE)
    @Target(ElementType.PARAMETER)
    @interface BoundProperty {
        @NotNull String value();
    }
}
