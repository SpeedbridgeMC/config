package io.github.speedbridgemc.config;

import org.intellij.lang.annotations.Language;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;

/**
 * Specifies that the marked {@code String} should be viewed as an existing class.
 */
@Target({ METHOD, FIELD, PARAMETER, LOCAL_VARIABLE, ANNOTATION_TYPE })
@Retention(RetentionPolicy.SOURCE)
@Language(value = "JAVA", prefix = "final class X { Class<?> clazz = ", suffix = ".class }")
public @interface JavaClass { }
