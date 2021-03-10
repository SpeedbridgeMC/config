package io.github.speedbridgemc.config.test;

import io.github.speedbridgemc.config.Component;
import io.github.speedbridgemc.config.Config;

@Config(name = "test",
        components = {
        @Component(
                value = "speedbridge-config:serializer",
        params = { "pathTemplate=java.nio.file.Paths.get(\".\", \"$L.json5\")",
                "logTemplate=System.err.println($S); $L.printStackTrace()" })
})
public class TestConfig {
    public boolean testBool = false;
    public String testString = "hello world";
}
