package io.github.speedbridgemc.config.processor.api.util;

import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.Locale;
import java.util.Optional;

public final class PropertyUtils {
    private PropertyUtils() { }

    public static final class AccessorInfo {
        public enum Kind { GETTER, SETTER }

        public final @NotNull PropertyUtils.AccessorInfo.Kind kind;
        public final @NotNull TypeMirror propertyType;

        public AccessorInfo(@NotNull PropertyUtils.AccessorInfo.Kind kind, @NotNull TypeMirror propertyType) {
            this.kind = kind;
            this.propertyType = propertyType;
        }
    }

    public static @NotNull Optional<AccessorInfo> getAccessorInfo(@NotNull ExecutableElement method) {
        if (method.getParameters().isEmpty() && method.getReturnType().getKind() != TypeKind.VOID)
            return Optional.of(new AccessorInfo(AccessorInfo.Kind.GETTER, method.getReturnType()));
        else if (method.getParameters().size() == 1 && method.getReturnType().getKind() == TypeKind.VOID)
            return Optional.of(new AccessorInfo(AccessorInfo.Kind.SETTER, method.getParameters().get(0).asType()));
        return Optional.empty();
    }

    public static @NotNull String getPropertyName(@NotNull String accessorName, boolean isBool) {
        if (isBool && accessorName.startsWith("is"))
            return stripAndLower(accessorName, 2);
        if (accessorName.startsWith("get") || accessorName.startsWith("set"))
            return stripAndLower(accessorName, 3);
        return accessorName;
    }

    public static @NotNull String makeGetterName(@NotNull String propertyName, boolean isBool) {
        if (isBool)
            return "is" + titleCase(propertyName);
        else
            return "get" + titleCase(propertyName);
    }

    public static @NotNull String makeSetterName(@NotNull String propertyName) {
        return "set" + titleCase(propertyName);
    }

    private static @NotNull String titleCase(@NotNull String s) {
        if (s.isEmpty())
            return "";
        if (s.length() == 1)
            return s.toUpperCase(Locale.ROOT);
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static @NotNull String stripAndLower(@NotNull String s, int count) {
        if (s.length() <= count)
            return s;
        s = s.substring(count);
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }
}
