package io.github.speedbridgemc.config.processor.api.util;

import org.jetbrains.annotations.Nullable;

import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.TypeMirror;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class AnnotationUtils {
    private AnnotationUtils() { }

    public static <A extends Annotation, V> @Nullable V getFirstValue(Iterable<? extends AnnotatedConstruct> constructs,
                                                                      Class<A> annotationType,
                                                                      Function<A, V> valueMapper,
                                                                      Predicate<V> valueChecker) {
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

    public static <A extends Annotation, V> @Nullable V getFirstValue(Class<A> annotationType,
                                                                      Function<A, V> valueMapper,
                                                                      Predicate<V> valueChecker,
                                                                      @Nullable AnnotatedConstruct ... constructs) {
        return getFirstValue(Arrays.asList(constructs), annotationType, valueMapper, valueChecker);
    }

    public static <A extends Annotation, V> V concatValues(Iterable<? extends AnnotatedConstruct> constructs,
                                                                    Class<A> annotationType,
                                                                    Function<A, V> valueMapper,
                                                                    Supplier<V> initialValueSupplier,
                                                                    BinaryOperator<V> valueCombiner) {
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

    public static <A extends Annotation, V> V concatValues(Class<A> annotationType,
                                                                    Function<A, V> valueMapper,
                                                                    Supplier<V> initialValueSupplier,
                                                                    BinaryOperator<V> valueCombiner,
                                                                    @Nullable AnnotatedConstruct ... constructs) {
        return concatValues(Arrays.asList(constructs), annotationType, valueMapper, initialValueSupplier, valueCombiner);
    }

    public static <A extends Annotation> TypeMirror getClass(A annotation,
                                                                      Function<A, Class<?>> valueMapper) {
        try {
            valueMapper.apply(annotation);
        } catch (MirroredTypeException e) {
            return e.getTypeMirror();
        }
        throw new InternalError("Getting Class from annotation didn't throw MirroredTypeException?!");
    }

    public static <A extends Annotation> List<? extends TypeMirror> getClasses(A annotation,
                                                                                        Function<A, Class<?>[]> valueMapper) {
        try {
            valueMapper.apply(annotation);
        } catch (MirroredTypesException e) {
            return e.getTypeMirrors();
        }
        throw new InternalError("Getting Class[] from annotation didn't throw MirroredTypesException?!");
    }
}
