package io.github.speedbridgemc.config.test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Test {
    public static void main(String[] args) {
        TestConfig cfg = TestConfigHandler.get();
        System.out.println("testString = \"" + cfg.testString + "\"");
        cfg.testBool = !cfg.testBool;
        cfg.testString = "Hello world! " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        TestConfigHandler.save();
    }
}
