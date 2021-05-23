package io.github.speedbridgemc.config.processor.impl.scan;

import com.google.auto.service.AutoService;
import com.google.common.collect.Iterables;
import io.github.speedbridgemc.config.processor.api.scan.ConfigValueNamingStrategy;
import io.github.speedbridgemc.config.processor.api.util.PropertyUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.Collection;
import java.util.Optional;

@AutoService(ConfigValueNamingStrategy.class)
public final class IdentityConfigValueNamingStrategy extends ConfigValueNamingStrategy {
    public IdentityConfigValueNamingStrategy() {
        super("speedbridge-config:identity");
    }

    private TypeMirror booleanTM;

    @Override
    public void init(@NotNull ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        booleanTM = elements.getTypeElement(Boolean.class.getCanonicalName()).asType();
    }

    @Override
    public @NotNull String name(@NotNull TypeElement type, @NotNull Collection<? extends Element> elements, @NotNull String variantId) {
        Element firstElem = Iterables.getFirst(elements, null);
        if (firstElem == null)
            throw new RuntimeException("Can't name value if it has no elements associated with it!");
        if (firstElem instanceof ExecutableElement) {
            Optional<PropertyUtils.AccessorInfo> accessorInfo = PropertyUtils.getAccessorInfo((ExecutableElement) firstElem);
            if (accessorInfo.isPresent())
                return PropertyUtils.getPropertyName(firstElem.getSimpleName().toString(), isBool(accessorInfo.get().propertyType));
        }
        return firstElem.getSimpleName().toString();
    }

    private boolean isBool(@NotNull TypeMirror type) {
        return type.getKind() == TypeKind.BOOLEAN || types.isSameType(booleanTM, type);
    }
}
