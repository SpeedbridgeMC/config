package io.github.speedbridgemc.config.processor.api;

import com.squareup.javapoet.CodeBlock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.type.TypeMirror;
import java.lang.annotation.Annotation;

public interface ConfigField {
    static @NotNull ConfigField simple(@NotNull TypeMirror type, @NotNull String name,
                                       @NotNull AnnotatedConstruct annotatedConstruct) {
        return new ConfigField() {
            @Override
            public @NotNull TypeMirror type() {
                return type;
            }

            @Override
            public @NotNull String name() {
                return name;
            }

            @Override
            public <A extends Annotation> @Nullable A annotation(@NotNull Class<A> annotationClass) {
                return annotatedConstruct.getAnnotation(annotationClass);
            }

            @Override
            public @NotNull CodeBlock generateGet(@NotNull String object, @NotNull String dest) {
                return CodeBlock.of("%s = %s.%s", dest, object, name);
            }

            @Override
            public @NotNull CodeBlock generateSet(@NotNull String object, @NotNull String src) {
                return CodeBlock.of("%s.%s = %s", object, name, src);
            }
        };
    }

    static @NotNull ConfigField property(@NotNull TypeMirror type, @NotNull String name,
                                         @NotNull String getter, @NotNull String setter,
                                         @NotNull AnnotatedConstruct annotatedConstruct) {
        return new ConfigField() {
            @Override
            public @NotNull TypeMirror type() {
                return type;
            }

            @Override
            public @NotNull String name() {
                return name;
            }

            @Override
            public <A extends Annotation> @Nullable A annotation(@NotNull Class<A> annotationClass) {
                return annotatedConstruct.getAnnotation(annotationClass);
            }

            @Override
            public @NotNull CodeBlock generateGet(@NotNull String object, @NotNull String dest) {
                return CodeBlock.of("%s = %s.%s()", dest, object, getter);
            }

            @Override
            public @NotNull CodeBlock generateSet(@NotNull String object, @NotNull String src) {
                return CodeBlock.of("%s.%s(%s)", object, setter, src);
            }
        };
    }

    @NotNull TypeMirror type();
    @NotNull String name();
    <A extends Annotation> @Nullable A annotation(@NotNull Class<A> annotationClass);
    @NotNull CodeBlock generateGet(@NotNull String object, @NotNull String dest);
    @NotNull CodeBlock generateSet(@NotNull String object, @NotNull String src);
}
