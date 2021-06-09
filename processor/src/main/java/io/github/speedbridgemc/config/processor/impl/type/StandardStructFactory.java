package io.github.speedbridgemc.config.processor.impl.type;

import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedHashMultimap;
import com.squareup.javapoet.TypeName;
import io.github.speedbridgemc.config.Config;
import io.github.speedbridgemc.config.ScanTarget;
import io.github.speedbridgemc.config.processor.api.property.ConfigProperty;
import io.github.speedbridgemc.config.processor.api.property.ConfigPropertyExtension;
import io.github.speedbridgemc.config.processor.api.type.BaseStructFactory;
import io.github.speedbridgemc.config.processor.api.type.ConfigType;
import io.github.speedbridgemc.config.processor.api.type.ConfigTypeProvider;
import io.github.speedbridgemc.config.processor.api.type.StructInstantiationStrategy;
import io.github.speedbridgemc.config.processor.api.util.AnnotationUtils;
import io.github.speedbridgemc.config.processor.api.util.Lazy;
import io.github.speedbridgemc.config.processor.api.util.MirrorElementPair;
import io.github.speedbridgemc.config.processor.api.util.PropertyUtils;
import io.github.speedbridgemc.config.processor.impl.property.ConfigPropertyImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import java.util.*;
import java.util.function.Function;

// this is the class responsible for converting any Java class into a matching ConfigType of kind STRUCT
public final class StandardStructFactory extends BaseStructFactory {
    private TypeMirror booleanTM, voidTM;
    private ImmutableList<ExecutableElement> objectMethods;

    @Override
    public void init(@NotNull ProcessingEnvironment processingEnv, @NotNull ConfigTypeProvider typeProvider) {
        super.init(processingEnv, typeProvider);

        booleanTM = elements.getTypeElement(Boolean.class.getCanonicalName()).asType();
        voidTM = elements.getTypeElement(Void.class.getCanonicalName()).asType();

        TypeElement objectElem = elements.getTypeElement(Object.class.getCanonicalName());
        objectMethods = ImmutableList.copyOf(ElementFilter.methodsIn(objectElem.getEnclosedElements()));
    }

    private static final String[] DUMMY_STRING_ARRAY = new String[0];

    @Override
    public @NotNull Optional<ConfigType> createStruct(@NotNull DeclaredType mirror, Config.@Nullable StructOverride structOverride) {
        TypeElement te = (TypeElement) mirror.asElement();

        boolean includeFieldsByDefault = true;
        boolean includePropertiesByDefault = true;
        Config.Struct structAnno;
        if (structOverride == null) {
            structAnno = te.getAnnotation(Config.Struct.class);
            if (structAnno != null) {
                includeFieldsByDefault = false;
                includePropertiesByDefault = false;
                for (ScanTarget target : structAnno.scanFor()) {
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
        } else
            structAnno = structOverride.override();

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
                boolean overridesObjectMethod = false;
                for (ExecutableElement objectMethod : objectMethods) {
                    if (elements.overrides((ExecutableElement) enclosed, objectMethod, te)) {
                        overridesObjectMethod = true;
                        break;
                    }
                }
                if (overridesObjectMethod)
                    continue;
                methods.put(enclosed.getSimpleName().toString(), new MethodData((ExecutableType) enclosedM, (ExecutableElement) enclosed));
                Config.Property propAnno = enclosed.getAnnotation(Config.Property.class);
                if (propAnno != null)
                    accessorPairDefs.add(new AccessorPairDef(propAnno.name(), propAnno.getter(), propAnno.setter(), propAnno.optional(), (ExecutableElement) enclosed, (ExecutableType) enclosedM));
            } else if (enclosed instanceof VariableElement) {
                fields.put(enclosed.getSimpleName().toString(), new FieldData(enclosedM, (VariableElement) enclosed, mods.contains(Modifier.FINAL)));
            }
        }

        ImmutableList.Builder<ConfigProperty> propertiesBuilder = ImmutableList.builder();
        HashSet<String> propertyNames = new HashSet<>();

        if (structOverride != null)
            getPropertiesFromStructOverride(mirror, structOverride, fields, methods, propertiesBuilder, propertyNames);
        getPropertiesFromFields(includeFieldsByDefault, fields, propertiesBuilder, propertyNames);
        getPropertiesFromAccessorPairs(mirror, includePropertiesByDefault, methods, accessorPairDefs, propertiesBuilder, propertyNames);
        StructInstantiationStrategy instantiationStrategy = createInstantiationStrategy(mirror, te, structAnno, propertyNames);
        return Optional.of(new ConfigTypeImpl.Struct(mirror,
                instantiationStrategy,
                propertiesBuilder.build()));
    }

