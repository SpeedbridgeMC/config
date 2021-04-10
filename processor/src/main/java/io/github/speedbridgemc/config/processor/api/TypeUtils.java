package io.github.speedbridgemc.config.processor.api;

import com.squareup.javapoet.*;
import io.github.speedbridgemc.config.Exclude;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Utilities for working with {@link TypeName}s, {@link TypeMirror}s and {@link TypeElement}s.
 */
public final class TypeUtils {
    private TypeUtils() { }

    /**
     * Gets the {@link TypeName} of a {@link TypeElement}, by the {@code TypeElement}'s name.
     * @param processingEnv processing environment
     * @param name type element name
     * @return type name, or {@link TypeName#VOID} if it couldn't be found
     */
    public static @NotNull TypeName getTypeName(@NotNull ProcessingEnvironment processingEnv, @NotNull String name) {
        TypeElement type = processingEnv.getElementUtils().getTypeElement(name);
        if (type == null) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Missing class \"" + name + "\"");
            return TypeName.VOID;
        }
        return TypeName.get(type.asType());
    }

    /**
     * Gets the {@link TypeMirror} of a {@link TypeElement}, by the {@code TypeElement}'s name.
     * @param processingEnv processing environment
     * @param name type element name
     * @return type mirror, or {@code null} if it couldn't be found
     */
    public static @Nullable TypeMirror getTypeMirror(@NotNull ProcessingEnvironment processingEnv, @NotNull String name) {
        TypeElement type = processingEnv.getElementUtils().getTypeElement(name);
        if (type == null) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Missing class \"" + name + "\"");
            return null;
        }
        return type.asType();
    }

    /**
     * Gets all field elements in the specified element list.
     * @param elements element list
     * @return field list
     */
    public static @NotNull List<@NotNull VariableElement> fieldsIn(@NotNull List<@NotNull ? extends Element> elements) {
        List<VariableElement> fields = new ArrayList<>();
        for (Element element : elements) {
            if (element.getKind() != ElementKind.FIELD)
                continue;
            fields.add((VariableElement) element);
        }
        return fields;
    }

    /**
     * Gets all fields that should be processed in an element.
     * @param typeElement type element
     * @return list of fields to process
     */
    public static @NotNull List<@NotNull VariableElement> getFieldsToProcess(@NotNull TypeElement typeElement) {
        List<VariableElement> list = new ArrayList<>();
        for (VariableElement field : fieldsIn(typeElement.getEnclosedElements())) {
            Set<Modifier> modifiers = field.getModifiers();
            if (!modifiers.contains(Modifier.TRANSIENT)
                    && !modifiers.contains(Modifier.STATIC)
                    && !modifiers.contains(Modifier.FINAL)
                    && field.getAnnotation(Exclude.class) == null)
                list.add(field);
        }
        return list;
    }

    private static final HashMap<TypeElement, Boolean> DEFAULT_CONSTRUCTOR_CACHE = new HashMap<>();

    /**
     * Checks if a type element has a default (no arguments) constructor.
     * @param type type to check
     * @return {@code true} if the type has a default constructor, {@code false} otherwise
     */
    public static boolean hasDefaultConstructor(@NotNull TypeElement type) {
        return DEFAULT_CONSTRUCTOR_CACHE.computeIfAbsent(type, typeElement -> {
            boolean hasDefaultConstructor = false;
            for (ExecutableElement constructor : ElementFilter.constructorsIn(typeElement.getEnclosedElements())) {
                if (constructor.getParameters().isEmpty() && constructor.getModifiers().contains(Modifier.PUBLIC)) {
                    hasDefaultConstructor = true;
                    break;
                }
            }
            return hasDefaultConstructor;
        });
    }

    private static final HashMap<TypeName, String> SIMPLE_NAME_CACHE = new HashMap<>();

    /**
     * Gets a {@link TypeName}'s simple name.
     * @param typeName type name
     * @return simple name
     */
    public static @NotNull String getSimpleName(@NotNull TypeName typeName) {
        return SIMPLE_NAME_CACHE.computeIfAbsent(typeName, typeName1 -> {
            if (typeName1 instanceof ClassName)
                return ((ClassName) typeName1).simpleName();
            else if (typeName1 instanceof ArrayTypeName)
                return getSimpleName(((ArrayTypeName) typeName1).componentType) + "[]";
            else if (typeName1 instanceof ParameterizedTypeName) {
                ParameterizedTypeName ptn = (ParameterizedTypeName) typeName1;
                StringBuilder sb = new StringBuilder(getSimpleName(ptn.rawType)).append('<');
                for (TypeName typeArg : ptn.typeArguments)
                    sb.append(getSimpleName(typeArg)).append(',');
                if (ptn.typeArguments.size() > 0)
                    sb.setLength(sb.length() - 1); // remove last ","
                return sb.append('>').toString();
            } else if (typeName1 instanceof TypeVariableName)
                return ((TypeVariableName) typeName1).name;
            else
                return typeName1.withoutAnnotations().toString();
        });
    }

    private static final HashMap<TypeMirror, TypeName> TYPE_NAME_CACHE = new HashMap<>();

    /**
     * Gets a {@link TypeMirror}'s simple name.
     * @param type type mirror
     * @return simple name
     */
    public static @NotNull String getSimpleName(@NotNull TypeMirror type) {
        return getSimpleName(TYPE_NAME_CACHE.computeIfAbsent(type, TypeName::get));
    }

    private static final Pattern GENERICS_PATTERN = Pattern.compile("[<,>]");
    private static final Pattern ARRAY_PATTERN = Pattern.compile("\\[]");

    private static final HashMap<String, String> SANITIZED_CACHE = new HashMap<>();

    /**
     * Sanitizes a string for use as a Java identifier.
     * @param name name
     * @return sanitized name
     */
    public static @NotNull String sanitizeForId(@NotNull String name) {
        return SANITIZED_CACHE.computeIfAbsent(name, a -> {
            a = GENERICS_PATTERN.matcher(a).replaceAll("");
            return ARRAY_PATTERN.matcher(a).replaceAll("Array");
        });
    }

    /**
     * Returns a {@link TypeName}'s simple name, sanitized for use as a Java identifier.
     * @param name type name
     * @return sanitized simple name
     */
    public static @NotNull String getSimpleIdSafeName(@NotNull TypeName name) {
        return sanitizeForId(getSimpleName(name));
    }

    /**
     * Returns a {@link TypeMirror}'s simple name, sanitized for use as a Java identifier.
     * @param type type mirror
     * @return sanitized simple name
     */
    public static @NotNull String getSimpleIdSafeName(@NotNull TypeMirror type) {
        return sanitizeForId(getSimpleName(type));
    }
}
