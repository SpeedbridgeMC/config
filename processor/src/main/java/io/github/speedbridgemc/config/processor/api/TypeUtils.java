package io.github.speedbridgemc.config.processor.api;

import com.squareup.javapoet.*;
import io.github.speedbridgemc.config.Exclude;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.SimpleElementVisitor8;
import javax.tools.Diagnostic;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Utilities for working with {@link TypeName}s, {@link TypeMirror}s and {@link TypeElement}s.
 */
public final class TypeUtils {
    private TypeUtils() { }

    private static ClassName generatedAnnotation;

    /**
     * Gets the {@code @Generated} annotation's {@link ClassName}.<p>
     * Needed because the clowns at Java HQ decided to move this annotation to another package in Java 9.
     * @param processingEnv processing environment
     * @return class name of {@code @Generated} annotation
     */
    public static @NotNull ClassName getGeneratedAnnotation(@NotNull ProcessingEnvironment processingEnv) {
        if (generatedAnnotation == null) {
            TypeElement elem = processingEnv.getElementUtils().getTypeElement("javax.annotation.processing.Generated");
            if (elem == null)
                elem = processingEnv.getElementUtils().getTypeElement("javax.annotation.Generated");
            if (elem == null) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "Missing @Generated annotation!");
                generatedAnnotation = ClassName.get(Deprecated.class);
                return generatedAnnotation;
            }
            generatedAnnotation = ClassName.get(elem);
        }
        return generatedAnnotation;
    }

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

    private static @NotNull TypeName getParameterizedName(@NotNull TypeMirror mirror, @NotNull Map<String, TypeName> typeParams) {
        return getParameterizedName(TypeName.get(mirror), typeParams);
    }

    private static @NotNull TypeName getParameterizedName(@NotNull TypeName typeName, @NotNull Map<String, TypeName> typeParams) {
        if (typeName instanceof TypeVariableName)
            return typeParams.get(((TypeVariableName) typeName).name);
        else if (typeName instanceof ParameterizedTypeName) {
            ParameterizedTypeName ptn = (ParameterizedTypeName) typeName;
            List<TypeName> typeArgs = ptn.typeArguments;
            TypeName[] resolvedTypeArgs = new TypeName[typeArgs.size()];
            for (int i = 0; i < resolvedTypeArgs.length; i++)
                resolvedTypeArgs[i] = getParameterizedName(typeArgs.get(i), typeParams);
            return ParameterizedTypeName.get(ptn.rawType, resolvedTypeArgs);
        } else
            return typeName.withoutAnnotations();
    }

    /**
     * Gets all methods in an element, including methods from its superclass and superinterfaces.
     * @param processingEnv processing environment
     * @param element element to check
     * @return all methods in element
     */
    public static @NotNull Set<@NotNull MethodSignature> allMethodsIn(@NotNull ProcessingEnvironment processingEnv, @NotNull TypeElement element,
                                                                      @NotNull List<@NotNull TypeMirror> typeArgs) {
        return element.accept(new SimpleElementVisitor8<Set<MethodSignature>, List<TypeMirror>>() {
            @Override
            public Set<MethodSignature> visitType(TypeElement e, List<TypeMirror> typeArgs) {
                Set<MethodSignature> signatures = new HashSet<>();
                for (TypeMirror interfaceMirror : e.getInterfaces()) {
                    if (processingEnv.getTypeUtils().isSameType(e.asType(), interfaceMirror))
                        continue;
                    List<TypeMirror> nestedTypeArgs = new ArrayList<>(((DeclaredType) interfaceMirror).getTypeArguments());
                    TypeElement interfaceElem = (TypeElement) processingEnv.getTypeUtils().asElement(interfaceMirror);
                    signatures.addAll(visitType(interfaceElem, nestedTypeArgs));
                }
                List<? extends TypeParameterElement> typeParamElems = e.getTypeParameters();
                Map<String, TypeName> typeParams = new HashMap<>();
                for (int i = 0, size = typeParamElems.size(); i < size; i++) {
                    TypeParameterElement typeParam = typeParamElems.get(i);
                    String typeParamName = typeParam.getSimpleName().toString();
                    if (!typeParams.containsKey(typeParamName)) {
                        if (i >= typeArgs.size()) {
                            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                                    "Unfilled type parameter", typeParam);
                            return Collections.emptySet();
                        }
                        TypeName value = TypeName.get(typeArgs.get(i)).withoutAnnotations();
                        typeParams.put(typeParamName, value);
                    }
                }
                for (ExecutableElement method : ElementFilter.methodsIn(e.getEnclosedElements())) {
                    TypeName returnTypeName = getParameterizedName(method.getReturnType(), typeParams);
                    List<? extends VariableElement> params = method.getParameters();
                    TypeName[] paramNames = new TypeName[params.size()];
                    for (int i = 0; i < paramNames.length; i++)
                        paramNames[i] = getParameterizedName(params.get(i).asType(), typeParams);
                    MethodSignature signature = method.isDefault()
                            ? MethodSignature.ofDefault(returnTypeName, method.getSimpleName().toString(), paramNames)
                            : MethodSignature.of(returnTypeName, method.getSimpleName().toString(), paramNames);
                    signatures.add(signature);
                }
                return signatures;
            }

            @Override
            protected Set<MethodSignature> defaultAction(Element e, List<TypeMirror> unused) {
                throw new IllegalStateException("uh, not sure what to do with this: " + e);
            }
        }, typeArgs);
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
        String cached = SIMPLE_NAME_CACHE.get(typeName);
        if (cached == null) {
            if (typeName instanceof ClassName)
                cached = ((ClassName) typeName).simpleName();
            else if (typeName instanceof ArrayTypeName)
                cached = getSimpleName(((ArrayTypeName) typeName).componentType) + "[]";
            else if (typeName instanceof ParameterizedTypeName) {
                ParameterizedTypeName ptn = (ParameterizedTypeName) typeName;
                StringBuilder sb = new StringBuilder(getSimpleName(ptn.rawType)).append('<');
                for (TypeName typeArg : ptn.typeArguments)
                    sb.append(getSimpleName(typeArg)).append(',');
                if (ptn.typeArguments.size() > 0)
                    sb.setLength(sb.length() - 1); // remove last ","
                cached = sb.append('>').toString();
            } else if (typeName instanceof TypeVariableName)
                cached = ((TypeVariableName) typeName).name;
            else
                cached = typeName.withoutAnnotations().toString();
            SIMPLE_NAME_CACHE.put(typeName, cached);
        }
        return cached;
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
