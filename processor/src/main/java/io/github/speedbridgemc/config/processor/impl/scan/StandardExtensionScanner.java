package io.github.speedbridgemc.config.processor.impl.scan;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableSet;
import io.github.speedbridgemc.config.Config;
import io.github.speedbridgemc.config.processor.api.ext.SerializeExtension;
import io.github.speedbridgemc.config.processor.api.scan.ConfigValueExtensionScanner;
import io.github.speedbridgemc.config.seralize.Aliases;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.Collection;

@AutoService(ConfigValueExtensionScanner.class)
public final class StandardExtensionScanner extends ConfigValueExtensionScanner {
    public StandardExtensionScanner() {
        super("speedbridge-config:standard");
    }

    @Override
    public void findExtensions(@NotNull TypeElement type, @NotNull Config config, @NotNull Collection<? extends Element> elements, @NotNull Callback callback) {
        final ImmutableSet.Builder<String> aliasBuilder = ImmutableSet.builder();
        for (Element element : elements) {
            Aliases aliases = element.getAnnotation(Aliases.class);
            if (aliases == null)
                continue;
            for (String alias : aliases.value())
                aliasBuilder.add(alias);
        }
        callback.putExtension(SerializeExtension.class, new SerializeExtension(aliasBuilder.build()));
    }
}