    // region Intermediary data classes
    private static final class FieldData {
        public final @NotNull TypeMirror mirror;
        public final @NotNull VariableElement element;
        public final boolean isFinal;

        FieldData(@NotNull TypeMirror mirror, @NotNull VariableElement element, boolean isFinal) {
            this.mirror = mirror;
            this.element = element;
            this.isFinal = isFinal;
        }
    }
    private static final class MethodData {
        public final @NotNull ExecutableType mirror;
        public final @NotNull ExecutableElement element;

        MethodData(@NotNull ExecutableType mirror, @NotNull ExecutableElement element) {
            this.mirror = mirror;
            this.element = element;
        }
    }
    private static final class AccessorPairDef {
        public final @NotNull String name, getter, setter;
        public final boolean optional;
        public final @NotNull ExecutableElement definingMethod;
        public final @NotNull ExecutableType definingMethodType;

        AccessorPairDef(@NotNull String name, @NotNull String getter, @NotNull String setter, boolean optional, @NotNull ExecutableElement definingMethod, @NotNull ExecutableType definingMethodType) {
            this.name = name;
            this.getter = getter;
            this.setter = setter;
            this.optional = optional;
            this.definingMethod = definingMethod;
            this.definingMethodType = definingMethodType;
        }
    }
    private static final class AccessorPair {
        public final @NotNull TypeMirror type;
        public final @NotNull ExecutableType getterM;
        public final ExecutableType setterM;
        public final boolean hasSetter;
        public final @NotNull ExecutableElement getterE;
        public final ExecutableElement setterE;
        public final boolean optional;

        AccessorPair(@NotNull TypeMirror type,
                     @NotNull ExecutableType getterM, @NotNull ExecutableType setterM,
                     @NotNull ExecutableElement getterE, @NotNull ExecutableElement setterE, boolean optional) {
            this.optional = optional;
            hasSetter = true;
            this.type = type;
            this.getterM = getterM;
            this.setterM = setterM;
            this.getterE = getterE;
            this.setterE = setterE;
        }

        AccessorPair(@NotNull TypeMirror type,
                     @NotNull ExecutableType getterM, @NotNull ExecutableElement getterE, boolean optional) {
            this.optional = optional;
            hasSetter = false;
            this.type = type;
            this.getterM = getterM;
            this.getterE = getterE;
            setterM = null;
            setterE = null;
        }
    }
    // endregion

