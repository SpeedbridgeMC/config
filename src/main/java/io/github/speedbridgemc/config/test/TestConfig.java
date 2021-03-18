package io.github.speedbridgemc.config.test;

import io.github.speedbridgemc.config.*;
import io.github.speedbridgemc.config.serialize.ThrowIfMissing;
import io.github.speedbridgemc.config.serialize.UseDefaultIfMissing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Config(name = "test", handlerInterface = "TestConfigHandler",
        components = {
        @Component(
                value = "speedbridge-config:serializer",
                params = "provider=speedbridge-config:jankson"
                ),
                @Component("speedbridge-config:remote-storage"),
                @Component("speedbridge-config:validator")
        },
        nonNullAnnotation = "org.jetbrains.annotations.NotNull", nullableAnnotation = "org.jetbrains.annotations.Nullable")
public final class TestConfig {
    public boolean testBool = false;
    @ThrowIfMissing(message = "test string is :crab:, also \"%s\" is missing")
    public String testString = "hello world";
    @FloatingRange(max = 50.2)
    public float testFloat = 21.5f;
    @EnforceNotNull @IntegerRange(min = 24, max = 48, minMode = RangeMode.EXCLUSIVE, maxMode = RangeMode.INCLUSIVE)
    public Integer testInt = 26;
    @UseDefaultIfMissing
    public HelloWorld helloWorld = new HelloWorld();
    public Nested3 nested3 = new Nested3();
    public int[] testArray = new int[] { 1, 2, 3 };
    public List<String> testList = new ArrayList<>();
    public List<int[]> testNestedList = new ArrayList<>();
    public List<HelloWorld> testList2 = new ArrayList<>();
    public Map<String, StringEntry> testStringKeysMap = new HashMap<>();
    public Map<MapKey, MapEntry> testMap = new HashMap<>();
    public Map<String, Map<String, String>> testNestedMap = new HashMap<>();

    public TestConfig() {
        testList.add("a");
        testList.add("b");
        testList.add("c");
        testNestedList.add(new int[] { 1, 2, 3 });
        testNestedList.add(new int[] { 4, 5, 6 });
    }

    @IntegerRange(max = 100)
    public static final class HelloWorld {
        public boolean enabledThis = true;
        @UseDefaultIfMissing
        public boolean enabledThat = false;
        public String mode = "modes";
        public int intA = 25;
        public int intB = 75;
        @IntegerRange(min = 5, max = 55, mode = EnforceMode.USE_DEFAULT)
        public int intC = 10;
        @FloatingRange(min = 2, max = 22, mode = EnforceMode.TRY_FIX)
        public float aFloat = 2.5f;
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

    public static final class StringEntry {
        public int ayy = 2;
        public boolean[] lmao = new boolean[] { false, true, false };
    }

    public static final class MapKey {
        public int testo = 42;
        public float acto = 0.2f;
    }

    public static final class MapEntry {
        public HelloWorld helloWorld;
    }
}
