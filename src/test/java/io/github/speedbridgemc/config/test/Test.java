package io.github.speedbridgemc.config.test;

import javax.swing.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class Test {
    public static SerialExecutorService executor;
    private static AtomicBoolean running;

    public static void main(String[] args) {
        executor = new SerialExecutorService();

        TestConfig cfg = TestConfigHandler.INSTANCE.get();
        System.out.println("testString = \"" + cfg.testString + "\"");
        cfg.testBool = !cfg.testBool;
        cfg.testString = "Hello world! " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        cfg.testInt += 12;
        boolean tmp = cfg.helloWorld.enabledThat;
        cfg.helloWorld.enabledThat = cfg.helloWorld.enabledThis;
        cfg.helloWorld.enabledThis = tmp;
        HashMap<String, String> nested = new HashMap<>();
        nested.put("yo", "yo!!!");
        nested.put("does this actually...", "work??");
        cfg.testNestedMap.put("aaaaa", nested);

        TestConfigHandler.INSTANCE.save();

        running = new AtomicBoolean(true);

        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                running.set(false);
                frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
            }
        });

        frame.setSize(200, 60);
        JLabel label = new JLabel("Press any button to exit.");
        frame.add(label);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        frame.requestFocus();

        while (running.get())
            executor.process();
        executor.shutdownNow();
    }
}
