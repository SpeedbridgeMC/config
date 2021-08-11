package io.github.speedbridgemc.config.processor.api.util;

import org.jetbrains.annotations.Nullable;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.Elements;

public final class MirrorUtils {
    private MirrorUtils() { }

    public static @Nullable TypeElement getTypeElementNullable(Elements elements, String canonicalName) {
        return elements.getTypeElement(canonicalName);
    }

    public static TypeElement getTypeElement(Elements elements, String canonicalName) {
        TypeElement elem = getTypeElementNullable(elements, canonicalName);
        if (elem == null)
            throw new IllegalStateException("Couldn't get type element for class \"" + canonicalName + "\"! Does it not exist?");
        return elem;
    }

    public static <T> @Nullable TypeElement getTypeElementNullable(Elements elements, Class<T> clazz) {
        if (clazz.isPrimitive())
            throw new UnsupportedOperationException("Can't get type element of primitive class \"" + clazz + "\"!");
        if (clazz.isArray())
            throw new UnsupportedOperationException("Can't get type element of array class \"" + clazz + "\"!");
        return getTypeElementNullable(elements, clazz.getCanonicalName());
    }

    public static <T> TypeElement getTypeElement(Elements elements, Class<T> clazz) {
        TypeElement elem = getTypeElementNullable(elements, clazz);
        if (elem == null)
            throw new IllegalStateException("Couldn't get type element for class \"" + clazz.getCanonicalName() + "\"! Does it not exist?");
        return elem;
    }

    public static @Nullable DeclaredType getDeclaredTypeNullable(Elements elements, String canonicalName) {
        TypeElement elem = getTypeElementNullable(elements, canonicalName);
        if (elem == null)
            return null;
        return (DeclaredType) elem.asType();
    }

    public static DeclaredType getDeclaredType(Elements elements, String canonicalName) {
        DeclaredType mirror = getDeclaredTypeNullable(elements, canonicalName);
        if (mirror == null)
            throw new IllegalStateException("Couldn't get declared type for class \"" + canonicalName + "\"! Does it not exist?");
        return mirror;
    }

    public static <T> @Nullable DeclaredType getDeclaredTypeNullable(Elements elements, Class<T> clazz) {
        TypeElement elem = getTypeElementNullable(elements, clazz);
        if (elem == null)
            return null;
        return (DeclaredType) elem.asType();
    }

    public static <T> DeclaredType getDeclaredType(Elements elements, Class<T> clazz) {
        DeclaredType mirror = getDeclaredTypeNullable(elements, clazz);
        if (mirror == null)
            throw new IllegalStateException("Couldn't get declared type for class \"" + clazz.getCanonicalName() + "\"! Does it not exist?");
        return mirror;
    }
}
