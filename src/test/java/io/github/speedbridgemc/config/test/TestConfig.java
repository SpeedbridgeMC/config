package io.github.speedbridgemc.config.test;

import io.github.speedbridgemc.config.Config;
import io.github.speedbridgemc.config.seralize.Aliases;
import org.jetbrains.annotations.NotNull;

@Config
public class TestConfig {
    public enum TestEnum {
        FOO("foo"),
        BAR("bar"),
        BAZ("baz");

        private final @NotNull String name;

        TestEnum(@NotNull String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    @Aliases({ "int_one", "INTONEBABY" })
    public int int1;
    @Config.Property(name = "int_2_baby")
    public int int2;
    @Config.Exclude
    public boolean excluded;

    public TestEnum testEnum = TestEnum.FOO;

    private int intProperty;
    private float floatProperty;
    private double doubleProperty;

    public int getIntProperty() {
        return intProperty;
    }

    public void setIntProperty(int intProperty) {
        this.intProperty = intProperty;
    }

    public float floatProperty() {
        return floatProperty;
    }

    public void floatProperty(float floatProperty) {
        this.floatProperty = floatProperty;
    }

    @Config.Property(name = "doubleProp", setter = "doublePropSet")
    public double doublePropGet() {
        return doubleProperty;
    }

    public void doublePropSet(double doubleProperty) {
        this.doubleProperty = doubleProperty;
    }
}