    private void getPropertiesFromStructOverride(@NotNull DeclaredType mirror, Config.@NotNull StructOverride structOverride, LinkedHashMap<String, FieldData> fields, LinkedHashMultimap<String, MethodData> methods, ImmutableList.Builder<ConfigProperty> propertiesBuilder, HashSet<String> propertyNames) {
        for (Config.Property property : structOverride.properties()) {
            final String propName = property.name();
            if (propName.isEmpty())
                throw new RuntimeException("Empty property name!");
            ImmutableClassToInstanceMap.Builder<ConfigPropertyExtension> extensionsBuilder = ImmutableClassToInstanceMap.builder();
            if (property.field().isEmpty()) {
                if (property.getter().isEmpty())
                    throw new RuntimeException("Property must have field or getter defined!");
                Set<MethodData> getterDataSet = methods.get(property.getter());
                MethodData getterData = null;
                Optional<PropertyUtils.AccessorInfo> getterAccInfOpt = Optional.empty();
                for (MethodData methodData : getterDataSet) {
                    getterAccInfOpt = PropertyUtils.getAccessorInfo(methodData.mirror);
                    if (getterAccInfOpt.isPresent()) {
                        getterData = methodData;
                        break;
                    }
                }
                PropertyUtils.AccessorInfo getterAccInf = getterAccInfOpt.orElseThrow(() ->
                        new RuntimeException("Can't find valid getter void " + mirror + "." + property.getter() + "()"));
                MirrorElementPair getterMEP = new MirrorElementPair(getterData.mirror, getterData.element);
                if (property.setter().isEmpty()) {
                    typeProvider.findExtensions(extensionsBuilder, getterMEP);
                    propertiesBuilder.add(new ConfigPropertyImpl.Accessors(Lazy.wrap(() -> typeProvider.fromMirror(getterAccInf.propertyType)),
                            propName, extensionsBuilder.build(), property.optional(), property.getter()));
                } else {
                    Set<MethodData> setterDataSet = methods.get(property.setter());
                    MethodData setterData = null;
                    Optional<PropertyUtils.AccessorInfo> setterAccInfOpt = Optional.empty();
                    for (MethodData methodData : setterDataSet) {
                        setterAccInfOpt = PropertyUtils.getAccessorInfo(methodData.mirror);
                        if (setterAccInfOpt.isPresent()) {
                            setterData = methodData;
                            break;
                        }
                    }
                    PropertyUtils.AccessorInfo setterAccInf = setterAccInfOpt.orElseThrow(() ->
                            new RuntimeException("Can't find valid setter " + getterAccInf.propertyType + " " + mirror + "." + property.setter() + "()"));
                    if (!types.isSameType(getterAccInf.propertyType, setterAccInf.propertyType))
                        throw new RuntimeException(mirror + ": Type mismatch between getter" + property.getter() + " and setter " + property.setter());
                    typeProvider.findExtensions(extensionsBuilder, getterMEP, new MirrorElementPair(setterData.mirror, setterData.element));
                    propertiesBuilder.add(new ConfigPropertyImpl.Accessors(Lazy.wrap(() -> typeProvider.fromMirror(getterAccInf.propertyType)),
                            propName, extensionsBuilder.build(), property.optional(), property.getter(), property.setter()));
                }
            } else {
                FieldData fieldData = fields.get(property.field());
                if (fieldData == null)
                    throw new RuntimeException("Missing field \"" + property.field() + "\"!");
                typeProvider.findExtensions(extensionsBuilder, new MirrorElementPair(fieldData.mirror, fieldData.element));
                propertiesBuilder.add(new ConfigPropertyImpl.Field(Lazy.wrap(() -> typeProvider.fromMirror(fieldData.mirror)),
                        propName, extensionsBuilder.build(), !fieldData.isFinal, property.optional(), property.field()));
            }
            if (!propertyNames.add(propName))
                throw new RuntimeException("Duplicate property key \"" + propName + "\"!");
        }
    }

    private void getPropertiesFromFields(boolean includeFieldsByDefault, LinkedHashMap<String, FieldData> fields, ImmutableList.Builder<ConfigProperty> propertiesBuilder, HashSet<String> propertyNames) {
        for (Map.Entry<String, FieldData> field : fields.entrySet()) {
            TypeMirror fieldM = field.getValue().mirror;
            VariableElement fieldE = field.getValue().element;
            Config.Property propAnno = fieldE.getAnnotation(Config.Property.class);
            String fieldName = field.getKey();
            boolean optional = false;
            String propName = "";
            if (propAnno == null) {
                if (!includeFieldsByDefault)
                    continue;
            } else {
                propName = propAnno.name();
                optional = propAnno.optional();
            }
            if (propName.isEmpty())
                propName = typeProvider.name(fieldE.getSimpleName().toString());
            ImmutableClassToInstanceMap.Builder<ConfigPropertyExtension> extensions = ImmutableClassToInstanceMap.builder();
            typeProvider.findExtensions(extensions, new MirrorElementPair(fieldM, fieldE));
            propertiesBuilder.add(new ConfigPropertyImpl.Field(Lazy.wrap(() -> typeProvider.fromMirror(fieldM)),
                    propName, extensions.build(), !field.getValue().isFinal, optional, fieldName));
            if (!propertyNames.add(propName))
                throw new RuntimeException("Duplicate property key \"" + propName + "\"!");
        }
    }

