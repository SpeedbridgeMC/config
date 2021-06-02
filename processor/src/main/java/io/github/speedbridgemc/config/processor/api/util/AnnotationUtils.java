package io.github.speedbridgemc.config.processor.api.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class AnnotationUtils {
    private AnnotationUtils() { }

    public static <A extends Annotation, V> @Nullable V getFirstValue(@NotNull Iterable<? extends AnnotatedConstruct> constructs,
                                                                      @NotNull Class<A> annotationType,
                                                                      @NotNull Function<A, V> valueMapper,
                                                                      @NotNull Predicate<V> valueChecker) {
        for (AnnotatedConstruct construct : constructs) {
            if (construct == null)
                continue;
            A anno = construct.getAnnotation(annotationType);
            if (anno == null)
                continue;
            V value = valueMapper.apply(anno);
            if (valueChecker.test(value))
                return value;
        }
        return null;
    }

    public static <A extends Annotation, V> @Nullable V getFirstValue(@NotNull Class<A> annotationType,
                                                                      @NotNull Function<A, V> valueMapper,
                                                                      @NotNull Predicate<V> valueChecker,
                                                                      @Nullable AnnotatedConstruct @NotNull ... constructs) {
        return getFirstValue(Arrays.asList(constructs), annotationType, valueMapper, valueChecker);
    }

    public static <A extends Annotation, V> @NotNull V concatValues(@NotNull Iterable<? extends AnnotatedConstruct> constructs,
                                                                    @NotNull Class<A> annotationType,
                                                                    @NotNull Function<A, V> valueMapper,
                                                                    @NotNull Supplier<V> initialValueSupplier,
                                                                    @NotNull BinaryOperator<V> valueCombiner) {
        V finalValue = initialValueSupplier.get();
        for (AnnotatedConstruct construct : constructs) {
            if (construct == null)
                continue;
            A anno = construct.getAnnotation(annotationType);
            if (anno == null)
                continue;
            V value = valueMapper.apply(anno);
            finalValue = valueCombiner.apply(finalValue, value);
        }
        return finalValue;
    }

    public static <A extends Annotation, V> @NotNull V concatValues(@NotNull Class<A> annotationType,
                                                                    @NotNull Function<A, V> valueMapper,
                                                                    @NotNull Supplier<V> initialValueSupplier,
                                                                    @NotNull BinaryOperator<V> valueCombiner,
                                                                    @Nullable AnnotatedConstruct @NotNull ... constructs) {
        return concatValues(Arrays.asList(constructs), annotationType, valueMapper, initialValueSupplier, valueCombiner);
    }

    public static <A extends Annotation> @NotNull TypeMirror getClass(@NotNull Elements elements,
                                                                      @NotNull A annotation,
                                                                      @NotNull Function<A, Class<?>> valueMapper) {
        Class<?> aClass;
        try {
            aClass = valueMapper.apply(annotation);
        } catch (MirroredTypeException e) {
            return e.getTypeMirror();
        }
        return elements.getTypeElement(aClass.getCanonicalName()).asType();
    }

    public static <A extends Annotation> @NotNull List<? extends TypeMirror> getClasses(@NotNull Elements elements,
                                                                                        @NotNull A annotation,
                                                                                        @NotNull Function<A, Class<?>[]> valueMapper) {
        Class<?>[] classes;
        try {
            classes = valueMapper.apply(annotation);
        } catch (MirroredTypesException e) {
            return e.getTypeMirrors();
        }
        ArrayList<TypeMirror> typeMirrors = new ArrayList<>();
        for (Class<?> aClass : classes)
            typeMirrors.add(elements.getTypeElement(aClass.getCanonicalName()).asType());
        return typeMirrors;
    }
}
