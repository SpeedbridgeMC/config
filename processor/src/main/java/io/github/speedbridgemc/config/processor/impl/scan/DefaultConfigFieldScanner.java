package io.github.speedbridgemc.config.processor.impl.scan;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableClassToInstanceMap;
import io.github.speedbridgemc.config.Config;
import io.github.speedbridgemc.config.processor.api.ConfigField;
import io.github.speedbridgemc.config.processor.api.ConfigFieldExtension;
import io.github.speedbridgemc.config.processor.api.scan.ConfigFieldScanner;
import org.jetbrains.annotations.NotNull;

import javax.annotation.concurrent.Immutable;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;

@AutoService(ConfigFieldScanner.class)
public final class DefaultConfigFieldScanner extends ConfigFieldScanner {
    public DefaultConfigFieldScanner() {
        super("speedbridge-config:default");
    }

    @Override
    public void scan(@NotNull TypeElement type, @NotNull Config config, @NotNull Callback callback) {
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
        for (VariableElement field : ElementFilter.fieldsIn(type.getEnclosedElements())) {
            if (field.getAnnotation(Config.Exclude.class) != null)
                continue;
            Config.Field fieldAnno = field.getAnnotation(Config.Field.class);
            if (!includeFieldsByDefault && fieldAnno == null)
                continue;
            TypeMirror fieldType = field.asType();
            String fieldName = fieldAnno.name();
            if (fieldName.isEmpty())
                fieldName = field.getSimpleName().toString();
            ImmutableClassToInstanceMap.Builder<ConfigFieldExtension> extBuilder = ImmutableClassToInstanceMap.builder();
            // TODO call extension scanners
            callback.addField(ConfigField.field(fieldName, fieldType, extBuilder.build(), field.getSimpleName().toString()));
        }
        // TODO properties (getters/setters)
    }
}
