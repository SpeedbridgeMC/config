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

    @Config.Struct(scanFor = { }, factoryOwner = HelloWorld.class, factoryName = "create")
    public static class HelloWorld {
        @Config.Property(name = "hello")
        public final String hello;
        private final int world;

        public static HelloWorld create(String hello, @Config.BoundProperty("world") int zaWarudo) {
            return new HelloWorld(hello, zaWarudo);
        }

        private HelloWorld(String hello,  int world) {
            this.hello = hello;
            this.world = world;
        }

        @Config.Property(name = "world")
        public int getWorld() {
            return world;
        }
    }

    @Config.Struct(constructorOwner = TestInterfaceImpl.class)
    public interface TestInterface {
        int getA();
        void setA(int a);
    }

    public static class TestInterfaceImpl implements TestInterface {
        private int a;

        @Override
        public int getA() {
            return a;
        }

        @Override
        public void setA(int a) {
            this.a = a;
        }
    }

    @Aliases({ "int_one", "INTONEBABY" })
    public int int1;
    @Config.Property(name = "int_2_baby")
    public int int2;
    @Config.Exclude
    public boolean excluded;

    public TestEnum testEnum = TestEnum.FOO;
    public HelloWorld helloWorld = HelloWorld.create("hello", "world".hashCode());
    public TestInterface testInterface = new TestInterfaceImpl();

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
