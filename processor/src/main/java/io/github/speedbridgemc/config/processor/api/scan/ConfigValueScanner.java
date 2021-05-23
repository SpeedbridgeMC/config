package io.github.speedbridgemc.config.processor.api.scan;

import io.github.speedbridgemc.config.Config;
import io.github.speedbridgemc.config.processor.api.ConfigValue;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.Collection;

public abstract class ConfigValueScanner extends ScanPhaseWorker {
    protected ConfigValueScanner(@NotNull String id) {
        super(id);
    }

    public static final class Context {
        private final @NotNull ConfigValueNamingStrategy namingStrategy;
        private final @NotNull String namingStrategyVariantId;
        private final @NotNull Iterable<? extends ConfigValueExtensionScanner> extensionScanners;

        public Context(@NotNull ConfigValueNamingStrategy namingStrategy, @NotNull String namingStrategyVariantId,
                       @NotNull Iterable<? extends ConfigValueExtensionScanner> extensionScanners) {
            this.namingStrategy = namingStrategy;
            this.namingStrategyVariantId = namingStrategyVariantId;
            this.extensionScanners = extensionScanners;
        }

        public @NotNull String name(@NotNull TypeElement type, @NotNull Collection<? extends Element> elements) {
            return namingStrategy.name(type, elements, namingStrategyVariantId);
        }

        public void findExtensions(@NotNull TypeElement type, @NotNull Config config, @NotNull Collection<? extends Element> elements, @NotNull ConfigValueExtensionScanner.Callback callback) {
            for (ConfigValueExtensionScanner scanner : extensionScanners)
                scanner.findExtensions(type, config, elements, callback);
        }
    }

    public interface Callback {
        void addValue(@NotNull ConfigValue field);
    }

    public abstract void findValues(@NotNull Context ctx, @NotNull TypeElement type, @NotNull Config config, @NotNull Callback callback);
}
