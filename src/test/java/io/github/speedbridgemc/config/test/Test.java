package io.github.speedbridgemc.config.test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Random;

public class Test {
    public static void main(String[] args) {
        Random rnd = new Random();

        TestConfig cfg = TestConfigHandler.instance().get();
        System.out.println("testString = \"" + cfg.testString + "\"");
        cfg.testBool = !cfg.testBool;
        cfg.testString = "Hello world! " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        cfg.testInt += 12;
        boolean tmp = cfg.helloWorld.enabledThat;
        cfg.helloWorld.enabledThat = cfg.helloWorld.enabledThis;
        cfg.helloWorld.enabledThis = tmp;
        TestConfig.HelloWorld newHW = new TestConfig.HelloWorld();
        newHW.aFloat = rnd.nextFloat() * 10;
        cfg.testList2.add(newHW);
        cfg.testNestedList.add(new int[] { rnd.nextInt(30), rnd.nextInt(20), rnd.nextInt(10) });
        TestConfig.StringEntry newSE = new TestConfig.StringEntry();
        newSE.ayy = rnd.nextInt(40);
        newSE.lmao = new boolean[] { rnd.nextBoolean(), rnd.nextBoolean() };
        cfg.testStringKeysMap.put("yoyo", newSE);
        TestConfig.MapKey newMK = new TestConfig.MapKey();
        newMK.acto = rnd.nextFloat() * 20;
        newMK.testo = rnd.nextInt(10);
        TestConfig.MapValue newMV = new TestConfig.MapValue();
        newMV.helloWorld.intA = rnd.nextInt(100);
        newMV.helloWorld.intB = rnd.nextInt(100);
        newMV.helloWorld.intC = 6 + rnd.nextInt(30);
        cfg.testMap.put(newMK, newMV);
        HashMap<String, String> nested = new HashMap<>();
        nested.put("yo", "yo!!!");
        nested.put("does this actually...", "work??");
        cfg.testNestedMap.put("aaaaa", nested);

        TestConfigHandler.instance().save();
    }
}
