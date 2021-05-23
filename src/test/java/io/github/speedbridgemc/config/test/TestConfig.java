package io.github.speedbridgemc.config.test;

import io.github.speedbridgemc.config.Config;

@Config
public class TestConfig {
    public int int1;
    @Config.Value(name = "int_2_baby")
    public int int2;
    @Config.Exclude
    public boolean excluded;

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

    @Config.Value(name = "doubleProp", setter = "doublePropSet")
    public double doublePropGet() {
        return doubleProperty;
    }

    public void doublePropSet(double doubleProperty) {
        this.doubleProperty = doubleProperty;
    }
}
