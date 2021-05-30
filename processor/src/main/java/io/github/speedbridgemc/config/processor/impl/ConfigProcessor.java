package io.github.speedbridgemc.config.processor.impl;

import com.google.auto.service.AutoService;
import io.github.speedbridgemc.config.Config;
import io.github.speedbridgemc.config.processor.api.type.ConfigTypeKind;
import io.github.speedbridgemc.config.processor.api.type.ConfigTypeProvider;
import io.github.speedbridgemc.config.processor.impl.type.ConfigTypeProviderImpl;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
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
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        // TODO collect configuration stuff
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

            ConfigTypeProvider provider = new ConfigTypeProviderImpl();
            provider.init(processingEnv);

            System.out.println("Composite tests:");

            System.out.println("String[][] = " + provider.arrayOf(provider.arrayOf(provider.primitiveOf(ConfigTypeKind.STRING))));
            System.out.println("Map<String, int[]> = " +
                    provider.mapOf(provider.primitiveOf(ConfigTypeKind.STRING), provider.arrayOf(provider.primitiveOf(ConfigTypeKind.INT))));

            System.out.println("Mirror tests:");

            TypeElement arrayListTE = elements.getTypeElement(ArrayList.class.getCanonicalName());
            TypeMirror intArrListTM = types.getDeclaredType(arrayListTE, types.getArrayType(types.getPrimitiveType(TypeKind.INT)));
            System.out.println("ArrayList<int[]> = " + provider.fromMirror(intArrListTM));

            TypeMirror stringTM = elements.getTypeElement(String.class.getCanonicalName()).asType();
            TypeMirror uuidTM = elements.getTypeElement(UUID.class.getCanonicalName()).asType();
            TypeElement hashMapTE = elements.getTypeElement(HashMap.class.getCanonicalName());
            TypeMirror stringUuidArrTM = types.getDeclaredType(hashMapTE, stringTM, types.getArrayType(uuidTM));
            System.out.println("HashMap<String, UUID[]> = " + provider.fromMirror(stringUuidArrTM));
        }
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return singleton(Config.class.getCanonicalName());
    }

}
