package io.github.speedbridgemc.config.processor.serialize;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.*;
import io.github.speedbridgemc.config.LogLevel;
import io.github.speedbridgemc.config.processor.api.*;
import io.github.speedbridgemc.config.processor.serialize.api.NamingStrategyProvider;
import io.github.speedbridgemc.config.processor.serialize.api.SerializerContext;
import io.github.speedbridgemc.config.processor.serialize.api.SerializerProvider;
import io.github.speedbridgemc.config.serialize.*;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

@ApiStatus.Internal
@AutoService(ComponentProvider.class)
public final class SerializerComponentProvider extends BaseComponentProvider {
    private static final ClassName STRING_NAME = ClassName.get(String.class), MAP_NAME = ClassName.get(HashMap.class);
    private static TypeMirror keyedEnumTM;
    private HashMap<String, SerializerProvider> serializerProviders;
    private HashMap<String, NamingStrategyProvider> nameProviders;
    private static NamingStrategyProvider nameProvider;
    private static String nameProviderVariant;

    public SerializerComponentProvider() {
        super("speedbridge-config:serializer");
    }

    @Override
    public void init(@NotNull ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        if (keyedEnumTM == null) {
            keyedEnumTM = TypeUtils.getTypeMirror(processingEnv, KeyedEnum.class.getCanonicalName());
            if (keyedEnumTM != null)
                keyedEnumTM = types.erasure(keyedEnumTM);
        }

        serializerProviders = new HashMap<>();
        ServiceLoader<SerializerProvider> spLoader = ServiceLoader.load(SerializerProvider.class, SerializerComponentProvider.class.getClassLoader());
        for (SerializerProvider provider : spLoader)
            serializerProviders.put(provider.getId(), provider);
        nameProviders = new HashMap<>();
        ServiceLoader<NamingStrategyProvider> nameLoader = ServiceLoader.load(NamingStrategyProvider.class, SerializerComponentProvider.class.getClassLoader());
        for (NamingStrategyProvider provider : nameLoader)
            nameProviders.put(provider.getId(), provider);

        for (SerializerProvider componentProvider : serializerProviders.values())
            componentProvider.init(processingEnv);
        for (NamingStrategyProvider nameProvider : nameProviders.values())
            nameProvider.init(processingEnv);
    }

