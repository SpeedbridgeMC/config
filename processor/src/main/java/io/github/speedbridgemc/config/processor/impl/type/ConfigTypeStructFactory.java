package io.github.speedbridgemc.config.processor.impl.type;

import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedHashMultimap;
import com.squareup.javapoet.TypeName;
import io.github.speedbridgemc.config.Config;
import io.github.speedbridgemc.config.processor.api.property.ConfigProperty;
import io.github.speedbridgemc.config.processor.api.property.ConfigPropertyExtension;
import io.github.speedbridgemc.config.processor.api.property.ConfigPropertyExtensionFinder;
import io.github.speedbridgemc.config.processor.api.type.ConfigType;
import io.github.speedbridgemc.config.processor.api.type.StructInstantiationStrategy;
import io.github.speedbridgemc.config.processor.api.util.AnnotationUtils;
import io.github.speedbridgemc.config.processor.api.util.MirrorElementPair;
import io.github.speedbridgemc.config.processor.api.util.PropertyUtils;
import io.github.speedbridgemc.config.processor.impl.property.ConfigPropertyImpl;
import org.jetbrains.annotations.NotNull;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.util.*;
import java.util.function.Function;

final class ConfigTypeStructFactory {
    private final ConfigTypeProviderImpl typeProvider;
    private final ProcessingEnvironment processingEnv;
    private final Elements elements;
    private final Types types;

    private final TypeMirror booleanTM, voidTM;

    private final ArrayList<ConfigPropertyExtensionFinder> extensionFinders;

    public ConfigTypeStructFactory(ConfigTypeProviderImpl typeProvider, ProcessingEnvironment processingEnv) {
        this.typeProvider = typeProvider;
        this.processingEnv = processingEnv;
        elements = processingEnv.getElementUtils();
        types = processingEnv.getTypeUtils();

        booleanTM = elements.getTypeElement(Boolean.class.getCanonicalName()).asType();
        voidTM = elements.getTypeElement(Void.class.getCanonicalName()).asType();

        extensionFinders = new ArrayList<>();
    }

    public void addExtensionFinder(@NotNull ConfigPropertyExtensionFinder extensionFinder) {
        extensionFinders.add(extensionFinder);
    }

    private void findExtensions(@NotNull ImmutableClassToInstanceMap.Builder<ConfigPropertyExtension> mapBuilder,
                                @NotNull MirrorElementPair @NotNull ... pairs) {
        for (ConfigPropertyExtensionFinder finder : extensionFinders)
            finder.findExtensions(mapBuilder::put, pairs);
    }

