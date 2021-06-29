package io.github.speedbridgemc.config.processor.impl.type.provider;

import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.TypeName;
import io.github.speedbridgemc.config.Config;
import io.github.speedbridgemc.config.EnumName;
import io.github.speedbridgemc.config.processor.api.naming.NamingStrategy;
import io.github.speedbridgemc.config.processor.api.property.ConfigPropertyExtensionFinder;
import io.github.speedbridgemc.config.processor.api.type.ConfigStruct;
import io.github.speedbridgemc.config.processor.api.type.ConfigType;
import io.github.speedbridgemc.config.processor.api.type.ConfigTypeKind;
import io.github.speedbridgemc.config.processor.api.type.provider.ConfigTypeProvider;
import io.github.speedbridgemc.config.processor.api.type.provider.StructFactory;
import io.github.speedbridgemc.config.processor.api.util.Lazy;
import io.github.speedbridgemc.config.processor.api.util.MirrorElementPair;
import io.github.speedbridgemc.config.processor.api.util.MirrorUtils;
import io.github.speedbridgemc.config.processor.impl.type.ConfigTypeImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.SimpleTypeVisitor8;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

public final class ConfigTypeProviderImpl implements ConfigTypeProvider {
    private class StructFactoryContextImpl implements StructFactory.Context {
        @Override
        public @NotNull ConfigTypeProvider typeProvider() {
            return ConfigTypeProviderImpl.this;
        }

        @Override
        public @NotNull String name(@NotNull String originalName) {
            return namingStrategy.name(namingStrategyVariant, originalName);
        }

        @Override
        public void findExtensions(ConfigPropertyExtensionFinder.@NotNull Callback mapBuilder,
                                   @NotNull MirrorElementPair @NotNull ... pairs) {
            for (ConfigPropertyExtensionFinder finder : extensionFinders)
                finder.findExtensions(mapBuilder, pairs);
        }
    }

    private boolean initialized = false;
    private ProcessingEnvironment processingEnv;
    private Messager messager;
    private Elements elements;
    private Types types;
    private TypeMirror stringTM, enumTM, collectionTM, mapTM;
    private TypeElement mapTE;

    private NamingStrategy namingStrategy;
    private String namingStrategyVariant;

    private ConfigType boolCType, byteCType, shortCType, intCType, longCType, charCType, floatCType, doubleCType, stringCType;

    private final StructFactoryContextImpl structFactoryCtx = new StructFactoryContextImpl();
    private final HashMap<TypeMirror, Config.StructOverride> structOverrides = new HashMap<>();
    private final ArrayList<StructFactory> structFactories = new ArrayList<>();
    private final ArrayList<ConfigPropertyExtensionFinder> extensionFinders = new ArrayList<>();

    @Override
    public void init(@NotNull ProcessingEnvironment processingEnv) {
        if (initialized)
            throw new IllegalStateException("Already initialized!");
        initialized = true;
        this.processingEnv = processingEnv;
        messager = processingEnv.getMessager();
        elements = processingEnv.getElementUtils();
        types = processingEnv.getTypeUtils();
        stringTM = MirrorUtils.getDeclaredType(elements, String.class);
        enumTM = types.erasure(MirrorUtils.getDeclaredType(elements, Enum.class));
        collectionTM = types.erasure(MirrorUtils.getDeclaredType(elements, Collection.class));
        mapTE = MirrorUtils.getTypeElement(elements, Map.class);
        mapTM = types.erasure(mapTE.asType());

        boolCType = new ConfigTypeImpl.Primitive(ConfigTypeKind.BOOL, "bool",
                types.getPrimitiveType(TypeKind.BOOLEAN));
        byteCType = new ConfigTypeImpl.Primitive(ConfigTypeKind.BYTE, "byte",
                types.getPrimitiveType(TypeKind.BYTE));
        shortCType = new ConfigTypeImpl.Primitive(ConfigTypeKind.SHORT, "short",
                types.getPrimitiveType(TypeKind.SHORT));
        intCType = new ConfigTypeImpl.Primitive(ConfigTypeKind.INT, "int",
                types.getPrimitiveType(TypeKind.INT));
        longCType = new ConfigTypeImpl.Primitive(ConfigTypeKind.LONG, "long",
                types.getPrimitiveType(TypeKind.LONG));
        charCType = new ConfigTypeImpl.Primitive(ConfigTypeKind.CHAR, "char",
                types.getPrimitiveType(TypeKind.CHAR));
        floatCType = new ConfigTypeImpl.Primitive(ConfigTypeKind.FLOAT, "float",
                types.getPrimitiveType(TypeKind.FLOAT));
        doubleCType = new ConfigTypeImpl.Primitive(ConfigTypeKind.DOUBLE, "double",
                types.getPrimitiveType(TypeKind.DOUBLE));
        stringCType = new ConfigTypeImpl.Primitive(ConfigTypeKind.STRING, "string",
                stringTM);
    }

