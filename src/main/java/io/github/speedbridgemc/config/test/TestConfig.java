package io.github.speedbridgemc.config.test;

import io.github.speedbridgemc.config.Component;
import io.github.speedbridgemc.config.Config;
import io.github.speedbridgemc.config.serialize.ThrowIfMissing;
import io.github.speedbridgemc.config.serialize.UseDefaultIfMissing;

@Config(name = "test", handlerInterface = "TestConfigHandler",
        components = {
        @Component(
                value = "speedbridge-config:serializer",
                params = "provider=speedbridge-config:jankson"
                ),
                @Component("speedbridge-config:remote")
        },
        nonNullAnnotation = "org.jetbrains.annotations.NotNull", nullableAnnotation = "org.jetbrains.annotations.Nullable")
public final class TestConfig {
    public boolean testBool = false;
    @ThrowIfMissing(message = "test string is :crab:, also \"%s\" is missing")
    public String testString = "hello world";
    public float testFloat = 21.5f;
    public Integer testInt = 21;
    @UseDefaultIfMissing
    public HelloWorld helloWorld = new HelloWorld();
    public Nested3 nested3 = new Nested3();

    public static final class HelloWorld {
        public boolean enabledThis = true;
        @UseDefaultIfMissing
        public boolean enabledThat = false;
        public String mode = "modes";
    }

    @UseDefaultIfMissing
    public static final class Nested {
        public String prize1 = "Goodbye";
    }

    @ThrowIfMissing(message = "yo, \"%s\" is missing")
    public static final class Nested2 {
        public String prize2 = "What?";
        public Nested nested = new Nested();
    }

    @UseDefaultIfMissing
    public static final class Nested3 {
        public String prize3 = "Hello";
        public Nested2 nested = new Nested2();
    }
}
