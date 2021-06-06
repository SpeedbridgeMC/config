package io.github.speedbridgemc.config;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface Config {
    StructOverride[] structOverrides() default { };

    @Retention(RetentionPolicy.SOURCE)
    @Target({ })
    @interface StructOverride {
        Class<?> target();
        Struct override();
        Property[] properties() default { };
    }
    
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    @Target({ ElementType.FIELD, ElementType.METHOD })
    @interface Exclude { }

    @Documented
    @Retention(RetentionPolicy.SOURCE)
    @Target({ ElementType.FIELD, ElementType.METHOD })
    @interface Property {
        String name() default "";
        // unused outside of StructOverrides
        String field() default "";
        String getter() default "";
        String setter() default "";
        boolean optional() default false;
    }

    @Documented
    @Retention(RetentionPolicy.SOURCE)
    @Target(ElementType.TYPE)
    @interface Struct {
        ScanTarget[] scanFor() default { ScanTarget.FIELDS, ScanTarget.PROPERTIES };

        Class<?> constructorOwner() default Void.class;
        // type parameters aren't necessary here - can't have 2 methods with the same signatures post-erasure
        Class<?>[] constructorParams() default { Void.class };
        Class<?> factoryOwner() default Void.class;
        String factoryName() default "";
        // factory return type is implied to be the annotated struct
        Class<?>[] factoryParams() default { Void.class };
        // each parameter's bound property, in order
        // priority is @BoundProperty > boundProperties > auto naming
        //  (if boundProperties has an empty string at that parameter's index, or isn't long enough)
        String[] boundProperties() default { };
    }

    @Documented
    @Retention(RetentionPolicy.SOURCE)
    @Target(ElementType.PARAMETER)
    @interface BoundProperty {
        String value();
    }
}
