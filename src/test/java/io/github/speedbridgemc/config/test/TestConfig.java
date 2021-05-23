package io.github.speedbridgemc.config.test;

import io.github.speedbridgemc.config.Config;

@Config
public class TestConfig {
    public int int1;
    @Config.Value(name = "int_2_baby")
    public int int2;
    @Config.Exclude
    public boolean excluded;
}
