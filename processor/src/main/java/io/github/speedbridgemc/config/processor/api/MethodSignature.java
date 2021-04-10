package io.github.speedbridgemc.config.processor.api;

import com.squareup.javapoet.TypeName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import java.util.ArrayList;
import java.util.Optional;

/**
 * Represents a method signature.<p>
 * Used for checking what methods the handler interface defines/implements.
 */
public final class MethodSignature {
    private final @NotNull String name;
    private final @NotNull TypeName @NotNull [] parameters;
    private final boolean isDefault;
    private final @Nullable TypeName returnType;

    private MethodSignature(@NotNull String name, @NotNull TypeName @NotNull [] parameters, boolean isDefault, @Nullable TypeName returnType) {
        this.name = name;
        this.parameters = parameters;
        this.isDefault = isDefault;
        this.returnType = returnType;
    }

    /**
     * Creates a {@code MethodSignature} that represents a {@link ExecutableElement}.
     * @param method element to represent
     * @return a signature based on the specified element
     */
    public static @NotNull MethodSignature fromElement(@NotNull ExecutableElement method) {
        ArrayList<TypeName> parameters = new ArrayList<>();
        for (VariableElement parameter : method.getParameters())
            parameters.add(TypeName.get(parameter.asType()).withoutAnnotations());
        return new MethodSignature(method.getSimpleName().toString(), parameters.toArray(new TypeName[0]),
                method.isDefault(), TypeName.get(method.getReturnType()).withoutAnnotations());
    }

    /**
     * Creates a {@code MethodSignature}.
     * @param returnType return type of method
     * @param name name of method
     * @param parameters parameters of method
     * @return a signature
     */
    public static @NotNull MethodSignature of(@NotNull TypeName returnType, @NotNull String name, @NotNull TypeName... parameters) {
        return new MethodSignature(name, parameters, false, returnType);
    }

    /**
     * Creates a {@code MethodSignature} which represents a {@code default} method.
     * @param returnType return type of method
     * @param name name of method
     * @param parameters parameters of method
     * @return a signature
     */
    public static @NotNull MethodSignature ofDefault(@NotNull TypeName returnType, @NotNull String name, @NotNull TypeName... parameters) {
        return new MethodSignature(name, parameters, true, returnType);
    }

    /**
     * Creates a {@code MethodSignature} which represents a {@code default} method with an unspecified return type.
     * @param name name of method
     * @param parameters parameters of method
     * @return a signature
     */
    public static @NotNull MethodSignature ofDefault(@NotNull String name, @NotNull TypeName... parameters) {
        return new MethodSignature(name, parameters, true, null);
    }

    /**
     * Gets the name of the method.
     * @return method name
     */
    public @NotNull String getName() {
        return name;
    }

    /**
     * Gets the method's parameters.
     * @return method parameters
     */
    public @NotNull TypeName @NotNull [] getParameters() {
        return parameters.clone();
    }

    /**
     * Checks if the method is a {@code default} method.
     * @return {@code true} if {@code default}, {@code false} otherwise
     */
    public boolean isDefault() {
        return isDefault;
    }

    /**
     * Gets the method's return type.
     * @return method return type, or empty if unspecified
     */
    public @NotNull Optional<TypeName> getReturnType() {
        return Optional.ofNullable(returnType);
    }

    /**
     * Checks if this method signature matches another signature.<p>
     * Note that this is <em>not</em> an equality check.
     * @param other other signature
     * @return {@code true} if this signature and the other signature match, {@code false} otherwise
     */
    public boolean matches(@NotNull MethodSignature other) {
        if (returnType == null)
            return toStringWithoutReturnType(true).equals(other.toStringWithoutReturnType(true));
        else
            return toString().equals(other.toString());
    }

    /**
     * Checks if a method signature has a matching signature in an {@link Iterable}.
     * @param methods method iterable
     * @param signature signature to check
     * @return {@code true} if a match was found, {@code false} otherwise
     */
    public static boolean contains(@NotNull Iterable<@NotNull ? extends MethodSignature> methods,
                                   @NotNull MethodSignature signature) {
        for (MethodSignature method : methods) {
            if (signature.matches(method))
                return true;
        }
        return false;
    }

    private @Nullable String cachedNRTString, cachedFullString;

    private @NotNull String toStringWithoutReturnType(boolean addDefault) {
        if (cachedNRTString == null) {
            StringBuilder sb = new StringBuilder(name).append('(');
            for (TypeName param : parameters)
                sb.append(param).append(", ");
            if (parameters.length > 0)
                sb.setLength(sb.length() - 2);
            cachedNRTString = sb.append(')').toString();
        }
        if (isDefault && addDefault)
            return "default " + cachedNRTString;
        else
            return cachedNRTString;
    }

    /**
     * Returns this signature as a {@link String}.
     * @return {@code String} representation
     */
    @Override
    public @NotNull String toString() {
        if (cachedFullString == null) {
            StringBuilder sb = new StringBuilder();
            if (isDefault)
                sb.append("default ");
            if (returnType == null)
                sb.append("<ignored>");
            else
                sb.append(returnType);
            sb.append(' ').append(toStringWithoutReturnType(false));
            cachedFullString = sb.toString();
        }
        return cachedFullString;
    }
}
