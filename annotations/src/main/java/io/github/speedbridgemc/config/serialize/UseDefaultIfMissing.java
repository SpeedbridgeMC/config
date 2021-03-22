package io.github.speedbridgemc.config.serialize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies that, should a field be missing, the serializer should load the field's default value instead.<p>
 * If a type is marked with this method, all of its fields will "inherit" the annotation and its values.
 */
@Target({ ElementType.TYPE, ElementType.FIELD })
@Retention(RetentionPolicy.SOURCE)
public @interface UseDefaultIfMissing { }
