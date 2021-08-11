package io.github.speedbridgemc.config.processor.api.util;

import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.Optional;

import static io.github.speedbridgemc.config.processor.api.util.StringUtils.camelCaseToUpperCamelCase;

public final class PropertyUtils {
    private PropertyUtils() { }

    public static final class AccessorInfo {
        public enum Kind { GETTER, SETTER }

        public final PropertyUtils.AccessorInfo.Kind kind;
        public final TypeMirror propertyType;

        public AccessorInfo(PropertyUtils.AccessorInfo.Kind kind, TypeMirror propertyType) {
            this.kind = kind;
            this.propertyType = propertyType;
        }
    }

    public static Optional<AccessorInfo> getAccessorInfo(ExecutableType method) {
        if (method.getParameterTypes().isEmpty() && method.getReturnType().getKind() != TypeKind.VOID)
            return Optional.of(new AccessorInfo(AccessorInfo.Kind.GETTER, method.getReturnType()));
        else if (method.getParameterTypes().size() == 1 && method.getReturnType().getKind() == TypeKind.VOID)
            return Optional.of(new AccessorInfo(AccessorInfo.Kind.SETTER, method.getParameterTypes().get(0)));
        return Optional.empty();
    }

    public static String getPropertyName(String accessorName, boolean isBool) {
        if (isBool && accessorName.startsWith("is"))
            return StringUtils.stripAndLower(accessorName, 2);
        if (accessorName.startsWith("get") || accessorName.startsWith("set"))
            return StringUtils.stripAndLower(accessorName, 3);
        return accessorName;
    }

    public static String makeGetterName(String propertyName, boolean isBool) {
        if (isBool)
            return "is" + camelCaseToUpperCamelCase(propertyName);
        else
            return "get" + camelCaseToUpperCamelCase(propertyName);
    }

    public static String makeSetterName(String propertyName) {
        return "set" + camelCaseToUpperCamelCase(propertyName);
    }
}