    public @NotNull ConfigType create(@NotNull DeclaredType mirror) {
        class FieldData {
            public final @NotNull TypeMirror mirror;
            public final @NotNull VariableElement element;
            public final boolean isFinal;

            FieldData(@NotNull TypeMirror mirror, @NotNull VariableElement element, boolean isFinal) {
                this.mirror = mirror;
                this.element = element;
                this.isFinal = isFinal;
            }
        }
        class MethodData {
            public final @NotNull ExecutableType mirror;
            public final @NotNull ExecutableElement element;

            MethodData(@NotNull ExecutableType mirror, @NotNull ExecutableElement element) {
                this.mirror = mirror;
                this.element = element;
            }
        }
        class AccessorPairDef {
            public final @NotNull String name, getter, setter;
            public final @NotNull ExecutableElement definingMethod;

            AccessorPairDef(@NotNull String name, @NotNull String getter, @NotNull String setter, @NotNull ExecutableElement definingMethod) {
                this.name = name;
                this.getter = getter;
                this.setter = setter;
                this.definingMethod = definingMethod;
            }
        }
        class AccessorPair {
            public final @NotNull TypeMirror type;
            public final @NotNull ExecutableType getterM;
            public final ExecutableType setterM;
            public final boolean hasSetter;
            public final @NotNull ExecutableElement getterE;
            public final ExecutableElement setterE;

            AccessorPair(@NotNull TypeMirror type,
                         @NotNull ExecutableType getterM, @NotNull ExecutableType setterM,
                         @NotNull ExecutableElement getterE, @NotNull ExecutableElement setterE) {
                hasSetter = true;
                this.type = type;
                this.getterM = getterM;
                this.setterM = setterM;
                this.getterE = getterE;
                this.setterE = setterE;
            }

            AccessorPair(@NotNull TypeMirror type,
                         @NotNull ExecutableType getterM, @NotNull ExecutableElement getterE) {
                hasSetter = false;
                this.type = type;
                this.getterM = getterM;
                this.getterE = getterE;
                setterM = null;
                setterE = null;
            }
        }

        TypeElement te = (TypeElement) mirror.asElement();

        boolean includeFieldsByDefault = true;
        boolean includePropertiesByDefault = true;
        Config config = te.getAnnotation(Config.class);
        if (config != null) {
            includeFieldsByDefault = false;
            includePropertiesByDefault = false;
            for (Config.ScanTarget target : config.scanFor()) {
                switch (target) {
                case FIELDS:
                    includeFieldsByDefault = true;
                    break;
                case PROPERTIES:
                    includePropertiesByDefault = true;
                    break;
                }
            }
        }

        LinkedHashMap<String, FieldData> fields = new LinkedHashMap<>();
        LinkedHashMultimap<String, MethodData> methods = LinkedHashMultimap.create();

        // map all fields and methods
        // also collect explicit accessor property definitions
        HashSet<AccessorPairDef> accessorPairDefs = new HashSet<>();
        for (Element enclosed : te.getEnclosedElements()) {
            if (enclosed.getAnnotation(Config.Exclude.class) != null)
                continue;
            Set<Modifier> mods = enclosed.getModifiers();
            if (!mods.contains(Modifier.PUBLIC) || mods.contains(Modifier.STATIC) || mods.contains(Modifier.TRANSIENT))
                continue;
            TypeMirror enclosedM = types.asMemberOf(mirror, enclosed);  // fills in type variables!
                                                                        // also erases annotations, apparently
                                                                        // (which is why we also keep the element around)
            if (enclosed instanceof ExecutableElement) {
                methods.put(enclosed.getSimpleName().toString(), new MethodData((ExecutableType) enclosedM, (ExecutableElement) enclosed));
                Config.Property propAnno = enclosed.getAnnotation(Config.Property.class);
                if (propAnno != null)
                    accessorPairDefs.add(new AccessorPairDef(propAnno.name(), propAnno.getter(), propAnno.setter(), (ExecutableElement) enclosed));
            } else if (enclosed instanceof VariableElement) {
                fields.put(enclosed.getSimpleName().toString(), new FieldData(enclosedM, (VariableElement) enclosed, mods.contains(Modifier.FINAL)));
            }
        }

        ImmutableList.Builder<ConfigProperty> propertiesBuilder = ImmutableList.builder();
        HashSet<String> propertyNames = new HashSet<>();

        // fields
        for (Map.Entry<String, FieldData> field : fields.entrySet()) {
            TypeMirror fieldM = field.getValue().mirror;
            VariableElement fieldE = field.getValue().element;
            MirrorElementPair fieldMEP = new MirrorElementPair(fieldM, fieldE);
            Config.Property propAnno = fieldE.getAnnotation(Config.Property.class);
            String fieldName = field.getKey();
            String propName = "";
            if (propAnno == null) {
                if (!includeFieldsByDefault)
                    continue;
            } else
                propName = propAnno.name();
            if (propName.isEmpty())
                propName = typeProvider.name(fieldMEP);
            ConfigType fieldType = typeProvider.fromMirror(fieldM);
            ImmutableClassToInstanceMap.Builder<ConfigPropertyExtension> extensions = ImmutableClassToInstanceMap.builder();
            findExtensions(extensions, fieldMEP);
            propertiesBuilder.add(new ConfigPropertyImpl.Field(fieldType, propName, extensions.build(), !field.getValue().isFinal, fieldName));
            if (!propertyNames.add(propName))
                throw new RuntimeException("Duplicate property key \"" + propName + "\"!");
        }

        // properties (accessor pairs)

        HashMap<String, AccessorPair> accessorPairs = new HashMap<>();
        // discover and zip up implicit accessor pairs
        HashSet<String> implicitAccessorPairs = new HashSet<>();
        if (includePropertiesByDefault) {
            HashSet<String> methodsToSkip = new HashSet<>();
            for (Map.Entry<String, MethodData> entry : methods.entries()) {
                String methodName = entry.getKey();
                if (methodsToSkip.contains(methodName))
                    continue;
                ExecutableElement method = entry.getValue().element;
                if (method.getAnnotation(Config.Property.class) != null)
                    // handled later
                    continue;
                Optional<PropertyUtils.AccessorInfo> accessorInfo = PropertyUtils.getAccessorInfo(method);
                if (!accessorInfo.isPresent())
                    continue;
                TypeMirror propType = accessorInfo.get().propertyType;
                boolean isBool = isBool(accessorInfo.get().propertyType);
                String propName = PropertyUtils.getPropertyName(methodName, isBool);
                if (accessorPairs.containsKey(propName))
                    // TODO error details
                    throw new RuntimeException("Duplicate implicit property name \"" + propName + "\"!");
                switch (accessorInfo.get().kind) {
                case GETTER:
                    // try to find setter
                    boolean foundSetter = false;
                    String setterName = "";
                    ExecutableElement setter = null;
                    for (String possibleName : new String[]{PropertyUtils.makeSetterName(propName), propName}) {
                        setterName = possibleName;
                        for (MethodData setter1 : methods.get(setterName)) {
                            setter = setter1.element;
                            Optional<PropertyUtils.AccessorInfo> setterInfo = PropertyUtils.getAccessorInfo(setter);
                            if (!setterInfo.isPresent() || setterInfo.get().kind != PropertyUtils.AccessorInfo.Kind.SETTER)
                                continue;
                            if (!types.isSameType(propType, setterInfo.get().propertyType))
                                continue;
                            foundSetter = true;
                            break;
                        }
                        if (foundSetter)
                            break;
                    }
                    if (foundSetter) {
                        accessorPairs.put(propName, new AccessorPair(propType,
                                (ExecutableType) types.asMemberOf(mirror, method), (ExecutableType) types.asMemberOf(mirror, setter),
                                method, setter));
                        methodsToSkip.add(setterName);
                    } else {
                        accessorPairs.put(propName, new AccessorPair(propType,
                                (ExecutableType) types.asMemberOf(mirror, method), method));
                    }
                    implicitAccessorPairs.add(propName);
                    if (!propertyNames.add(propName))
                        throw new RuntimeException("Duplicate property key \"" + propName + "\"!");
                    break;
                case SETTER:
                    // try to find getter
                    boolean foundGetter = false;
                    String getterName = "";
                    ExecutableElement getter = null;
                    for (String possibleName : new String[]{PropertyUtils.makeGetterName(propName, isBool), propName}) {
                        getterName = possibleName;
                        for (MethodData getter1 : methods.get(getterName)) {
                            getter = getter1.element;
                            Optional<PropertyUtils.AccessorInfo> getterInfo = PropertyUtils.getAccessorInfo(getter);
                            if (!getterInfo.isPresent() || getterInfo.get().kind != PropertyUtils.AccessorInfo.Kind.GETTER)
                                continue;
                            if (!types.isSameType(propType, getterInfo.get().propertyType))
                                continue;
                            foundGetter = true;
                            break;
                        }
                        if (foundGetter)
                            break;
                    }
                    if (foundGetter) {
                        accessorPairs.put(propName, new AccessorPair(propType,
                                (ExecutableType) types.asMemberOf(mirror, getter), (ExecutableType) types.asMemberOf(mirror, method),
                                getter, method));
                        implicitAccessorPairs.add(propName);
                        methodsToSkip.add(getterName);
                    }
                    break;
                }
            }
        }

        // zip up explicit accessor pair definitions
        // TODO clean up errors (collect instead of throwing)
        for (AccessorPairDef accessorPairDef : accessorPairDefs) {
            ExecutableElement getter;
            String getterName;
            if (accessorPairDef.getter.isEmpty()) {
                getter = accessorPairDef.definingMethod;
                getterName = getter.getSimpleName().toString();
            } else {
                getterName = accessorPairDef.getter;
                getter = null;
                for (MethodData possibleGetter : methods.get(getterName)) {
                    Optional<PropertyUtils.AccessorInfo> getterInfo = PropertyUtils.getAccessorInfo(possibleGetter.element);
                    if (!getterInfo.isPresent() || getterInfo.get().kind != PropertyUtils.AccessorInfo.Kind.GETTER)
                        continue;
                    getter = possibleGetter.element;
                    break;
                }
                if (getter == null)
                    throw new RuntimeException("Explicit property \"" + accessorPairDef.name + "\": Getter method \"" + getterName + "\" is missing");
            }
            ExecutableElement setter;
            String setterName;
            boolean implicitSetter;
            if (accessorPairDef.setter.isEmpty()) {
                implicitSetter = true;
                setter = accessorPairDef.definingMethod;
                setterName = setter.getSimpleName().toString();
            } else {
                implicitSetter = false;
                setterName = accessorPairDef.setter;
                setter = null;
                for (MethodData possibleSetter : methods.get(setterName)) {
                    Optional<PropertyUtils.AccessorInfo> setterInfo = PropertyUtils.getAccessorInfo(possibleSetter.element);
                    if (!setterInfo.isPresent() || setterInfo.get().kind != PropertyUtils.AccessorInfo.Kind.SETTER)
                        continue;
                    setter = possibleSetter.element;
                    break;
                }
                if (setter == null)
                    throw new RuntimeException("Explicit property \"" + accessorPairDef.name + "\": Setter method \"" + setterName + "\" is missing");
            }

            String propName = accessorPairDef.name;
            if (propName.isEmpty()) {
                propName = AnnotationUtils.getFirstValue(Config.Property.class, Config.Property::name, s -> !s.isEmpty(), getter, setter);
                if (propName == null)
                    propName = typeProvider.name(MirrorElementPair.create(types, mirror, getter),
                            MirrorElementPair.create(types, mirror, setter));
            }

            TypeMirror propType;

            Optional<PropertyUtils.AccessorInfo> getterInfo = PropertyUtils.getAccessorInfo(getter);
            if (!getterInfo.isPresent() || getterInfo.get().kind != PropertyUtils.AccessorInfo.Kind.GETTER)
                throw new RuntimeException("Explicit property \"" + propName + "\": Getter method \"" + getterName + "\" is invalid");
            propType = getterInfo.get().propertyType;

            Optional<PropertyUtils.AccessorInfo> setterInfo = PropertyUtils.getAccessorInfo(setter);
            if (implicitSetter) {
                if (!setterInfo.isPresent()) {
                    // explicit accessor pairs override implicit ones
                    if (implicitAccessorPairs.remove(propName))
                        accessorPairs.remove(propName);

                    if (accessorPairs.put(propName, new AccessorPair(propType,
                            (ExecutableType) types.asMemberOf(mirror, getter), getter)) != null)
                        throw new RuntimeException("Explicit property \"" + propName + "\": Duplicate name!");
                }
            } else {
                if (!setterInfo.isPresent() || setterInfo.get().kind != PropertyUtils.AccessorInfo.Kind.SETTER)
                    throw new RuntimeException("Explicit property \"" + propName + "\": Setter method \"" + setterName + "\" is invalid");
                if (!types.isSameType(propType, setterInfo.get().propertyType))
                    throw new RuntimeException("Explicit property \"" + propName + "\": Type mismatch between getter method \"" + getterName + "\" and setter method \"" + setterName + "\"");

                // explicit accessor pairs override implicit ones
                if (implicitAccessorPairs.remove(propName))
                    accessorPairs.remove(propName);

                if (accessorPairs.put(propName, new AccessorPair(propType,
                        (ExecutableType) types.asMemberOf(mirror, getter), (ExecutableType) types.asMemberOf(mirror, setter),
                        getter, setter)) != null)
                    throw new RuntimeException("Explicit property \"" + propName + "\": Duplicate name!");
            }
        }

        // finally, covert accessor pairs to properties
        for (Map.Entry<String, AccessorPair> entry : accessorPairs.entrySet()) {
            AccessorPair prop = entry.getValue();
            ImmutableClassToInstanceMap.Builder<ConfigPropertyExtension> extensions = ImmutableClassToInstanceMap.builder();
            if (prop.hasSetter) {
                findExtensions(extensions,
                        new MirrorElementPair(prop.getterM, prop.getterE),
                        new MirrorElementPair(prop.setterM, prop.setterE));
                propertiesBuilder.add(new ConfigPropertyImpl.Accessors(typeProvider.fromMirror(prop.type), entry.getKey(),
                        extensions.build(),
                        prop.getterE.getSimpleName().toString(), prop.setterE.getSimpleName().toString()));
            } else {
                findExtensions(extensions,
                        new MirrorElementPair(prop.getterM, prop.getterE));
                propertiesBuilder.add(new ConfigPropertyImpl.Accessors(typeProvider.fromMirror(prop.type), entry.getKey(),
                        extensions.build(),
                        prop.getterE.getSimpleName().toString()));
            }
        }

        // and now, for the instantiation strategy!
        Config.Struct structAnno = mirror.asElement().getAnnotation(Config.Struct.class);
        boolean isFactory;
        DeclaredType owner;
        String factoryName = "";
        List<? extends TypeMirror> params;
        if (structAnno == null) {
            isFactory = false;
            owner = mirror;
            params = Collections.emptyList();
        } else {
            TypeMirror ownerM = AnnotationUtils.getClass(elements, structAnno, Config.Struct::factoryOwner);
            if (ownerM.getKind() != TypeKind.DECLARED)
                throw new RuntimeException("Factory owner \"" + ownerM + "\" must be a declared type");
            owner = (DeclaredType) ownerM;
            isFactory = !types.isSameType(owner, voidTM);
            Function<Config.Struct, Class<?>[]> paramsMapper;
            if (isFactory) {
                factoryName = structAnno.factoryName();
                paramsMapper = Config.Struct::factoryParams;
            } else {
                ownerM = AnnotationUtils.getClass(elements, structAnno, Config.Struct::constructorOwner);
                if (ownerM.getKind() != TypeKind.DECLARED)
                    throw new RuntimeException("Constructor owner \"" + ownerM + "\" must be a declared type");
                owner = (DeclaredType) ownerM;
                if (types.isSameType(owner, voidTM))
                    owner = mirror;
                System.out.println(owner);
                TypeElement ownerTE = elements.getTypeElement(owner.toString());
                if (ownerTE.getKind() == ElementKind.INTERFACE)
                    throw new RuntimeException("Can't specify constructor from interface \"" + owner + "\"!");
                paramsMapper = Config.Struct::constructorParams;
            }
            params = AnnotationUtils.getClasses(elements, structAnno, paramsMapper);
        }

        int paramCount = params.size();
        boolean paramsUnspecified = paramCount == 1 && types.isSameType(params.get(0), voidTM);
        TypeElement ownerElem = elements.getTypeElement(owner.toString());
        LinkedHashMap<String, MirrorElementPair> paramsMap = new LinkedHashMap<>();
        boolean found = false;
        StructInstantiationStrategy instantiationStrategy;
        if (isFactory) {
            for (ExecutableElement method : ElementFilter.methodsIn(ownerElem.getEnclosedElements())) {
                Set<Modifier> modifiers = method.getModifiers();
                if (!modifiers.contains(Modifier.PUBLIC) || !modifiers.contains(Modifier.STATIC))
                    continue;
                if (!factoryName.equals(method.getSimpleName().toString()))
                    continue;
                ExecutableType asMember = (ExecutableType) types.asMemberOf(owner, method);
                if (!types.isSameType(mirror, asMember.getReturnType()))
                    continue;
                List<? extends TypeMirror> mParams = asMember.getParameterTypes();
                if (paramsUnspecified) {
                    if (found)
                        throw new RuntimeException("Factory method " + mirror + " " + owner + "." + factoryName + " has multiple overloads!\n"
                                + "Use the \"factoryParams\" attribute to specify what overload to use");
                    params = new ArrayList<>(mParams);
                    paramCount = params.size();
                    for (int i = 0; i < paramCount; i++) {
                        VariableElement paramElem = method.getParameters().get(i);
                        paramsMap.put(paramElem.getSimpleName().toString(), new MirrorElementPair(params.get(i), paramElem));
                    }
                    found = true;
                } else {
                    if (mParams.size() != paramCount)
                        continue;
                    boolean mismatch = false;
                    for (int i = 0; i < paramCount; i++) {
                        final TypeMirror erasure = types.erasure(mParams.get(i));
                        if (types.isSameType(erasure, params.get(i))) {
                            VariableElement paramElem = method.getParameters().get(i);
                            paramsMap.put(paramElem.getSimpleName().toString(), new MirrorElementPair(erasure, paramElem));
                        } else {
                            mismatch = true;
                            paramsMap.clear();
                            break;
                        }
                    }
                    if (mismatch)
                        continue;
                    found = true;
                    break;
                }
            }
        } else {
            for (ExecutableElement method : ElementFilter.constructorsIn(ownerElem.getEnclosedElements())) {
                Set<Modifier> modifiers = method.getModifiers();
                if (!modifiers.contains(Modifier.PUBLIC))
                    continue;
                ExecutableType asMember = (ExecutableType) types.asMemberOf(owner, method);
                List<? extends TypeMirror> mParams = asMember.getParameterTypes();
                if (paramsUnspecified) {
                    params = new ArrayList<>(mParams);
                    paramCount = params.size();
                    for (int i = 0; i < paramCount; i++) {
                        VariableElement paramElem = method.getParameters().get(i);
                        paramsMap.put(paramElem.getSimpleName().toString(), new MirrorElementPair(params.get(i), paramElem));
                    }
                    found = true;
                    break;
                }
                if (mParams.size() != paramCount)
                    continue;
                boolean mismatch = false;
                for (int i = 0; i < paramCount; i++) {
                    final TypeMirror erasure = types.erasure(mParams.get(i));
                    if (types.isSameType(erasure, params.get(i))) {
                        VariableElement paramElem = method.getParameters().get(i);
                        paramsMap.put(paramElem.getSimpleName().toString(), new MirrorElementPair(erasure, paramElem));
                    } else {
                        mismatch = true;
                        paramsMap.clear();
                        break;
                    }
                }
                if (mismatch)
                    continue;
                found = true;
                break;
            }
        }

        if (found) {
            ImmutableList.Builder<StructInstantiationStrategy.Parameter> paramsBuilder = ImmutableList.builder();
            for (Map.Entry<String, MirrorElementPair> entry : paramsMap.entrySet()) {
                final TypeMirror paramMirror = entry.getValue().mirror();
                final ConfigType paramType = typeProvider.fromMirror(paramMirror);
                Config.BoundProperty boundPropertyAnno = entry.getValue().element().getAnnotation(Config.BoundProperty.class);
                String boundPropertyName = entry.getValue().element().getSimpleName().toString();
                if (boundPropertyAnno != null)
                    boundPropertyName = boundPropertyAnno.value();
                if (!propertyNames.contains(boundPropertyName))
                    throw new RuntimeException("Missing bound property \"" + boundPropertyName + "\" for parameter \"" + entry.getValue().element() + "\"!");
                paramsBuilder.add(new StructInstantiationStrategy.Parameter(paramType, entry.getKey(), boundPropertyName));
            }
            if (isFactory)
                instantiationStrategy = new StructInstantiationStrategyImpl.Factory(paramsBuilder.build(),
                        TypeName.get(owner).withoutAnnotations(), factoryName);
            else
                instantiationStrategy = new StructInstantiationStrategyImpl.Constructor(paramsBuilder.build(),
                        TypeName.get(owner).withoutAnnotations());
        } else {
            StringBuilder sb = new StringBuilder();
            if (!params.isEmpty()) {
                for (TypeMirror param : params)
                    sb.append(param).append(", ");
                sb.setLength(sb.length() - 2);
            }
            if (isFactory)
                throw new RuntimeException("Failed to find factory method " + mirror + " " + owner + "." + factoryName + "(" + sb + ")");
            else if (te.getKind() == ElementKind.INTERFACE) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.MANDATORY_WARNING, "Type cannot be instantiated", te);
                instantiationStrategy = StructInstantiationStrategyImpl.None.INSTANCE;
            } else
                throw new RuntimeException("Failed to find constructor " + owner + "(" + sb + ")");
        }

        return new ConfigTypeImpl.Struct(mirror,
                instantiationStrategy,
                propertiesBuilder.build());
    }

    private boolean isBool(@NotNull TypeMirror type) {
        return type.getKind() == TypeKind.BOOLEAN || types.isSameType(booleanTM, type);
    }
}
