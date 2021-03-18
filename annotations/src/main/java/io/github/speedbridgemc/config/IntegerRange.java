package io.github.speedbridgemc.config;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface IntegerRange {
    long max();
    @NotNull RangeMode maxMode() default RangeMode.EXCLUSIVE;
    long min() default 0;
    @NotNull RangeMode minMode() default RangeMode.INCLUSIVE;
    @NotNull EnforceMode mode() default EnforceMode.ERROR;
}
