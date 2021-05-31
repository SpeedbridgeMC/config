package io.github.speedbridgemc.config.processor.api.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.AnnotatedConstruct;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class AnnotationUtils {
    private AnnotationUtils() { }

    public static <T extends Annotation, V> @Nullable V getFirstValue(@NotNull Iterable<? extends AnnotatedConstruct> constructs,
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

    public static <T extends Annotation, V> @Nullable V getFirstValue(@NotNull Class<T> annotationType,
                                                                      @NotNull Function<T, V> valueMapper,
                                                                      @NotNull Predicate<V> valueChecker,
                                                                      @Nullable AnnotatedConstruct @NotNull ... constructs) {
        return getFirstValue(Arrays.asList(constructs), annotationType, valueMapper, valueChecker);
    }

    public static <T extends Annotation, V> @NotNull V concatValues(@NotNull Iterable<? extends AnnotatedConstruct> constructs,
                                                                    @NotNull Class<T> annotationType,
                                                                    @NotNull Function<T, V> valueMapper,
                                                                    @NotNull Supplier<V> initialValueSupplier,
                                                                    @NotNull BinaryOperator<V> valueCombiner) {
        V finalValue = initialValueSupplier.get();
        for (AnnotatedConstruct construct : constructs) {
            if (construct == null)
                continue;
            T anno = construct.getAnnotation(annotationType);
            if (anno == null)
                continue;
            V value = valueMapper.apply(anno);
            finalValue = valueCombiner.apply(finalValue, value);
        }
        return finalValue;
    }

    public static <T extends Annotation, V> @NotNull V concatValues(@NotNull Class<T> annotationType,
                                                                    @NotNull Function<T, V> valueMapper,
                                                                    @NotNull Supplier<V> initialValueSupplier,
                                                                    @NotNull BinaryOperator<V> valueCombiner,
                                                                    @Nullable AnnotatedConstruct @NotNull ... constructs) {
        return concatValues(Arrays.asList(constructs), annotationType, valueMapper, initialValueSupplier, valueCombiner);
    }
}
