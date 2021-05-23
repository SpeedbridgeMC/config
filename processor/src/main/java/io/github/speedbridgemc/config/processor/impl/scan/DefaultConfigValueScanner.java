package io.github.speedbridgemc.config.processor.impl.scan;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableClassToInstanceMap;
import io.github.speedbridgemc.config.Config;
import io.github.speedbridgemc.config.processor.api.ConfigValue;
import io.github.speedbridgemc.config.processor.api.ConfigValueExtension;
import io.github.speedbridgemc.config.processor.api.scan.ConfigValueScanner;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;

@AutoService(ConfigValueScanner.class)
public final class DefaultConfigValueScanner extends ConfigValueScanner {
    public DefaultConfigValueScanner() {
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
            Config.Value valueAnno = field.getAnnotation(Config.Value.class);
            if (!includeFieldsByDefault && valueAnno == null)
                continue;
            TypeMirror valueType = field.asType();
            String valueName = valueAnno.name();
            if (valueName.isEmpty())
                // TODO naming strategy
                valueName = field.getSimpleName().toString();
            ImmutableClassToInstanceMap.Builder<ConfigValueExtension> extBuilder = ImmutableClassToInstanceMap.builder();
            // TODO call extension scanners
            callback.addField(ConfigValue.field(valueName, valueType, extBuilder.build(), field.getSimpleName().toString()));
        }
        // TODO properties (getters/setters)
    }
}
