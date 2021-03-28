package io.github.speedbridgemc.config.serialize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method to be used to deserialize a {@code KeyedEnum} value.<p>
 * Methods marked with this annotation should be public and static, take the enum's key type as its single parameter,
 * and return an enum value.<p>
 * Ignored outside of enums that don't implement {@code KeyedEnum}.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface KeyedEnumDeserializer { }
