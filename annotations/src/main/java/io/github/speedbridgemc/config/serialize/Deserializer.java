package io.github.speedbridgemc.config.serialize;

/**
 * Marks a method as a <em>deserializer</em> method.<br>
 * The method must be public, static, take a {@link java.io.InputStream} as its only parameter,
 * and return an instance of the configuration type.<br>
 * The method can optionally throw an {@link java.io.IOException}.<p>
 * Note that the method will only be used if the {@code "speedbridge-config:custom"} serializer provider is used.
 */
public @interface Deserializer { }
