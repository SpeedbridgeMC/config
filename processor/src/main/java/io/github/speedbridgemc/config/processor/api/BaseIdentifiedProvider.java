package io.github.speedbridgemc.config.processor.api;

import org.jetbrains.annotations.NotNull;

/**
 * A base {@link IdentifiedProvider} implementation.<p>
 * Provides the same benefits as {@link BaseProvider}, plus zero boilerplate for the provider's ID.
 */
public abstract class BaseIdentifiedProvider extends BaseProvider implements IdentifiedProvider {
    protected final String id;

    /**
     * Initializes this provider's ID.
     * @param id provider ID
     */
    protected BaseIdentifiedProvider(String id) {
        this.id = id;
    }

    @Override
    public @NotNull String getId() {
        return id;
    }
}
