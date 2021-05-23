package io.github.speedbridgemc.config.processor.api.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.AnnotatedConstruct;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.function.Function;
import java.util.function.Predicate;

public final class AnnotationUtils {
    private AnnotationUtils() { }

    public static <T extends Annotation, V> @Nullable V getAnnotationValue(@NotNull Iterable<? extends AnnotatedConstruct> constructs,
                                                                           @NotNull Class<T> annotationType,
                                                                           @NotNull Function<T, V> valueMapper,
                                                                           @NotNull Predicate<V> valueChecker) {
        for (AnnotatedConstruct construct : constructs) {
            if (construct == null)
                continue;
            T anno = construct.getAnnotation(annotationType);
            if (anno == null)
                continue;
            V value = valueMapper.apply(anno);
            if (valueChecker.test(value))
                return value;
        }
        return null;
    }

    public static <T extends Annotation, V> @Nullable V getAnnotationValue(@NotNull Class<T> annotationType,
                                                                           @NotNull Function<T, V> valueMapper,
                                                                           @NotNull Predicate<V> valueChecker,
                                                                           @Nullable AnnotatedConstruct @NotNull ... constructs) {
        return getAnnotationValue(Arrays.asList(constructs), annotationType, valueMapper, valueChecker);
    }
}
