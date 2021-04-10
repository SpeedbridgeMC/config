package io.github.speedbridgemc.config.processor.api;

import org.jetbrains.annotations.NotNull;

import javax.annotation.processing.ProcessingEnvironment;

/**
 * The base provider interface.<p>
 * Only defines a single {@link #init(ProcessingEnvironment)} method.
 */
public interface Provider {
    /**
     * Initializes this provider.<p>
     * Note that this is called for <em>every installed provider</em>, no matter if it's used or not.<p>
     * <b>Don't perform expensive operations here!</b>
     * @param processingEnv the processing environment
     */
    void init(@NotNull ProcessingEnvironment processingEnv);
}