    @Override
    public @NotNull ConfigType primitiveOf(@NotNull ConfigTypeKind kind) {
        switch (kind) {
        case BOOL:
            return boolCType;
        case BYTE:
            return byteCType;
        case SHORT:
            return shortCType;
        case INT:
            return intCType;
        case LONG:
            return longCType;
        case CHAR:
            return charCType;
        case FLOAT:
            return floatCType;
        case DOUBLE:
            return doubleCType;
        case STRING:
            return stringCType;
        }
        throw new IllegalArgumentException("Kind " + kind + " is not primitive!");
    }

    @Override
    public @NotNull ConfigType arrayOf(@NotNull ConfigType componentType) {
        return new ConfigTypeImpl.Array(types.getArrayType(componentType.asMirror()), Lazy.of(componentType));
    }

    @Override
    public @NotNull ConfigType mapOf(@NotNull ConfigType keyType, @NotNull ConfigType valueType) {
        return new ConfigTypeImpl.Map(types.getDeclaredType(mapTE, keyType.asMirror(), valueType.asMirror()),
                Lazy.of(keyType), Lazy.of(valueType));
    }

    @Override
    public void addStruct(@NotNull ConfigType type) {
        if (type.kind() != ConfigTypeKind.STRUCT)
            throw new IllegalArgumentException("Injected type must be of kind " + ConfigTypeKind.STRUCT + "!");
        DeclaredType mirror = (DeclaredType) type.asMirror();
        ConfigType oldType = declaredTypeCache.put(mirror, type);
        if (oldType != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            pw.println("Pre-existing type for mirror \"" + mirror + "\" replaced!");
            pw.println("Was: " + oldType);
            pw.println("Replaced by: " + type);
            try {
                throw new Exception("Stack trace");
            } catch (Exception e) {
                e.printStackTrace(pw);
            }
            messager.printMessage(Diagnostic.Kind.MANDATORY_WARNING, sw.toString());
        }
    }

    @Override
    public void addExtensionFinder(@NotNull ConfigPropertyExtensionFinder extensionFinder) {
        extensionFinders.add(extensionFinder);
    }

    @Override
    public void setNamingStrategy(@NotNull NamingStrategy strategy, @NotNull String variant) {
        this.namingStrategy = strategy;
        this.namingStrategyVariant = variant;
    }

    @Override
    public void setStructOverride(@NotNull DeclaredType mirror, @Nullable Config.StructOverride structOverride) {
        structOverrides.put(types.erasure(mirror), structOverride);
    }

    @Override
    public void addStructFactory(@NotNull StructFactory structFactory) {
        structFactories.add(structFactory);
    }

    @Override
    public @NotNull ConfigType fromMirror(@NotNull TypeMirror mirror) {
        if (types.isSameType(mirror, stringTM))
            return stringCType;
        switch (mirror.getKind()) {
        case BOOLEAN:
            return boolCType;
        case BYTE:
            return byteCType;
        case SHORT:
            return shortCType;
        case INT:
            return intCType;
        case LONG:
            return longCType;
        case CHAR:
            return charCType;
        case FLOAT:
            return floatCType;
        case DOUBLE:
            return doubleCType;
        case ARRAY:
            return arrayOf(fromMirror(((ArrayType) mirror).getComponentType()));
        case DECLARED:
            DeclaredType declaredType = (DeclaredType) mirror;
            ConfigType configType = declaredTypeCache.get(declaredType);
            if (configType == null) {
                configType = fromDeclaredType(declaredType);
                declaredTypeCache.put(declaredType, configType);
            }
            return configType;
        default:
            throw new IllegalArgumentException("Mirror " + mirror + " cannot be represented as config type!");
        }
    }

    private final @NotNull HashMap<DeclaredType, ConfigType> declaredTypeCache = new HashMap<>();

