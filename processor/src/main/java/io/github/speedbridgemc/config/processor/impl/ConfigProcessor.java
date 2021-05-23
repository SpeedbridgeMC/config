package io.github.speedbridgemc.config.processor.impl;

import com.google.auto.service.AutoService;
import io.github.speedbridgemc.config.Config;
import io.github.speedbridgemc.config.processor.api.ConfigValue;
import io.github.speedbridgemc.config.processor.api.scan.ConfigValueExtensionScanner;
import io.github.speedbridgemc.config.processor.api.scan.ConfigValueNamingStrategy;
import io.github.speedbridgemc.config.processor.api.scan.ConfigValueScanner;
import io.github.speedbridgemc.config.processor.impl.scan.IdentityConfigValueNamingStrategy;
import io.github.speedbridgemc.config.processor.impl.scan.StandardExtensionScanner;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.io.*;
import java.util.*;

import static java.util.Collections.singleton;

/**
 * The main entry point of the annotation processor.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor.class)
public final class ConfigProcessor extends AbstractProcessor {
    private String version;
    private Messager messager;
    private Elements elements;
    private Types types;

    private HashMap<String, ConfigValueScanner> valueScanners;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        messager = processingEnv.getMessager();
        elements = processingEnv.getElementUtils();
        types = processingEnv.getTypeUtils();

        try (InputStream is = ConfigProcessor.class.getResourceAsStream("/version.properties")) {
            if (is == null)
                throw new FileNotFoundException();
            Properties props = new Properties();
            props.load(is);
            version = props.getProperty("version");
            if (version == null)
                throw new IllegalStateException();
        } catch (FileNotFoundException e) {
            // version.properties isn't there
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "Couldn't find \"version.properties\" file.");
        } catch (IOException e) {
            // couldn't read from version.properties
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "Couldn't read \"version.properties\" file due to an IO error: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            // version.properties is malformed
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "Couldn't parse \"version.properties\" file: " + e.getMessage());
        } catch (IllegalStateException e) {
            // version.properties doesn't contain a "version" property
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "\"version.properties\" file doesn't contain a \"version\" property.");
        }
        if (version == null) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "Your build of Speedbridge Config's annotation processor is probably broken!");
            return;
        }

        final ClassLoader cl = ConfigProcessor.class.getClassLoader();

        valueScanners = new HashMap<>();
        for (ConfigValueScanner scanner : ServiceLoader.load(ConfigValueScanner.class, cl))
            valueScanners.put(scanner.id(), scanner);

        for (ConfigValueScanner scanner : valueScanners.values())
            scanner.init(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        try {
            run(roundEnv);
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "Speedbridge Config annotation processor failed exceptionally!\n" + sw);
        }
        return true;
    }

    private void run(RoundEnvironment roundEnv) {
        for (Element annotatedElem : roundEnv.getElementsAnnotatedWith(Config.class)) {
            if (!(annotatedElem instanceof TypeElement)) {
                messager.printMessage(Diagnostic.Kind.MANDATORY_WARNING, "Non-type element annotated with @Config", annotatedElem);
                continue;
            }
            TypeElement type = (TypeElement) annotatedElem;
            Config config = type.getAnnotation(Config.class);

            // scan for values
            // TODO actually allow the user to configure this
            ConfigValueNamingStrategy namingStrategy = new IdentityConfigValueNamingStrategy();
            namingStrategy.init(processingEnv);
            ConfigValueExtensionScanner extensionScanner = new StandardExtensionScanner();
            extensionScanner.init(processingEnv);
            ConfigValueScanner.Context scanCtx = new ConfigValueScanner.Context(namingStrategy, "", singleton(extensionScanner));
            ArrayList<ConfigValue> values = new ArrayList<>();
            for (Map.Entry<String, ConfigValueScanner> entry : valueScanners.entrySet()) {
                try {
                    entry.getValue().findValues(scanCtx, type, config, values::add);
                } catch (Exception e) {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    e.printStackTrace(pw);
                    messager.printMessage(Diagnostic.Kind.ERROR,
                            "Value scanner \"" + entry.getKey() + "\" failed exceptionally!\n" + sw);
                }
            }

            // TODO do something with the values
            // debugging for now...
            System.out.format("Found %d values in \"%s\"!%n", values.size(), type.getQualifiedName().toString());
            for (ConfigValue value : values)
                System.out.format(" - %s%n", value.toString());
        }
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return singleton(Config.class.getCanonicalName());
    }

}
