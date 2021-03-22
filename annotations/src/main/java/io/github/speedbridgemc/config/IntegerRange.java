package io.github.speedbridgemc.config;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.*;

/**
 * Applies a value range constraint to integer fields ({@code int}, {@code long},
 * and their {@linkplain Integer matching} {@linkplain Long wrappers}).<p>
 * Ignored when applied to a field of any other type.<p>
 * If a type is marked with this method, all of its fields will "inherit" the annotation and its values.
 */
@Documented
@Target({ ElementType.TYPE, ElementType.FIELD, ElementType.TYPE_USE })
@Retention(RetentionPolicy.SOURCE)
public @interface IntegerRange {
    /**
     * The maximum value.
     * @return maximum value
     */
    long max();

    /**
     * Whether the maximum value is inclusive or exclusive. Defaults to {@linkplain RangeMode#EXCLUSIVE exclusive}.
     * @return maximum value mode
     */
    @NotNull RangeMode maxMode() default RangeMode.EXCLUSIVE;

    /**
     * The minimum value. Defaults to 0.
     * @return minimum value
     */
    long min() default 0;

    /**
     * Whether the minimum value is inclusive or exclusive. Defaults to {@linkplain RangeMode#INCLUSIVE exclusive}.
     * @return minimum value mode
     */
    @NotNull RangeMode minMode() default RangeMode.INCLUSIVE;

    /**
     * The constraint enforcement mode to use. Defaults to {@linkplain EnforceMode#ERROR throwing an error}.
     * @return enforcement mode
     */
    @NotNull EnforceMode mode() default EnforceMode.ERROR;
}
