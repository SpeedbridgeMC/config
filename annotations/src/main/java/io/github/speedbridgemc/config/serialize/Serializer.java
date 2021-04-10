package io.github.speedbridgemc.config.serialize;

/**
 * Marks a method as a <em>serializer</em> method.<br>
 * The method must be public, static and take an instance of the configuration type and a {@link java.io.OutputStream}
 * as its only parameters.<br>
 * The method can optionally throw an {@link java.io.IOException}. Return type is ignored.<p>
 * Note that the method will only be used if the {@code "speedbridge-config:custom"} serializer provider is used.
 */
public @interface Serializer { }
