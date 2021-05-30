package io.github.speedbridgemc.config.processor.impl.type;

import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.TypeName;
import io.github.speedbridgemc.config.processor.api.type.ConfigType;
import io.github.speedbridgemc.config.processor.api.type.ConfigTypeKind;
import io.github.speedbridgemc.config.processor.api.type.ConfigTypeProvider;
import org.jetbrains.annotations.NotNull;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.*;
import javax.lang.model.util.Elements;
import javax.lang.model.util.SimpleTypeVisitor8;
import javax.lang.model.util.Types;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ConfigTypeProviderImpl implements ConfigTypeProvider {
    private boolean initialized = false;
    private ProcessingEnvironment processingEnv;
    private Elements elements;
    private Types types;
    private TypeMirror stringTM, collectionTM, mapTM;
    private TypeElement mapTE;

    private ConfigType boolCType, byteCType, shortCType, intCType, longCType, charCType, floatCType, doubleCType, stringCType;

    @Override
    public void init(@NotNull ProcessingEnvironment processingEnv) {
        if (initialized)
            throw new IllegalStateException("Already initialized!");
        initialized = true;
        this.processingEnv = processingEnv;
        elements = processingEnv.getElementUtils();
        types = processingEnv.getTypeUtils();
        stringTM = elements.getTypeElement(String.class.getCanonicalName()).asType();
        collectionTM = types.erasure(elements.getTypeElement(Collection.class.getCanonicalName()).asType());
        mapTE = elements.getTypeElement(Map.class.getCanonicalName());
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
        return new ConfigTypeImpl.Array(types.getArrayType(componentType.asMirror()),
                componentType);
    }

    @Override
    public @NotNull ConfigType mapOf(@NotNull ConfigType keyType, @NotNull ConfigType valueType) {
        return new ConfigTypeImpl.Map(types.getDeclaredType(mapTE, keyType.asMirror(), valueType.asMirror()),
                keyType, valueType);
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

        // 3 possible cases (4 if you count String, but that's handled in fromMirror)
        // 1. subtype of Collection - type of kind ARRAY
        if (types.isAssignable(mirrorErasure, collectionTM)) {
            TypeMirror compType = mirror.accept(collectionTypeArgFinder, null);
            if (compType == null)
                throw new RuntimeException("Got null component type (T in Collection<T>) for " + mirror);
            return new ConfigTypeImpl.Array(mirror, fromMirror(compType));
        }
        // 2. subtype of Map - type of kind MAP
        if (types.isAssignable(mirrorErasure, mapTM)) {
            MapTypeArgs typeArgs = mirror.accept(mapTypeArgsFinder, null);
            if (typeArgs == null)
                throw new RuntimeException("Got null key/value types (K and V in Map<K, V>) for " + mirror);
            return new ConfigTypeImpl.Map(mirror, fromMirror(typeArgs.keyType), fromMirror(typeArgs.valueType));
        }
        // 3. anything else - type of kind STRUCT
        TypeName typeName = TypeName.get(mirror).withoutAnnotations();
        // FIXME actually implement this - right now this reports every class as a stub with a public 0-arg constructor
        //  and no properties
        return new ConfigTypeImpl.Struct(mirror, new StructInstantiationStrategyImpl.Constructor(ImmutableList.of(), typeName), ImmutableList.of());
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
