package io.github.speedbridgemc.config.test;

import org.jetbrains.annotations.NotNull;

public final class TestData {
    public static @NotNull TestData of(int data1, @NotNull String data2) {
        return new TestData(data1, data2);
    }

    private final int data1;
    private final @NotNull String data2;

    private TestData(int data1, @NotNull String data2) {
        this.data1 = data1;
        this.data2 = data2;
    }

    public int getData1() {
        return data1;
    }

    public @NotNull String getData2() {
        return data2;
    }
}