    @Override
    public void process(@NotNull String name, @NotNull TypeElement type,
                        @NotNull ImmutableList<VariableElement> fields,
                        @NotNull ComponentContext ctx, TypeSpec.@NotNull Builder classBuilder) {
        String providerId = ParamUtils.allOrNothing(ctx.params, "provider");
        if (providerId == null) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "Serializer: No provider specified", type);
            return;
        }
        SerializerProvider provider = serializerProviders.get(providerId);
        if (provider == null) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "Serializer: Unknown provider \"" + providerId + "\"", type);
            return;
        }
        String nameId = ParamUtils.allOrNothing(ctx.params, "naming_strategy");
        if (nameId == null) {
            nameId = "speedbridge-config:snake_case";
            nameProviderVariant = "";
        } else if (nameId.contains("[")) {
            int varStart = nameId.lastIndexOf('[');
            int varEnd = nameId.lastIndexOf(']');
            if (varEnd < 0) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                        "Serializer: Unclosed variant specifier in naming strategy ID", type);
            }
            nameProviderVariant = nameId.substring(varStart + 1, varEnd);
            nameId = nameId.substring(0, varStart);
        } else
            nameProviderVariant = "";
        nameProvider = nameProviders.get(nameId);
        if (nameProvider == null) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "Serializer: Unknown naming strategy \"" + nameId + "\"", type);
            return;
        }
        TypeMirror pathTM = TypeUtils.getTypeMirror(processingEnv, Path.class.getCanonicalName());
        TypeMirror stringTM = TypeUtils.getTypeMirror(processingEnv, String.class.getCanonicalName());
        if (pathTM == null || stringTM == null)
            return;
        TypeName pathName = ClassName.get(Path.class);
        TypeName stringName = ClassName.get(String.class);
        boolean gotResolvePath = ctx.hasMethod(MethodSignature.ofDefault(pathName, "resolvePath", stringName));
        if (!gotResolvePath) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "Handler interface is missing required default method: Path resolvePath(String)", ctx.handlerInterfaceTypeElement);
        }
        String defaultMissingErrorMessage = getDefaultMissingErrorMessage(processingEnv, type);
        classBuilder.addField(FieldSpec.builder(Path.class, "path", Modifier.PRIVATE, Modifier.FINAL)
                .initializer("resolvePath($S)", name)
                .build());
        String basePackage = ParamUtils.allOrNothing(ctx.params, "base_package");
        HashMap<String, Boolean> options = new HashMap<>();
        parseOptions(ctx.params.get("options").toArray(new String[0]), options);
        TypeName configType = ctx.configType;
        ParameterSpec.Builder pathParamBuilder = ParameterSpec.builder(Path.class, "path");
        if (ctx.nonNullAnnotation != null)
            pathParamBuilder.addAnnotation(ctx.nonNullAnnotation);
        MethodSpec.Builder readMethodBuilder = MethodSpec.methodBuilder("read")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(pathParamBuilder.build())
                .returns(configType)
                .addException(IOException.class);
        if (ctx.nonNullAnnotation != null)
            readMethodBuilder.addAnnotation(ctx.nonNullAnnotation);
        ParameterSpec.Builder configParamBuilder = ParameterSpec.builder(configType, "config");
        if (ctx.nonNullAnnotation != null)
            configParamBuilder.addAnnotation(ctx.nonNullAnnotation);
        MethodSpec.Builder writeMethodBuilder = MethodSpec.methodBuilder("write")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(configParamBuilder.build())
                .addParameter(pathParamBuilder.build())
                .addException(IOException.class);
        SerializerContext sCtx = new SerializerContext(configType, basePackage, options,
                readMethodBuilder, writeMethodBuilder,
                defaultMissingErrorMessage, ctx.nonNullAnnotation, ctx.nullableAnnotation);
        provider.process(name, type, fields, sCtx, classBuilder);
        classBuilder.addMethod(readMethodBuilder.build()).addMethod(writeMethodBuilder.build());
        ctx.resetMethodBuilder.addCode("save();\n");

        CodeBlock.Builder loadCodeBuilder = CodeBlock.builder()
                .beginControlFlow("try")
                .addStatement("config = read(path)")
                .nextControlFlow("catch ($T e)", NoSuchFileException.class)
                .addStatement("log($T.INFO, $S + path + $S, null)",
                        LogLevel.class, "File \"", "\" does not exist, loading default values")
                .nextControlFlow("catch ($T e)", IOException.class)
                .addStatement("log($T.ERROR, $S + path + $S, e)",
                        LogLevel.class, "Failed to read from config file at \"", "\"!");
        boolean crashOnFail = options.getOrDefault("crashOnFail", false);
        if (options.getOrDefault("backupOnFail", true)) {
            loadCodeBuilder
                    .addStatement("$T backupPath = $T.resolveTimestampedSibling(path, $S)",
                            Path.class, PathUtils.class, "BACKUP")
                    .addStatement("boolean backupSuccess = true");
            loadCodeBuilder
                    .beginControlFlow("try")
                    .addStatement("$T.move(path, backupPath, $T.REPLACE_EXISTING)",
                            Files.class, StandardCopyOption.class)
                    .nextControlFlow("catch ($T be)", IOException.class)
                    .addStatement("log($T.ERROR, $S + backupPath + $S, be)",
                            LogLevel.class, "Failed to back up config file to \"", "\"!")
                    .addStatement("backupSuccess = false")
                    .endControlFlow();
            if (crashOnFail) {
                loadCodeBuilder
                        .beginControlFlow("if (backupSuccess)")
                        .addStatement("reset()")
                        .addStatement("save()")
                        .addStatement("throw new $T($S + path + $S + backupPath + $S)",
                                RuntimeException.class, "Failed to read from config file \"",
                                "\"! File has been replaced with default values, with the original backed up at \"", "\"")
                        .nextControlFlow("else")
                        .addStatement("throw new $T($S + path + $S)",
                                RuntimeException.class, "Failed to read from config file \"", "\"!")
                        .endControlFlow();
            } else
                loadCodeBuilder
                        .beginControlFlow("if (backupSuccess)")
                        .addStatement("log($T.INFO, $S + backupPath + $S, null)",
                                LogLevel.class, "Backed up config file to \"", "\"")
                        .endControlFlow()
                        .addStatement("log($T.WARN, $S, null)",
                                LogLevel.class, "Loading and saving default config values")
                        .addStatement("reset()")
                        .addStatement("save()");
        } else if (crashOnFail) {
            loadCodeBuilder.addStatement("throw new $T($S + path + $S)",
                    RuntimeException.class, "Failed to read from config file \"", "\"!");
        }
        loadCodeBuilder.endControlFlow();
        ctx.loadMethodBuilder.addCode(loadCodeBuilder.build());

        // FIXME find better name for this
        boolean saveToTempAndMoveOver = options.getOrDefault("saveToTempAndMoveOver", true);
        CodeBlock.Builder saveCodeBuilder = CodeBlock.builder();
        if (saveToTempAndMoveOver) {
            saveCodeBuilder
                    .addStatement("$T tempPath = $T.resolveTimestampedSibling(path, $S)",
                            Path.class, PathUtils.class, "TEMP")
                    .addStatement("log($T.INFO, $S + tempPath + $S, null)",
                            LogLevel.class, "Writing config to temp file \"", "\"")
                    .beginControlFlow("try")
                    .addStatement("write(config, tempPath)")
                    .nextControlFlow("catch ($T e)", IOException.class)
                    .addStatement("log($T.ERROR, $S + tempPath + $S, e)",
                            LogLevel.class, "Failed to write to temp file at \"", "\"!")
                    .addStatement("return")
                    .endControlFlow()
                    .addStatement("log($T.INFO, $S + tempPath + $S + path + $S, null)",
                            LogLevel.class, "Moving temp file \"", "\" over config file \"", "\"")
                    .beginControlFlow("try")
                    .addStatement("$T.move(tempPath, path, $T.REPLACE_EXISTING)",
                            Files.class, StandardCopyOption.class)
                    .nextControlFlow("catch ($T e)", IOException.class)
                    .addStatement("log($T.ERROR, $S + tempPath + $S + path + $S, e)",
                            LogLevel.class, "Failed to move temp file \"", "\" over config file at \"", "\"!")
                    .endControlFlow();
        } else {
            saveCodeBuilder
                    .addStatement("log($T.INFO, $S + path + $S, null)",
                            LogLevel.class, "Writing config to file \"", "\"")
                    .beginControlFlow("try")
                    .addStatement("write(config, path)")
                    .nextControlFlow("catch ($T e)", IOException.class)
                    .addStatement("log($T.ERROR, $S + path + $S, e)",
                            LogLevel.class, "Failed to write to config file at \"", "\"!")
                    .endControlFlow();
        }
        ctx.saveMethodBuilder.addCode(saveCodeBuilder.build());
    }

    public static void parseOptions(@NotNull String[] options, @NotNull Map<@NotNull String, @NotNull Boolean> map) {
        for (String option : options) {
            if (option.isEmpty())
                continue;
            char first = option.charAt(0);
            boolean enabled = first != '-';
            if (!enabled || first == '+')
                option = option.substring(1);
            map.put(option, enabled);
        }
    }

    private static final HashMap<VariableElement, String> SERIALIZED_NAME_CACHE = new HashMap<>();

    public static @NotNull String getSerializedName(@NotNull VariableElement field) {
        return SERIALIZED_NAME_CACHE.computeIfAbsent(field, variableElement -> {
            SerializedName serializedName = variableElement.getAnnotation(SerializedName.class);
            if (serializedName != null)
                return serializedName.value();
            else
                return nameProvider.translate(nameProviderVariant, variableElement);
        });
    }

    private static final String[] NO_ALIASES = new String[0];
    private static final HashMap<VariableElement, String[]> SERIALIZED_ALIASES_CACHE = new HashMap<>();

    public static @NotNull String @NotNull [] getSerializedAliases(@NotNull VariableElement field) {
        return SERIALIZED_ALIASES_CACHE.computeIfAbsent(field, variableElement -> {
            SerializedAliases serializedAliases = variableElement.getAnnotation(SerializedAliases.class);
            if (serializedAliases != null)
                return serializedAliases.value();
            else
                return NO_ALIASES;
        });
    }

    private static final HashMap<TypeElement, String> DMEM_CACHE = new HashMap<>();

    public static @Nullable String getDefaultMissingErrorMessage(@NotNull ProcessingEnvironment processingEnv, @NotNull TypeElement type) {
        return DMEM_CACHE.computeIfAbsent(type, typeElement -> {
            String defaultMissingErrorMessage = "Missing field \"%s\"!";
            if (typeElement.getAnnotation(UseDefaultIfMissing.class) != null)
                defaultMissingErrorMessage = null;
            else {
                ThrowIfMissing throwIfMissing = typeElement.getAnnotation(ThrowIfMissing.class);
                if (throwIfMissing != null) {
                    String[] defaultMissingErrorMessageIn = throwIfMissing.message();
                    if (defaultMissingErrorMessageIn.length == 1)
                        defaultMissingErrorMessage = defaultMissingErrorMessageIn[0];
                    else if (defaultMissingErrorMessageIn.length > 1) {
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                                "Class specifies more than one error message in @ThrowIfMissing", typeElement);
                        return null;
                    }
                }
            }
            return defaultMissingErrorMessage;
        });
    }

    public static void getMissingErrorMessages(@NotNull ProcessingEnvironment processingEnv,
                                               @NotNull List<@NotNull VariableElement> fields,
                                               @Nullable String defaultMissingErrorMessage,
                                               @NotNull Map<@NotNull String, @Nullable String> result) {
        for (VariableElement field : fields) {
            String fieldMissingErrorMessage = "Missing field \"%s\"!";
            if (field.getAnnotation(UseDefaultIfMissing.class) != null)
                fieldMissingErrorMessage = null;
            else {
                ThrowIfMissing throwIfMissing = field.getAnnotation(ThrowIfMissing.class);
                if (throwIfMissing == null)
                    fieldMissingErrorMessage = defaultMissingErrorMessage;
                else {
                    String[] fieldMissingErrorMessageIn = throwIfMissing.message();
                    if (fieldMissingErrorMessageIn.length == 1)
                        fieldMissingErrorMessage = fieldMissingErrorMessageIn[0];
                    else if (fieldMissingErrorMessageIn.length > 1) {
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                                "Field specifies more than one error message in @ThrowIfMissing", field);
                        continue;
                    }
                }
            }
            result.put(SerializerComponentProvider.getSerializedName(field), fieldMissingErrorMessage);
        }
    }

    public final static class EnumKeyType {
        public final @NotNull TypeMirror type;
        private final @NotNull String serializer;
        private final @NotNull String deserializer;
        public final boolean keyed;
        private final @NotNull List<Object> deserializerArgs;
        private @Nullable Object[] deserializerArgsArr;

        private EnumKeyType(@NotNull TypeMirror type, @NotNull String serializer, @NotNull String deserializer, boolean keyed,
                            @NotNull List<Object> deserializerArgs) {
            this.type = type;
            this.serializer = serializer;
            this.deserializer = deserializer;
            this.keyed = keyed;
            this.deserializerArgs = deserializerArgs;
        }

        public @NotNull CodeBlock generateSerializer(@NotNull String dest) {
            return CodeBlock.builder()
                    .add(serializer, dest)
                    .build();
        }

        public @NotNull CodeBlock generateDeserializer(@NotNull String src) {
            if (deserializerArgsArr == null) {
                int max = deserializerArgs.size();
                deserializerArgsArr = new Object[max + 1];
                for (int i = 0; i < max; i++)
                    deserializerArgsArr[i + 1] = deserializerArgs.get(i);
            }
            deserializerArgsArr[0] = src;
            return CodeBlock.builder()
                    .add(deserializer, deserializerArgsArr)
                    .build();
        }

        private static @NotNull EnumKeyType keyed(@NotNull TypeMirror type, @NotNull String deserializer,
                                                 @NotNull List<Object> deserializerArgs) {
            return new EnumKeyType(type, "$L.getId()", deserializer, true, deserializerArgs);
        }

        public static @NotNull EnumKeyType simple(@NotNull TypeMirror type, @NotNull String serializer, @NotNull String deserializer,
                                                  @NotNull List<Object> deserializerArgs) {
            return new EnumKeyType(type, serializer, deserializer, false, deserializerArgs);
        }
    }
    private static final HashMap<TypeElement, EnumKeyType> ENUM_KEY_TYPE_CACHE = new HashMap<>();

    public static @Nullable EnumKeyType getEnumKeyType(@NotNull ProcessingEnvironment processingEnv,
                                                       @NotNull TypeElement type,
                                                       @NotNull TypeSpec.Builder classBuilder) {
        final Types types = processingEnv.getTypeUtils();
        return ENUM_KEY_TYPE_CACHE.computeIfAbsent(type, typeElement -> {
            for (TypeMirror anInterface : typeElement.getInterfaces()) {
                TypeMirror erasedInterface = types.erasure(anInterface);
                if (types.isSameType(erasedInterface, keyedEnumTM)) {
                    List<? extends TypeMirror> typeArgs = ((DeclaredType) anInterface).getTypeArguments();
                    if (typeArgs.isEmpty()) {
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                                "Serializer: Raw keyed enum not supported", typeElement);
                        return null;
                    }
                    TypeMirror keyType = typeArgs.get(0);
                    ExecutableElement enumDeserializerMethod = findEnumDeserializer(processingEnv, type, keyType);
                    String deserializer;
                    ArrayList<Object> deserializerArgs = new ArrayList<>();
                    if (enumDeserializerMethod == null) {
                        deserializer = "$2L.get($1L)";
                        deserializerArgs.add(addEnumMap(classBuilder, keyType, "%s.getKey()", true, type));
                    } else {
                        deserializer = "$2T.$3L($1L)";
                        deserializerArgs.add(ClassName.get(type));
                        deserializerArgs.add(enumDeserializerMethod.getSimpleName().toString());
                    }
                    return EnumKeyType.keyed(typeArgs.get(0), deserializer, deserializerArgs);
                }
            }
            // see if we can find a public getId() method
            for (ExecutableElement method : ElementFilter.methodsIn(typeElement.getEnclosedElements())) {
                Set<Modifier> modifiers = method.getModifiers();
                if (modifiers.contains(Modifier.PUBLIC)
                        && method.getParameters().isEmpty()
                        && method.getSimpleName().contentEquals("getId")) {
                    TypeMirror keyType = method.getReturnType();
                    if (keyType.getKind().isPrimitive())
                        keyType = types.boxedClass((PrimitiveType) keyType).asType();
                    ArrayList<Object> deserializerArgs = new ArrayList<>();
                    deserializerArgs.add(addEnumMap(classBuilder, keyType, "%s.getId()", false, type));
                    return EnumKeyType.simple(keyType, "$L.getId()", "$2L.get($1L)", deserializerArgs);
                }
            }
            // see if we can find a public final id field
            for (VariableElement field : TypeUtils.fieldsIn(typeElement.getEnclosedElements())) {
                if (field.getKind() == ElementKind.ENUM_CONSTANT)
                    continue;
                Set<Modifier> modifiers = field.getModifiers();
                if (modifiers.contains(Modifier.PUBLIC) && modifiers.contains(Modifier.FINAL)
                        && field.getSimpleName().contentEquals("id")) {
                    TypeMirror keyType = field.asType();
                    if (keyType.getKind().isPrimitive())
                        keyType = types.boxedClass((PrimitiveType) keyType).asType();
                    ArrayList<Object> deserializerArgs = new ArrayList<>();
                    deserializerArgs.add(addEnumMap(classBuilder, keyType, "%s.id", false, type));
                    return EnumKeyType.simple(keyType, "$L.id", "$2L.get($1L)", deserializerArgs);
                }
            }
            return null;
        });
    }

    private static @Nullable ExecutableElement findEnumDeserializer(@NotNull ProcessingEnvironment processingEnv,
                                                                    @NotNull TypeElement type,
                                                                    @NotNull TypeMirror keyType) {
        final Types types = processingEnv.getTypeUtils();
        TypeMirror unboxedKeyType = keyType;
        if (TypeName.get(keyType).isBoxedPrimitive())
            unboxedKeyType = types.unboxedType(keyType);
        for (ExecutableElement method : ElementFilter.methodsIn(type.getEnclosedElements())) {
            if (method.getAnnotation(KeyedEnumDeserializer.class) == null)
                continue;
            Set<Modifier> modifiers = method.getModifiers();
            List<? extends VariableElement> params = method.getParameters();
            if (params.size() == 1) {
                TypeMirror paramType = params.get(0).asType();
                if (types.isSameType(keyType, paramType)
                    || types.isSameType(unboxedKeyType, paramType)) {
                    if (modifiers.contains(Modifier.PUBLIC) && modifiers.contains(Modifier.STATIC)
                            && types.isSameType(type.asType(), method.getReturnType()))
                        return method;
                }
            }
            processingEnv.getMessager().printMessage(Diagnostic.Kind.MANDATORY_WARNING,
                    "Serializer: Invalid @KeyedEnumDeserializer method. Should be public and static, return the enum type and take the key type as its only parameter",
                    method);
        }
        return null;
    }

    private static @NotNull String addEnumMap(@NotNull TypeSpec.Builder classBuilder,
                                             @NotNull TypeMirror keyType,
                                             @NotNull String serializer,
                                             boolean keyed,
                                             @NotNull TypeElement valueTypeElement) {
        TypeName keyTypeName = TypeName.get(keyType);
        TypeName valueTypeName = TypeName.get(valueTypeElement.asType());
        String mapName = "MAP_" + StringUtils.camelCaseToSnakeCase(valueTypeElement.getSimpleName().toString()).toUpperCase(Locale.ROOT);
        TypeName mapType = ParameterizedTypeName.get(MAP_NAME, keyTypeName, valueTypeName);
        classBuilder.addField(FieldSpec.builder(mapType, mapName, Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL).build());
        CodeBlock.Builder codeBuilder = CodeBlock.builder();
        codeBuilder
                .addStatement("$L = new $T()", mapName, mapType)
                .beginControlFlow("for ($1T e : $1T.values())", valueTypeName)
                .addStatement("$L.put($L, e)", mapName, String.format(serializer, "e"));
        if (keyed) {
            codeBuilder
                    .beginControlFlow("for ($T a : e.getAliases())", keyTypeName)
                    .addStatement("$L.put(a, e)", mapName)
                    .endControlFlow();
        }
        classBuilder.addStaticBlock(codeBuilder
                .endControlFlow()
                .build());
        return mapName;
    }

    public static @NotNull String getDefaultValue(@NotNull TypeName type) {
        if (STRING_NAME.equals(type))
            return "\"\"";
        else if (TypeName.BOOLEAN.equals(type))
            return "false";
        else if (type.isPrimitive())
            return "0";
        else
            return "null";
    }
}