    private void getPropertiesFromAccessorPairs(@NotNull DeclaredType mirror, boolean includePropertiesByDefault, LinkedHashMultimap<String, MethodData> methods, HashSet<AccessorPairDef> accessorPairDefs, ImmutableList.Builder<ConfigProperty> propertiesBuilder, HashSet<String> propertyNames) {
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
                ExecutableType methodType = entry.getValue().mirror;
                Optional<PropertyUtils.AccessorInfo> accessorInfo = PropertyUtils.getAccessorInfo(methodType);
                if (!accessorInfo.isPresent())
                    continue;
                TypeMirror propType = accessorInfo.get().propertyType;
                boolean isBool = isBool(accessorInfo.get().propertyType);
                String internalPropName = PropertyUtils.getPropertyName(methodName, isBool);
                String propName = typeProvider.name(internalPropName);
                if (accessorPairs.containsKey(propName))
                    // TODO error details
                    throw new RuntimeException("Duplicate implicit property name \"" + propName + "\"!");
                switch (accessorInfo.get().kind) {
                case GETTER:
                    // try to find setter
                    boolean foundSetter = false;
                    String setterName = "";
                    ExecutableElement setter = null;
                    for (String possibleName : new String[]{PropertyUtils.makeSetterName(internalPropName), internalPropName}) {
                        setterName = possibleName;
                        for (MethodData setter1 : methods.get(setterName)) {
                            setter = setter1.element;
                            Optional<PropertyUtils.AccessorInfo> setterInfo = PropertyUtils.getAccessorInfo(setter1.mirror);
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
                                method, setter, false));
                        implicitAccessorPairs.add(propName);
                        methodsToSkip.add(setterName);
                    }
                    break;
                case SETTER:
                    // try to find getter
                    boolean foundGetter = false;
                    String getterName = "";
                    ExecutableElement getter = null;
                    for (String possibleName : new String[]{PropertyUtils.makeGetterName(internalPropName, isBool), internalPropName}) {
                        getterName = possibleName;
                        for (MethodData getter1 : methods.get(getterName)) {
                            getter = getter1.element;
                            Optional<PropertyUtils.AccessorInfo> getterInfo = PropertyUtils.getAccessorInfo(getter1.mirror);
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
                                getter, method, false));
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
            ExecutableType getterType;
            String getterName;
            if (accessorPairDef.getter.isEmpty()) {
                getter = accessorPairDef.definingMethod;
                getterType = accessorPairDef.definingMethodType;
                getterName = getter.getSimpleName().toString();
            } else {
                getterName = accessorPairDef.getter;
                getter = null;
                getterType = null;
                for (MethodData possibleGetter : methods.get(getterName)) {
                    Optional<PropertyUtils.AccessorInfo> getterInfo = PropertyUtils.getAccessorInfo(possibleGetter.mirror);
                    if (!getterInfo.isPresent() || getterInfo.get().kind != PropertyUtils.AccessorInfo.Kind.GETTER)
                        continue;
                    getter = possibleGetter.element;
                    getterType = possibleGetter.mirror;
                    break;
                }
                if (getter == null)
                    throw new RuntimeException("Explicit property \"" + accessorPairDef.name + "\": Getter method \"" + getterName + "\" is missing");
            }
            ExecutableElement setter;
            ExecutableType setterType;
            String setterName;
            boolean implicitSetter;
            if (accessorPairDef.setter.isEmpty()) {
                implicitSetter = true;
                setter = accessorPairDef.definingMethod;
                setterType = accessorPairDef.definingMethodType;
                setterName = setter.getSimpleName().toString();
            } else {
                implicitSetter = false;
                setterName = accessorPairDef.setter;
                setter = null;
                setterType = null;
                for (MethodData possibleSetter : methods.get(setterName)) {
                    Optional<PropertyUtils.AccessorInfo> setterInfo = PropertyUtils.getAccessorInfo(possibleSetter.mirror);
                    if (!setterInfo.isPresent() || setterInfo.get().kind != PropertyUtils.AccessorInfo.Kind.SETTER)
                        continue;
                    setter = possibleSetter.element;
                    setterType = possibleSetter.mirror;
                    break;
                }
                if (setter == null)
                    throw new RuntimeException("Explicit property \"" + accessorPairDef.name + "\": Setter method \"" + setterName + "\" is missing");
            }

            String propName = accessorPairDef.name;
            if (propName.isEmpty()) {
                propName = AnnotationUtils.getFirstValue(Config.Property.class, Config.Property::name, s -> !s.isEmpty(), getter, setter);
                if (propName == null)
                    propName = typeProvider.name(PropertyUtils.getPropertyName(getter.getSimpleName().toString(), isBool(getter.getReturnType())));
            }

            TypeMirror propType;

            Optional<PropertyUtils.AccessorInfo> getterInfo = PropertyUtils.getAccessorInfo(getterType);
            if (!getterInfo.isPresent() || getterInfo.get().kind != PropertyUtils.AccessorInfo.Kind.GETTER)
                throw new RuntimeException("Explicit property \"" + propName + "\": Getter method \"" + getterName + "\" is invalid");
            propType = getterInfo.get().propertyType;

            Optional<PropertyUtils.AccessorInfo> setterInfo = PropertyUtils.getAccessorInfo(setterType);
            if (implicitSetter) {
                if (setter == getter || !setterInfo.isPresent()) {
                    // explicit accessor pairs override implicit ones
                    if (implicitAccessorPairs.remove(propName))
                        accessorPairs.remove(propName);

                    if (accessorPairs.put(propName, new AccessorPair(propType,
                            (ExecutableType) types.asMemberOf(mirror, getter), getter, accessorPairDef.optional)) != null)
                        throw new RuntimeException("Explicit property \"" + propName + "\": Duplicate name!");

                    continue;
                }
            } else {
                if (!setterInfo.isPresent() || setterInfo.get().kind != PropertyUtils.AccessorInfo.Kind.SETTER)
                    throw new RuntimeException("Explicit property \"" + propName + "\": Setter method \"" + setterName + "\" is invalid");
                if (!types.isSameType(propType, setterInfo.get().propertyType))
                    throw new RuntimeException("Explicit property \"" + propName + "\": Type mismatch between getter method \"" + getterName + "\" and setter method \"" + setterName + "\"");
            }

            // explicit accessor pairs override implicit ones
            if (implicitAccessorPairs.remove(propName))
                accessorPairs.remove(propName);

            if (accessorPairs.put(propName, new AccessorPair(propType,
                    (ExecutableType) types.asMemberOf(mirror, getter), (ExecutableType) types.asMemberOf(mirror, setter),
                    getter, setter, accessorPairDef.optional)) != null)
                throw new RuntimeException("Explicit property \"" + propName + "\": Duplicate name!");
        }

