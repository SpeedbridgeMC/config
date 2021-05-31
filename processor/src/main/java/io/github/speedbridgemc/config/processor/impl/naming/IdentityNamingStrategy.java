package io.github.speedbridgemc.config.processor.impl.naming;

import com.google.auto.service.AutoService;
import io.github.speedbridgemc.config.processor.api.naming.BaseNamingStrategy;
import io.github.speedbridgemc.config.processor.api.naming.NamingStrategy;
import io.github.speedbridgemc.config.processor.api.util.MirrorElementPair;
import io.github.speedbridgemc.config.processor.api.util.PropertyUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.Optional;

@AutoService(NamingStrategy.class)
public final class IdentityNamingStrategy extends BaseNamingStrategy {
    public IdentityNamingStrategy() {
        super("speedbridge-config:identity");
    }

    private TypeMirror booleanTM;

    @Override
    public void init(@NotNull ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        booleanTM = elements.getTypeElement(Boolean.class.getCanonicalName()).asType();
    }

    @Override
    public @NotNull String name(@NotNull String variant, @NotNull MirrorElementPair @NotNull ... pairs) {
        if (pairs.length == 0)
            throw new RuntimeException("Can't name value if it has no elements associated with it!");
        Element firstElem = pairs[0].element();
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
