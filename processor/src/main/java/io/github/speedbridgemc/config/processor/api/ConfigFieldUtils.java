package io.github.speedbridgemc.config.processor.api;

import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ConfigFieldUtils {
    private ConfigFieldUtils() { }

    private static final @NotNull AnnotatedConstruct EMPTY_ANNOTATED_CONSTRUCT = new AnnotatedConstruct() {
        @Override
        public List<? extends AnnotationMirror> getAnnotationMirrors() {
            return Collections.emptyList();
        }

        @Override
        public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
            return null;
        }

        @Override
        public <A extends Annotation> A[] getAnnotationsByType(Class<A> annotationType) {
            return ArrayUtils.emptyArrayOf(annotationType);
        }
    };

    public static @NotNull AnnotatedConstruct emptyAnnotatedConstruct() {
        return EMPTY_ANNOTATED_CONSTRUCT;
    }

    public static @NotNull AnnotatedConstruct concatAnnotatedConstructs(@NotNull AnnotatedConstruct @NotNull ... constructs) {
        if (constructs.length == 0)
            return emptyAnnotatedConstruct();
        else if (constructs.length == 1)
            return constructs[0];
        else {
            ImmutableList.Builder<AnnotationMirror> mirrorsBuilder = ImmutableList.builder();
            for (AnnotatedConstruct construct : constructs)
                mirrorsBuilder.addAll(construct.getAnnotationMirrors());
            ImmutableList<AnnotationMirror> mirrors = mirrorsBuilder.build();
            return new AnnotatedConstruct() {
                @Override
                public List<? extends AnnotationMirror> getAnnotationMirrors() {
                    return mirrors;
                }

                @Override
                public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
                    for (AnnotatedConstruct construct : constructs) {
                        A annotation = construct.getAnnotation(annotationType);
                        if (annotation != null)
                            return annotation;
                    }
                    return null;
                }

                @Override
                public <A extends Annotation> A[] getAnnotationsByType(Class<A> annotationType) {
                    ArrayList<A> list = new ArrayList<>();
                    for (AnnotatedConstruct construct : constructs)
                        Collections.addAll(list, construct.getAnnotationsByType(annotationType));
                    return list.toArray(ArrayUtils.emptyArrayOf(annotationType));
                }
            };
        }
    }

    public static @NotNull List<@NotNull ? extends ConfigField> getConfigFields(@NotNull TypeElement element) {
        ArrayList<ConfigField> list = new ArrayList<>();
        for (VariableElement field : TypeUtils.fieldsIn(element.getEnclosedElements()))
            list.add(ConfigField.simple(field.asType(), field.getSimpleName().toString(), field));
        // TODO handle getters/setters
        return list;
    }
}
