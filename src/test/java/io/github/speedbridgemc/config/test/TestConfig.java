package io.github.speedbridgemc.config.test;

import io.github.speedbridgemc.config.Config;
import io.github.speedbridgemc.config.EnumName;
import io.github.speedbridgemc.config.seralize.Aliases;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;

@Config
public class TestConfig {
    public enum TestEnum {
        @EnumName("foo")
        FOO,
        @EnumName("bar")
        BAR,
        @EnumName("baz")
        BAZ;

        private final @NotNull String name;

        TestEnum() {
            final String internalName = super.name();
            Field field;
            try {
                field = getClass().getField(internalName);
            } catch (NoSuchFieldException e) {
                throw new InternalError("Missing field \"" + internalName + "\"!", e);
            }
            EnumName enumName = field.getAnnotation(EnumName.class);
            if (enumName == null)
                name = internalName;
            else
                name = enumName.value();
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
