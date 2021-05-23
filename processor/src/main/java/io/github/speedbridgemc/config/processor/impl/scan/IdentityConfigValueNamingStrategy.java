package io.github.speedbridgemc.config.processor.impl.scan;

import com.google.auto.service.AutoService;
import com.google.common.collect.Iterables;
import io.github.speedbridgemc.config.processor.api.scan.ConfigValueNamingStrategy;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.Collection;

@AutoService(ConfigValueNamingStrategy.class)
public class IdentityConfigValueNamingStrategy extends ConfigValueNamingStrategy {
    public IdentityConfigValueNamingStrategy() {
        super("speedbridge-config:identity");
    }

    @Override
    public @NotNull String name(@NotNull TypeElement type, @NotNull Collection<? extends Element> elements, @NotNull String variantId) {
        Element firstElem = Iterables.getFirst(elements, null);
        if (firstElem == null)
            throw new RuntimeException("Can't name value if it has no elements associated with it!");
        return firstElem.getSimpleName().toString();
    }
}
