package io.github.speedbridgemc.config.processor.impl.type;

import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedHashMultimap;
import com.squareup.javapoet.TypeName;
import io.github.speedbridgemc.config.Config;
import io.github.speedbridgemc.config.processor.api.property.ConfigProperty;
import io.github.speedbridgemc.config.processor.api.type.ConfigType;
import io.github.speedbridgemc.config.processor.api.type.ConfigTypeProvider;
import io.github.speedbridgemc.config.processor.impl.property.ConfigPropertyImpl;
import org.jetbrains.annotations.NotNull;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

final class ConfigTypeStructFactory {
    private final ConfigTypeProvider typeProvider;
    private final ProcessingEnvironment processingEnv;
    private final Elements elements;
    private final Types types;

    public ConfigTypeStructFactory(ConfigTypeProvider typeProvider, ProcessingEnvironment processingEnv) {
        this.typeProvider = typeProvider;
        this.processingEnv = processingEnv;
        elements = processingEnv.getElementUtils();
        types = processingEnv.getTypeUtils();
    }

    public @NotNull ConfigType create(@NotNull DeclaredType mirror) {
        class FieldData {
            public final @NotNull TypeMirror mirror;
            public final @NotNull AnnotatedConstruct annoSrc;

            FieldData(@NotNull TypeMirror mirror, @NotNull AnnotatedConstruct annoSrc) {
                this.mirror = mirror;
                this.annoSrc = annoSrc;
            }
        }
        class MethodData {
            public final @NotNull ExecutableType mirror;
            public final @NotNull AnnotatedConstruct annoSrc;

            MethodData(@NotNull ExecutableType mirror, @NotNull AnnotatedConstruct annoSrc) {
                this.mirror = mirror;
                this.annoSrc = annoSrc;
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

        for (Element enclosed : te.getEnclosedElements()) {
            if (enclosed.getAnnotation(Config.Exclude.class) != null)
                continue;
            Set<Modifier> mods = enclosed.getModifiers();
            if (!mods.contains(Modifier.PUBLIC) || mods.contains(Modifier.STATIC) || mods.contains(Modifier.TRANSIENT))
                continue;
            TypeMirror enclosedM = types.asMemberOf(mirror, enclosed);  // fills in type variables!
                                                                        // also erases annotations, apparently
            if (enclosedM instanceof ExecutableType)
                methods.put(enclosed.getSimpleName().toString(), new MethodData((ExecutableType) enclosedM, enclosed));
            else if (enclosed instanceof VariableElement) {
                if (mods.contains(Modifier.FINAL))
                    continue;
                fields.put(enclosed.getSimpleName().toString(), new FieldData(enclosedM, enclosed));
            }
        }

        ImmutableList.Builder<ConfigProperty> propertiesBuilder = ImmutableList.builder();

        // fields
        if (includeFieldsByDefault) {
            for (Map.Entry<String, FieldData> field : fields.entrySet()) {
                TypeMirror fieldM = field.getValue().mirror;
                AnnotatedConstruct fieldAS = field.getValue().annoSrc;
                Config.Property propAnno = fieldAS.getAnnotation(Config.Property.class);
                // TODO scan for extensions
                String fieldName = field.getKey();
                String propName = fieldName;
                if (propAnno != null && !propAnno.name().isEmpty())
                    propName = propAnno.name();
                ConfigType fieldType = typeProvider.fromMirror(fieldM);
                propertiesBuilder.add(new ConfigPropertyImpl.Field(fieldType, propName, ImmutableClassToInstanceMap.of(), fieldName));
            }
        }

        // TODO properties

        // TODO instantiation (constructor/factory)
        return new ConfigTypeImpl.Struct(mirror,
                new StructInstantiationStrategyImpl.Constructor(ImmutableList.of(), TypeName.get(mirror).withoutAnnotations()),
                propertiesBuilder.build());
    }
}
