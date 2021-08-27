package io.github.speedbridgemc.config.processor.impl;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.TypeName;
import io.github.speedbridgemc.config.Config;
import io.github.speedbridgemc.config.processor.api.Identifiable;
import io.github.speedbridgemc.config.processor.api.ProcessingWorker;
import io.github.speedbridgemc.config.processor.api.component.Component;
import io.github.speedbridgemc.config.processor.api.naming.NamingStrategy;
import io.github.speedbridgemc.config.processor.api.property.ConfigProperty;
import io.github.speedbridgemc.config.processor.api.property.ConfigPropertyBuilder;
import io.github.speedbridgemc.config.processor.api.property.StandardConfigPropertyFlags;
import io.github.speedbridgemc.config.processor.api.type.ConfigStruct;
import io.github.speedbridgemc.config.processor.api.type.ConfigType;
import io.github.speedbridgemc.config.processor.api.type.ConfigTypeKind;
import io.github.speedbridgemc.config.processor.api.type.provider.ConfigStructBuilder;
import io.github.speedbridgemc.config.processor.api.type.provider.ConfigTypeProvider;
import io.github.speedbridgemc.config.processor.api.type.provider.StructFactory;
import io.github.speedbridgemc.config.processor.api.type.provider.StructInstantiationStrategyBuilder;
import io.github.speedbridgemc.config.processor.api.util.AnnotationUtils;
import io.github.speedbridgemc.config.processor.api.util.Lazy;
import io.github.speedbridgemc.config.processor.api.util.MirrorUtils;
import io.github.speedbridgemc.config.processor.impl.naming.SnakeCaseNamingStrategy;
import io.github.speedbridgemc.config.processor.impl.type.provider.ConfigTypeProviderImpl;
import io.github.speedbridgemc.config.processor.impl.type.provider.StandardStructFactory;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
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
    public static String id(String path) {
        return "speedbridge-config:" + path;
    }

    private String version;
    private Messager messager;
    private Elements elements;
    private Types types;

    private static final class LazyInit<PW extends ProcessingWorker> {
        private final PW worker;
        private boolean initialized;

        public LazyInit(PW worker) {
            this.worker = worker;
            initialized = false;
        }

        public PW get(ProcessingEnvironment processingEnv) {
            if (!initialized) {
                worker.init(processingEnv);
                initialized = true;
            }
            return worker;
        }
    }

    private final HashMap<String, LazyInit<Component>> components = new HashMap<>();
    private final HashMap<String, LazyInit<NamingStrategy>> namingStrategies = new HashMap<>();

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
        messager.printMessage(Diagnostic.Kind.NOTE, "Running Speedbridge Config Annotation Processor v" + version);

        loadServices(Component.class, components);
        loadServices(NamingStrategy.class, namingStrategies);
    }

    private <PW extends ProcessingWorker> void loadServices(Class<PW> clazz,
                                                            HashSet<LazyInit<PW>> set) {
        ServiceLoader<PW> loader = ServiceLoader.load(clazz, ConfigProcessor.class.getClassLoader());
        for (PW worker : loader)
            set.add(new LazyInit<>(worker));
    }

    private <PW extends ProcessingWorker & Identifiable> void loadServices(Class<PW> clazz,
                                                                           HashMap<String, LazyInit<PW>> map) {
        ServiceLoader<PW> loader = ServiceLoader.load(clazz, ConfigProcessor.class.getClassLoader());
        for (PW worker : loader)
            map.put(worker.id(), new LazyInit<>(worker));
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

            ConfigTypeProvider typeProvider = new ConfigTypeProviderImpl();
            typeProvider.init(processingEnv);
            NamingStrategy namingStrategy = new SnakeCaseNamingStrategy();
            namingStrategy.init(processingEnv);
            StructFactory structFactory = new StandardStructFactory();
            structFactory.init(processingEnv);
            typeProvider.setNamingStrategy(namingStrategy, "");
            typeProvider.addStructFactory(structFactory);

            for (Config.StructOverride structOverride : config.structOverrides()) {
                TypeMirror mirror = AnnotationUtils.getClass(structOverride, Config.StructOverride::target);
                if (mirror.getKind() != TypeKind.DECLARED)
                    // TODO warning
                    continue;
                typeProvider.setStructOverride((DeclaredType) mirror, structOverride);
            }

            Lazy<ConfigType> stringLazy = Lazy.of(typeProvider.primitiveOf(ConfigTypeKind.STRING));
            DeclaredType identifierM = MirrorUtils.getDeclaredType(elements, "io.github.speedbridgemc.config.test.Identifier");
            typeProvider.addStruct(new ConfigStructBuilder(identifierM)
                    .property(ConfigPropertyBuilder.getter(stringLazy,
                            "value", "toString").build())
                    .instantiationStrategy(StructInstantiationStrategyBuilder.factory(TypeName.get(identifierM), "tryParse")
                            .param(stringLazy, "string", "value")
                            .build())
                    .build());

            ConfigType cType = typeProvider.fromMirror(type.asType());
            System.out.println(cType);
            if (cType.kind() == ConfigTypeKind.STRUCT)
                dumpConfigStruct((ConfigStruct) cType);
        }
    }

    private void dumpConfigStruct(ConfigStruct cType) {
        dumpConfigStruct(cType, "");
    }

    private void dumpConfigStruct(ConfigStruct cType, String indent) {
        System.out.format("%sinstantiation strategy: %s%n", indent, cType.instantiationStrategy());
        System.out.format("%s%d properties:%n", indent, cType.properties().size());
        for (ConfigProperty prop : cType.properties()) {
            ConfigType pType = prop.type();
            System.out.format("%s - %s %s%s%n", indent, pType, prop.name(), propAttr(prop));
            switch (pType.kind()) {
            case STRUCT:
                dumpConfigStruct((ConfigStruct) pType, indent + "     ");
                break;
            default:
                break;
            }
        }
    }

    private String propAttr(ConfigProperty prop) {
        StringBuilder sb = new StringBuilder();
        if (!prop.canSet())
            sb.append("readonly, ");
        if (prop.hasFlag(StandardConfigPropertyFlags.OPTIONAL))
            sb.append("optional, ");
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 2);
            sb.insert(0, " (").append(')');
        }
        return sb.toString();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return singleton(Config.class.getCanonicalName());
    }
}