    private @NotNull ConfigType fromDeclaredType(@NotNull DeclaredType mirror) {
        TypeMirror mirrorErasure = types.erasure(mirror);

        // 5 possible cases (6 if you count String, but that's handled in fromMirror)
        // 1. enum (subtype of Enum) - type of kind ENUM
        if (types.isAssignable(mirrorErasure, enumTM)) {
            ImmutableList.Builder<String> constantsBuilder = ImmutableList.builder();
            for (VariableElement field : ElementFilter.fieldsIn(mirror.asElement().getEnclosedElements())) {
                if (field.getKind() != ElementKind.ENUM_CONSTANT)
                    break;
                String constName;
                EnumName nameAnno = field.getAnnotation(EnumName.class);
                if (nameAnno != null && !nameAnno.value().isEmpty())
                    constName = nameAnno.value();
                else
                    constName = namingStrategy.name(namingStrategyVariant, field.getSimpleName().toString());
                constantsBuilder.add(constName);
            }
            return new ConfigTypeImpl.Enum(mirror, constantsBuilder.build());
        }
        // 2. subtype of Collection - type of kind ARRAY
        if (types.isAssignable(mirrorErasure, collectionTM)) {
            TypeMirror compType = mirror.accept(collectionTypeArgFinder, null);
            if (compType == null)
                throw new RuntimeException("Got null component type (T in Collection<T>) for " + mirror);
            return new ConfigTypeImpl.Array(mirror, Lazy.wrap(() -> fromMirror(compType)));
        }
        // 3. subtype of Map - type of kind MAP
        if (types.isAssignable(mirrorErasure, mapTM)) {
            MapTypeArgs typeArgs = mirror.accept(mapTypeArgsFinder, null);
            if (typeArgs == null)
                throw new RuntimeException("Got null key/value types (K and V in Map<K, V>) for " + mirror);
            return new ConfigTypeImpl.Map(mirror, Lazy.wrap(() -> fromMirror(typeArgs.keyType)), Lazy.wrap(() -> fromMirror(typeArgs.valueType)));
        }
        TypeName typeName = TypeName.get(mirror).withoutAnnotations();
        // 4. boxed primitives - type of kind (unboxed primitive)
        if (typeName.isBoxedPrimitive()) {
            typeName = typeName.unbox();
            if (typeName.equals(TypeName.BOOLEAN))
                return boolCType;
            else if (typeName.equals(TypeName.BYTE))
                return byteCType;
            else if (typeName.equals(TypeName.SHORT))
                return shortCType;
            else if (typeName.equals(TypeName.INT))
                return intCType;
            else if (typeName.equals(TypeName.LONG))
                return longCType;
            else if (typeName.equals(TypeName.CHAR))
                return charCType;
            else if (typeName.equals(TypeName.FLOAT))
                return floatCType;
            else if (typeName.equals(TypeName.DOUBLE))
                return doubleCType;
            throw new RuntimeException("Unknown primitive " + typeName + "!");
        }
        // 5. anything else - type of kind STRUCT (delegated to StructFactory)
        Config.StructOverride structOverride = structOverrides.get(mirrorErasure);
        Optional<ConfigStruct> struct = Optional.empty();
        for (StructFactory structFactory : structFactories) {
            struct = structFactory.createStruct(structFactoryCtx, mirror, structOverride);
            if (struct.isPresent())
                break;
        }
        return struct.orElseThrow(() -> new RuntimeException("Failed to create struct config type from \"" + mirror + "\"!"));
    }

    private final @NotNull CollectionTypeArgFinder collectionTypeArgFinder = new CollectionTypeArgFinder();
    private final class CollectionTypeArgFinder extends SimpleTypeVisitor8<TypeMirror, Void> {
        @Override
        public TypeMirror visitDeclared(DeclaredType t, Void unused) {
            if (types.isSameType(types.erasure(t), collectionTM)) {
                List<? extends TypeMirror> typeArgs = t.getTypeArguments();
                if (typeArgs.isEmpty())
                    // TODO proper error
                    throw new RuntimeException("Raw Collection not supported!");
                return typeArgs.get(0);
            }
            for (TypeMirror supertype : types.directSupertypes(t)) {
                if (supertype.getKind() == TypeKind.DECLARED) {
                    TypeMirror typeArg = visitDeclared((DeclaredType) supertype, null);
                    if (typeArg != null)
                        return typeArg;
                }
            }
            return null;
        }
    }

    private static final class MapTypeArgs {
        public final @NotNull TypeMirror keyType, valueType;

        public MapTypeArgs(@NotNull TypeMirror keyType, @NotNull TypeMirror valueType) {
            this.keyType = keyType;
            this.valueType = valueType;
        }
    }

    private final @NotNull MapTypeArgsFinder mapTypeArgsFinder = new MapTypeArgsFinder();
    private final class MapTypeArgsFinder extends SimpleTypeVisitor8<MapTypeArgs, Void> {
        @Override
        public MapTypeArgs visitDeclared(DeclaredType t, Void unused) {
            if (types.isSameType(types.erasure(t), mapTM)) {
                List<? extends TypeMirror> typeArgs = t.getTypeArguments();
                if (typeArgs.isEmpty())
                    // TODO proper error
                    throw new RuntimeException("Raw Map not supported!");
                return new MapTypeArgs(typeArgs.get(0), typeArgs.get(1));
            }
            for (TypeMirror supertype : types.directSupertypes(t)) {
                if (supertype.getKind() == TypeKind.DECLARED) {
                    MapTypeArgs typeArgs = visitDeclared((DeclaredType) supertype, null);
                    if (typeArgs != null)
                        return typeArgs;
                }
            }
            return null;
        }
    }
}
