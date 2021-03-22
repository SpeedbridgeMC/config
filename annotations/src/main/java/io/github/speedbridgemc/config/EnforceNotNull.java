package io.github.speedbridgemc.config;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.*;

/**
 * Constrains the field to non-null values.<p>
 * Ignored when applied to primitive fields (that cannot be null anyways).<p>
 * If a type is marked with this method, all of its fields will "inherit" the annotation and its values.
 */
@Documented
@Target({ ElementType.TYPE, ElementType.FIELD, ElementType.TYPE_USE })
@Retention(RetentionPolicy.SOURCE)
public @interface EnforceNotNull {
    /**
     * The constraint enforcement mode to use. Defaults to {@linkplain EnforceMode#ERROR throwing an error}.
     * @return enforcement mode
     */
    @NotNull EnforceMode value() default EnforceMode.ERROR;
}
