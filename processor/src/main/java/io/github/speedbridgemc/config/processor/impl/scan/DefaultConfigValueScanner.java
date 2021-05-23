package io.github.speedbridgemc.config.processor.impl.scan;

import com.google.auto.service.AutoService;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.ImmutableSet;
import io.github.speedbridgemc.config.Config;
import io.github.speedbridgemc.config.processor.api.ConfigValue;
import io.github.speedbridgemc.config.processor.api.ConfigValueExtension;
import io.github.speedbridgemc.config.processor.api.scan.ConfigValueScanner;
import io.github.speedbridgemc.config.processor.api.util.AnnotationUtils;
import io.github.speedbridgemc.config.processor.api.util.PropertyUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.util.*;

import static java.util.Collections.singleton;

@AutoService(ConfigValueScanner.class)
public final class DefaultConfigValueScanner extends ConfigValueScanner {
    public DefaultConfigValueScanner() {
        super("speedbridge-config:default");
    }

    private TypeMirror booleanTM;

    @Override
    public void init(@NotNull ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        booleanTM = elements.getTypeElement(Boolean.class.getCanonicalName()).asType();
    }

    @Override
    public void findValues(@NotNull Context ctx, @NotNull TypeElement type, @NotNull Config config, @NotNull Callback callback) {
        boolean includeFieldsByDefault = false;
        boolean includePropertiesByDefault = false;
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

        // fields
        for (VariableElement field : ElementFilter.fieldsIn(type.getEnclosedElements())) {
            if (field.getKind() == ElementKind.ENUM_CONSTANT)
                continue;

            Set<Modifier> modifiers = field.getModifiers();
            if (modifiers.contains(Modifier.STATIC) || modifiers.contains(Modifier.FINAL)
                    || modifiers.contains(Modifier.TRANSIENT) || !modifiers.contains(Modifier.PUBLIC))
                continue;
            if (field.getAnnotation(Config.Exclude.class) != null)
                continue;
            Config.Value valueAnno = field.getAnnotation(Config.Value.class);
            if (!includeFieldsByDefault && valueAnno == null)
                continue;
            TypeMirror valueType = field.asType();
            String valueName = "";
            if (valueAnno != null)
                valueName = valueAnno.name();
            if (valueName.isEmpty())
                valueName = ctx.name(type, singleton(field));
            ImmutableClassToInstanceMap.Builder<ConfigValueExtension> extBuilder = ImmutableClassToInstanceMap.builder();
            ctx.findExtensions(type, config, singleton(field), extBuilder::put);
            callback.addValue(ConfigValue.field(valueName, valueType, extBuilder.build(), field.getSimpleName().toString()));
        }

        // properties (getter + setter pair)

        class PropertyDefinition {
            public final @NotNull String name, getter, setter;
            public final @NotNull ExecutableElement definingMethod;

            PropertyDefinition(@NotNull String name, @NotNull String getter, @NotNull String setter, @NotNull ExecutableElement definingMethod) {
                this.name = name;
                this.getter = getter;
                this.setter = setter;
                this.definingMethod = definingMethod;
            }
        }

        class Property {
            public final @NotNull TypeMirror type;
            public final @NotNull ExecutableElement getter, setter;

            Property(@NotNull TypeMirror type, @NotNull ExecutableElement getter, @NotNull ExecutableElement setter) {
                this.type = type;
                this.getter = getter;
                this.setter = setter;
            }
        }

        // map all (non-excluded) methods
        // also collect explicit property definitions
        HashMultimap<String, ExecutableElement> methods = HashMultimap.create();
        HashSet<PropertyDefinition> propertyDefinitions = new HashSet<>();
        for (ExecutableElement method : ElementFilter.methodsIn(type.getEnclosedElements())) {
            Set<Modifier> modifiers = method.getModifiers();
            if (modifiers.contains(Modifier.STATIC) || !modifiers.contains(Modifier.PUBLIC))
                continue;
            if (method.getAnnotation(Config.Exclude.class) != null)
                continue;
            methods.put(method.getSimpleName().toString(), method);
            Config.Value valueAnno = method.getAnnotation(Config.Value.class);
            if (valueAnno != null)
                propertyDefinitions.add(new PropertyDefinition(valueAnno.name(), valueAnno.getter(), valueAnno.setter(), method));
        }

        HashMap<String, Property> properties = new HashMap<>();
        // discover and zip up implicit properties
        HashSet<String> implicitProperties = new HashSet<>();
        if (includePropertiesByDefault) {
            HashSet<String> methodsToSkip = new HashSet<>();
            for (Map.Entry<String, ExecutableElement> entry : methods.entries()) {
                String methodName = entry.getKey();
                if (methodsToSkip.contains(methodName))
                    continue;
                ExecutableElement method = entry.getValue();
                if (method.getAnnotation(Config.Value.class) != null)
                    // handled later
                    continue;
                Optional<PropertyUtils.AccessorInfo> accessorInfo = PropertyUtils.getAccessorInfo(method);
                if (!accessorInfo.isPresent())
                    continue;
                TypeMirror propType = accessorInfo.get().propertyType;
                boolean isBool = isBool(accessorInfo.get().propertyType);
                String propName = PropertyUtils.getPropertyName(methodName, isBool);
                if (properties.containsKey(propName))
                    // TODO error details
                    throw new RuntimeException("Duplicate implicit property name \"" + propName + "\"!");
                switch (accessorInfo.get().type) {
                case GETTER:
                    // try to find setter
                    boolean foundSetter = false;
                    String setterName = "";
                    ExecutableElement setter = null;
                    for (String possibleName : new String[]{PropertyUtils.makeSetterName(propName), propName}) {
                        setterName = possibleName;
                        for (ExecutableElement setter1 : methods.get(setterName)) {
                            setter = setter1;
                            Optional<PropertyUtils.AccessorInfo> setterInfo = PropertyUtils.getAccessorInfo(setter);
                            if (!setterInfo.isPresent() || setterInfo.get().type != PropertyUtils.AccessorInfo.Type.SETTER)
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
                        properties.put(propName, new Property(propType, method, setter));
                        implicitProperties.add(propName);
                        methodsToSkip.add(setterName);
                    }
                    break;
                case SETTER:
                    // try to find getter
                    boolean foundGetter = false;
                    String getterName = "";
                    ExecutableElement getter = null;
                    for (String possibleName : new String[]{PropertyUtils.makeGetterName(propName, isBool), propName}) {
                        getterName = possibleName;
                        for (ExecutableElement getter1 : methods.get(getterName)) {
                            getter = getter1;
                            Optional<PropertyUtils.AccessorInfo> getterInfo = PropertyUtils.getAccessorInfo(getter);
                            if (!getterInfo.isPresent() || getterInfo.get().type != PropertyUtils.AccessorInfo.Type.GETTER)
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
                        properties.put(propName, new Property(propType, getter, method));
                        implicitProperties.add(propName);
                        methodsToSkip.add(getterName);
                    }
                    break;
                }
            }
        }
        
        // zip up explicit property definitions
        // TODO clean up errors (collect instead of throwing)
        for (PropertyDefinition propDef : propertyDefinitions) {
            ExecutableElement getter;
            String getterName;
            if (propDef.getter.isEmpty()) {
                getter = propDef.definingMethod;
                getterName = getter.getSimpleName().toString();
            } else {
                getterName = propDef.getter;
                getter = null;
                for (ExecutableElement possibleGetter : methods.get(getterName)) {
                    Optional<PropertyUtils.AccessorInfo> getterInfo = PropertyUtils.getAccessorInfo(possibleGetter);
                    if (!getterInfo.isPresent() || getterInfo.get().type != PropertyUtils.AccessorInfo.Type.GETTER)
                        continue;
                    getter = possibleGetter;
                    break;
                }
                if (getter == null)
                    throw new RuntimeException("Explicit property \"" + propDef.name + "\": Getter method \"" + getterName + "\" is missing");
            }
            ExecutableElement setter;
            String setterName;
            if (propDef.setter.isEmpty()) {
                setter = propDef.definingMethod;
                setterName = setter.getSimpleName().toString();
            } else {
                setterName = propDef.setter;
                setter = null;
                for (ExecutableElement possibleSetter : methods.get(setterName)) {
                    Optional<PropertyUtils.AccessorInfo> setterInfo = PropertyUtils.getAccessorInfo(possibleSetter);
                    if (!setterInfo.isPresent() || setterInfo.get().type != PropertyUtils.AccessorInfo.Type.SETTER)
                        continue;
                    setter = possibleSetter;
                    break;
                }
                if (setter == null)
                    throw new RuntimeException("Explicit property \"" + propDef.name + "\": Setter method \"" + setterName + "\" is missing");
            }

            String propName = propDef.name;
            if (propName.isEmpty()) {
                propName = AnnotationUtils.getAnnotationValue(Config.Value.class, Config.Value::name, s -> !s.isEmpty(), getter, setter);
                if (propName == null)
                    propName = ctx.name(type, ImmutableSet.of(getter, setter));
            }
            
            TypeMirror propType;
            
            Optional<PropertyUtils.AccessorInfo> getterInfo = PropertyUtils.getAccessorInfo(getter);
            if (!getterInfo.isPresent() || getterInfo.get().type != PropertyUtils.AccessorInfo.Type.GETTER)
                throw new RuntimeException("Explicit property \"" + propName + "\": Getter method \"" + getterName + "\" is invalid");
            propType = getterInfo.get().propertyType;
            
            Optional<PropertyUtils.AccessorInfo> setterInfo = PropertyUtils.getAccessorInfo(setter);
            if (!setterInfo.isPresent() || setterInfo.get().type != PropertyUtils.AccessorInfo.Type.SETTER)
                throw new RuntimeException("Explicit property \"" + propName + "\": Setter method \"" + setterName + "\" is invalid");
            if (!types.isSameType(propType, setterInfo.get().propertyType))
                throw new RuntimeException("Explicit property \"" + propName + "\": Type mismatch between getter method \"" + getterName + "\" and setter method \"" + setterName + "\"");

            // explicit properties override implicit ones
            if (implicitProperties.remove(propName))
                properties.remove(propName);

            if (properties.put(propName, new Property(propType, getter, setter)) != null)
                throw new RuntimeException("Explicit property \"" + propName + "\": Duplicate name!");
        }

        // finally, covert properties to values
        for (Map.Entry<String, Property> entry : properties.entrySet()) {
            Property prop = entry.getValue();
            ImmutableClassToInstanceMap.Builder<ConfigValueExtension> extBuilder = ImmutableClassToInstanceMap.builder();
            ctx.findExtensions(type, config, ImmutableSet.of(prop.getter, prop.setter), extBuilder::put);
            callback.addValue(ConfigValue.property(entry.getKey(), prop.type, extBuilder.build(),
                    prop.getter.getSimpleName().toString(), prop.setter.getSimpleName().toString()));
        }
    }

    private boolean isBool(@NotNull TypeMirror type) {
        return type.getKind() == TypeKind.BOOLEAN || types.isSameType(booleanTM, type);
    }
}