        // finally, covert accessor pairs to properties
        for (Map.Entry<String, AccessorPair> entry : accessorPairs.entrySet()) {
            AccessorPair prop = entry.getValue();
            ImmutableClassToInstanceMap.Builder<ConfigPropertyExtension> extensions = ImmutableClassToInstanceMap.builder();
            if (prop.hasSetter) {
                typeProvider.findExtensions(extensions,
                        new MirrorElementPair(prop.getterM, prop.getterE),
                        new MirrorElementPair(prop.setterM, prop.setterE));
                propertiesBuilder.add(new ConfigPropertyImpl.Accessors(Lazy.wrap(() -> typeProvider.fromMirror(prop.type)),
                        entry.getKey(),
                        extensions.build(),
                        prop.optional,
                        prop.setterE.getSimpleName().toString(), prop.getterE.getSimpleName().toString()));
            } else {
                typeProvider. findExtensions(extensions,
                        new MirrorElementPair(prop.getterM, prop.getterE));
                propertiesBuilder.add(new ConfigPropertyImpl.Accessors(Lazy.wrap(() -> typeProvider.fromMirror(prop.type)),
                        entry.getKey(),
                        extensions.build(),
                        prop.optional,
                        prop.getterE.getSimpleName().toString()));
            }
            if (!propertyNames.add(entry.getKey()))
                throw new RuntimeException("Duplicate property key \"" + entry.getKey()+ "\"!");
        }
    }

    @NotNull
    private StructInstantiationStrategy createInstantiationStrategy(@NotNull DeclaredType mirror, TypeElement te, Config.Struct structAnno, HashSet<String> propertyNames) {
        boolean isFactory;
        DeclaredType owner;
        String factoryName = "";
        String[] boundProperties = DUMMY_STRING_ARRAY;
        List<? extends TypeMirror> params;
        if (structAnno == null) {
            isFactory = false;
            owner = mirror;
            params = Collections.singletonList(voidTM);
        } else {
            TypeMirror ownerM = AnnotationUtils.getClass(structAnno, Config.Struct::factoryOwner);
            if (ownerM.getKind() != TypeKind.DECLARED)
                throw new RuntimeException("Factory owner \"" + ownerM + "\" must be a declared type");
            boundProperties = structAnno.boundProperties();
            owner = (DeclaredType) ownerM;
            isFactory = !types.isSameType(owner, voidTM);
            Function<Config.Struct, Class<?>[]> paramsMapper;
            if (isFactory) {
                factoryName = structAnno.factoryName();
                paramsMapper = Config.Struct::factoryParams;
            } else {
                ownerM = AnnotationUtils.getClass(structAnno, Config.Struct::constructorOwner);
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
            params = AnnotationUtils.getClasses(structAnno, paramsMapper);
        }

        final DeclaredType mirrorErasure = (DeclaredType) types.erasure(mirror);
        int paramCount = params.size();
        boolean paramsUnspecified = paramCount == 1 && types.isSameType(params.get(0), voidTM);
        TypeElement ownerElem = elements.getTypeElement(owner.toString());
        LinkedHashMap<String, MirrorElementPair> paramsMap = new LinkedHashMap<>();
        boolean found = false;
        StructInstantiationStrategy instantiationStrategy;
        if (isFactory) {
            // FIXME: THIS DOES NOT WORK WITH GENERICS!
            //  param types need to be substituted with actual type parameter values
            for (ExecutableElement method : ElementFilter.methodsIn(ownerElem.getEnclosedElements())) {
                Set<Modifier> modifiers = method.getModifiers();
                if (!modifiers.contains(Modifier.PUBLIC) || !modifiers.contains(Modifier.STATIC))
                    continue;
                if (!factoryName.equals(method.getSimpleName().toString()))
                    continue;
                ExecutableType asMember = (ExecutableType) types.asMemberOf(owner, method);
                TypeMirror returnType = asMember.getReturnType();
                if (!types.isSameType(mirrorErasure, types.erasure(returnType)))
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
                            paramsMap.put(paramElem.getSimpleName().toString(), new MirrorElementPair(mParams.get(i), paramElem));
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
                        paramsMap.put(paramElem.getSimpleName().toString(), new MirrorElementPair(mParams.get(i), paramElem));
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
            int i = 0;
            for (Map.Entry<String, MirrorElementPair> entry : paramsMap.entrySet()) {
                String boundPropertyName = "";
                if (i < boundProperties.length) {
                    boundPropertyName = boundProperties[i];
                    i++;
                }
                final TypeMirror paramMirror = entry.getValue().mirror();
                Config.BoundProperty boundPropertyAnno = entry.getValue().element().getAnnotation(Config.BoundProperty.class);
                if (boundPropertyAnno == null) {
                    if (boundPropertyName.isEmpty())
                        boundPropertyName = typeProvider.name(entry.getValue().element().getSimpleName().toString());
                } else
                    boundPropertyName = boundPropertyAnno.value();
                if (!propertyNames.contains(boundPropertyName))
                    throw new RuntimeException("Missing bound property \"" + boundPropertyName + "\" for parameter \"" + entry.getValue().element() + "\"!");
                paramsBuilder.add(new StructInstantiationStrategyImpl.ParameterImpl(Lazy.wrap(() -> typeProvider.fromMirror(paramMirror)),
                        entry.getKey(), boundPropertyName));
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
                instantiationStrategy = StructInstantiationStrategy.NONE;
            } else
                throw new RuntimeException("Failed to find constructor " + owner + "(" + sb + ")");
        }
        return instantiationStrategy;
    }

    private boolean isBool(@NotNull TypeMirror type) {
        return type.getKind() == TypeKind.BOOLEAN || types.isSameType(booleanTM, type);
    }
}
