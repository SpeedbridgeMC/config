package io.github.speedbridgemc.config.test;

import io.github.speedbridgemc.config.*;
import io.github.speedbridgemc.config.serialize.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Config(name = "test", handlerInterface = "TestConfigHandler",
        components = {
        @Component(
                value = "speedbridge-config:serializer",
                params = { "provider=speedbridge-config:jankson", "options=+watchFileForChanges" }
        ),
                @Component("speedbridge-config:validator"),
                @Component("speedbridge-config:listener"),
                @Component("speedbridge-config:remote_storage")
        },
        nonNullAnnotation = "org.jetbrains.annotations.NotNull", nullableAnnotation = "org.jetbrains.annotations.Nullable")
public final class TestConfig {
    public boolean testBool = false;
    @ThrowIfMissing(message = "test string is :crab:")
    public String testString = "hello world";
    @FloatingRange(max = 50.2)
    public float testFloat = 21.5f;
    @EnforceNotNull @IntegerRange(min = 24, max = 48, minMode = RangeMode.EXCLUSIVE, maxMode = RangeMode.INCLUSIVE, mode = EnforceMode.TRY_FIX)
    public Integer testInt = 26;
    public TestEnum testEnum = TestEnum.FOO;
    public @EnforceNotNull TestEnum2 testEnum2 = TestEnum2.ABC;
    public NetworkEnum networkEnum = NetworkEnum.ACTION_TWO;
    @UseDefaultIfMissing
    public HelloWorld helloWorld = new HelloWorld();
    public Nested3 nested3 = new Nested3();
    public int[] testArray = new int[] { 1, 2, 3 };
    public @EnforceNotNull @IntegerRange(max = 10) int[][] testNestedArray = new int[][] { new int[] { 1, 2, 3 } };
    public @EnforceNotNull String[] testArray2 = new String[] { "abc", "def", "ghi" };
    public @EnforceNotNull @FloatingRange(max = 20) float[] testArray3 = new float[] { 1.2f, 2.44f, 4.8f };
    public @EnforceNotNull List<String> testList = new ArrayList<>();
    public @EnforceNotNull @IntegerRange(max = 52) List<int[]> testNestedList = new ArrayList<>();
    public @EnforceNotNull List<HelloWorld> testList2 = new ArrayList<>();
    public Map<String, StringEntry> testStringKeysMap = new HashMap<>();
    public Map<MapKey, MapValue> testMap = new HashMap<>();
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
        @SerializedName("dumbAlienMeme")
        public int ayy = 2;
        public boolean[] lmao = new boolean[] { false, true, false };
    }

    public static final class MapKey {
        public int testo = 42;
        public float acto = 0.2f;
    }

    public static final class MapValue {
        @SerializedName("helloWorld")
        @SerializedAliases("HELLOWORLD")
        public HelloWorld helloWorld = new HelloWorld();
    }

    public enum TestEnum implements KeyedEnum<String> {
        FOO("foo"), BAR("bar"), BAZ("baz") {
            @Override
            public @NotNull String @NotNull [] getAliases() {
                return new String[] { "baz_override" };
            }
        };

        private final @NotNull String id;

        TestEnum(@NotNull String id) {
            this.id = id;
        }

        public @NotNull String getId() {
            return id;
        }

        @Override
        public @NotNull String getKey() {
            return getId();
        }
    }

    public enum TestEnum2 {
        ABC(0), DEF(1), GHI(2);

        private final int id;

        TestEnum2(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }
    }

    public enum NetworkEnum implements KeyedEnum<Integer> {
        ACTION_ONE(0), ACTION_TWO(1), ACTION_THREE(2);

        private final int id;

        NetworkEnum(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        @Override
        public @NotNull Integer getKey() {
            return id;
        }

        @KeyedEnumDeserializer
        public static @NotNull NetworkEnum fromId(int id) {
            return ID_TO_VALUE.get(id);
        }

        private static final HashMap<Integer, NetworkEnum> ID_TO_VALUE;

        static {
            ID_TO_VALUE = new HashMap<>();
            for (NetworkEnum e : values())
                ID_TO_VALUE.put(e.getId(), e);
        }
    }
}
