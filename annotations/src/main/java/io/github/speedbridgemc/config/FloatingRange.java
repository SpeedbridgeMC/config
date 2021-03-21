package io.github.speedbridgemc.config;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.TYPE, ElementType.FIELD, ElementType.TYPE_USE })
@Retention(RetentionPolicy.SOURCE)
public @interface FloatingRange {
    double max();
    @NotNull RangeMode maxMode() default RangeMode.EXCLUSIVE;
    double min() default 0;
    @NotNull RangeMode minMode() default RangeMode.INCLUSIVE;
    @NotNull EnforceMode mode() default EnforceMode.ERROR;
}
