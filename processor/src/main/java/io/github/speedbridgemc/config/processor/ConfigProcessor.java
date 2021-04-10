package io.github.speedbridgemc.config.processor;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.squareup.javapoet.*;
import io.github.speedbridgemc.config.Component;
import io.github.speedbridgemc.config.Config;
import io.github.speedbridgemc.config.LogLevel;
import io.github.speedbridgemc.config.processor.api.ComponentContext;
import io.github.speedbridgemc.config.processor.api.ComponentProvider;
import io.github.speedbridgemc.config.processor.api.MethodSignature;
import io.github.speedbridgemc.config.processor.api.TypeUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Generated;
import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import java.io.*;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * The main entry point of the annotation processor.<p>
 * Loads and initializes all {@link ComponentProvider}s, then processes all classes annotated with {@link Config}.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor.class)
public final class ConfigProcessor extends AbstractProcessor {
    private String version;
    private HashMap<String, ComponentProvider> componentProviders;
    private Messager messager;
    private Elements elements;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        messager = processingEnv.getMessager();
        elements = processingEnv.getElementUtils();

        try (InputStream is = getClass().getResourceAsStream("/version.properties")) {
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

        componentProviders = new HashMap<>();
        ServiceLoader<ComponentProvider> cpLoader = ServiceLoader.load(ComponentProvider.class, ConfigProcessor.class.getClassLoader());
        for (ComponentProvider provider : cpLoader)
            componentProviders.put(provider.getId(), provider);

        for (ComponentProvider componentProvider : componentProviders.values())
            componentProvider.init(processingEnv);
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
                    "Speedbridge Config annotation processor failed exceptionally!\n" + sw.toString());
            e.printStackTrace();
        }
        return true;
    }

    private void run(RoundEnvironment roundEnv) {
        if (componentProviders == null)
            return;
        for (Element element : roundEnv.getElementsAnnotatedWith(Config.class)) {
            if (element.getKind() != ElementKind.CLASS) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                        "@Config annotation applied to non-class element", element);
                continue;
            }
            TypeElement typeElement = (TypeElement) element;

            if (!TypeUtils.hasDefaultConstructor(typeElement)) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                        "@Config annotation applied to class with no public 0-parameter constructor", typeElement);
            }

            Config config = typeElement.getAnnotation(Config.class);
            String configPackage = elements.getPackageOf(typeElement).getQualifiedName().toString();
            String name = config.name();
            String handlerInterfaceName = config.handlerInterface();
            TypeElement handlerInterfaceTypeElement = elements.getTypeElement(handlerInterfaceName);
            if (handlerInterfaceTypeElement == null) {
                // try again with absolute name
                handlerInterfaceName = configPackage + "." + handlerInterfaceName;
                handlerInterfaceTypeElement = elements.getTypeElement(handlerInterfaceName);
            }
            if (handlerInterfaceTypeElement == null) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                        "Missing handler interface class \"" + handlerInterfaceName + "\"", typeElement);
                continue;
            }
            List<ExecutableElement> methods = TypeUtils.getMethodsIn(handlerInterfaceTypeElement);
            ImmutableList.Builder<MethodSignature> signatureBuilder = ImmutableList.builder();
            for (ExecutableElement method : methods)
                signatureBuilder.add(MethodSignature.fromElement(method));
            ImmutableList<MethodSignature> handlerInterfaceMethods = signatureBuilder.build();
            TypeName handlerInterfaceTypeName = TypeName.get(handlerInterfaceTypeElement.asType());
            String handlerInterfacePackage = "";
            int splitIndex = handlerInterfaceName.lastIndexOf('.');
            if (splitIndex >= 0) {
                handlerInterfacePackage = handlerInterfaceName.substring(0, splitIndex);
                handlerInterfaceName = handlerInterfaceName.substring(splitIndex + 1);
            }
            ClassName handlerName;
            String[] handlerNameIn = config.handlerName();
            if (handlerNameIn.length == 0)
                handlerName = ClassName.get(handlerInterfacePackage, handlerInterfaceName + "Impl");
            else if (handlerNameIn.length == 1) {
                if (handlerNameIn[0].contains(".")) {
                    splitIndex = handlerNameIn[0].lastIndexOf('.');
                    handlerName = ClassName.get(handlerNameIn[0].substring(0, splitIndex), handlerNameIn[1].substring(splitIndex + 1));
                } else
                    handlerName = ClassName.get(configPackage, handlerNameIn[0]);
            }
            else {
                messager.printMessage(Diagnostic.Kind.ERROR,
                        "@Config annotation specifies more than one handler name", typeElement);
                continue;
            }

            TypeMirror configTM = typeElement.asType();
            TypeMirror stringTM = TypeUtils.getTypeMirror(processingEnv, String.class.getCanonicalName());
            TypeMirror exceptionTM = TypeUtils.getTypeMirror(processingEnv, Exception.class.getCanonicalName());
            if (stringTM == null || exceptionTM == null)
                continue;
            TypeName configType = TypeName.get(configTM);
            TypeName logLvlName = ClassName.get(LogLevel.class);
            TypeName stringName = ClassName.get(String.class);
            TypeName exceptionName = ClassName.get(Exception.class);
            boolean gotGet = MethodSignature.contains(handlerInterfaceMethods,
                    MethodSignature.of(configType, "get"));
            boolean gotSet = MethodSignature.contains(handlerInterfaceMethods,
                    MethodSignature.of(TypeName.VOID, "set", configType));
            boolean gotReset = MethodSignature.contains(handlerInterfaceMethods,
                    MethodSignature.of(TypeName.VOID, "reset"));
            boolean gotLoad = MethodSignature.contains(handlerInterfaceMethods,
                    MethodSignature.of(TypeName.VOID, "load"));
            boolean gotSave = MethodSignature.contains(handlerInterfaceMethods,
                    MethodSignature.of(TypeName.VOID, "save"));
            boolean gotLog = MethodSignature.contains(handlerInterfaceMethods,
                    MethodSignature.ofDefault("log", logLvlName, stringName, exceptionName));
            boolean gotPostLoad = MethodSignature.contains(handlerInterfaceMethods,
                    MethodSignature.ofDefault(configType, "postLoad", configType));
            boolean gotPostSave = MethodSignature.contains(handlerInterfaceMethods,
                    MethodSignature.ofDefault("postSave", configType));
            if (!gotGet)
                messager.printMessage(Diagnostic.Kind.ERROR,
                        "Handler interface is missing required method: " + typeElement.getSimpleName() + " get()", handlerInterfaceTypeElement);
            if (!gotReset)
                messager.printMessage(Diagnostic.Kind.ERROR,
                        "Handler interface is missing required method: void reset()", handlerInterfaceTypeElement);
            if (!gotSave)
                messager.printMessage(Diagnostic.Kind.ERROR,
                        "Handler interface is missing required method: void save()", handlerInterfaceTypeElement);
            if (!gotLoad)
                messager.printMessage(Diagnostic.Kind.ERROR,
                        "Handler interface is missing required method: void load()", handlerInterfaceTypeElement);
            if (!gotLog)
                messager.printMessage(Diagnostic.Kind.ERROR,
                        "Handler interface is missing required default method: <ignored> log(LogLevel, String, Exception)", handlerInterfaceTypeElement);
            if (!gotGet || !gotReset || !gotLoad || !gotSave || !gotLog)
                continue;

            ClassName nonNullAnnotation = getAnnotationName(typeElement, config.nonNullAnnotation(), "non-null");
            ClassName nullableAnnotation = getAnnotationName(typeElement, config.nullableAnnotation(), "nullable");
            TypeSpec.Builder classBuilder;
            try {
                classBuilder = TypeSpec.classBuilder(handlerName);
            } catch (IllegalArgumentException e) {
                messager.printMessage(Diagnostic.Kind.ERROR, "Handler name \"" + handlerName + "\" is invalid", typeElement);
                continue;
            }
            if (!handlerInterfacePackage.equals(handlerName.packageName()))
                classBuilder.addModifiers(Modifier.PUBLIC);
            classBuilder
                    .addAnnotation(AnnotationSpec.builder(Generated.class)
                            .addMember("value", "$S", getClass().getCanonicalName())
                            .addMember("date", "$S", OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                            .addMember("comments", "$S", "Generated by Speedbridge Config's annotation processor v" + version)
                            .build())
                    .addModifiers(Modifier.FINAL)
                    .addSuperinterface(handlerInterfaceTypeName)
                    .addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC).build());
            FieldSpec.Builder configFieldBuilder = FieldSpec.builder(configType, "config", Modifier.PRIVATE);
            if (nullableAnnotation != null)
                configFieldBuilder.addAnnotation(nullableAnnotation);
            classBuilder.addField(configFieldBuilder.build());
            MethodSpec.Builder getMethodBuilder = MethodSpec.methodBuilder("get")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(configType);
            if (nonNullAnnotation != null)
                getMethodBuilder.addAnnotation(nonNullAnnotation);
            MethodSpec.Builder setMethodBuilder = null;
            if (gotSet) {
                ParameterSpec.Builder configParamBuilder = ParameterSpec.builder(configType, "config");
                if (nonNullAnnotation != null)
                    configParamBuilder.addAnnotation(nonNullAnnotation);
                setMethodBuilder = MethodSpec.methodBuilder("set")
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(configParamBuilder.build())
                        .addStatement("this.config = config");
            }
            MethodSpec.Builder resetMethodBuilder = MethodSpec.methodBuilder("reset")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .addStatement("config = new $T()", configType);
            MethodSpec.Builder loadMethodBuilder = MethodSpec.methodBuilder("load")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC);
            if (nonNullAnnotation != null)
                loadMethodBuilder.addAnnotation(nonNullAnnotation);
            MethodSpec.Builder saveMethodBuilder = MethodSpec.methodBuilder("save")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC);
            if (nonNullAnnotation != null)
                saveMethodBuilder.addAnnotation(nonNullAnnotation);
            CodeBlock.Builder postLoadBuilder = CodeBlock.builder(), postSaveBuilder = CodeBlock.builder();

            ImmutableList<@NotNull VariableElement> fields = ImmutableList.copyOf(TypeUtils.getFieldsToSerialize(typeElement));

            Component[] components = config.components();
            for (Component component : components) {
                ComponentProvider provider = componentProviders.get(component.value());
                if (provider == null) {
                    messager.printMessage(Diagnostic.Kind.ERROR,
                            "@Config annotation specifies @Component with unknown ID \"" + component.value() + "\"", typeElement);
                    continue;
                }
                String[] paramsIn = component.params();
                Multimap<String, String> params = MultimapBuilder.hashKeys().arrayListValues().build();
                for (String paramIn : paramsIn) {
                    ArrayList<String> values = new ArrayList<>();
                    if (paramIn.contains("=")) {
                        String[] kv = paramIn.split("=");
                        paramIn = kv[0];
                        String valueIn = kv[1];
                        if (valueIn.contains(","))
                            Collections.addAll(values, valueIn.split(","));
                        else
                            values.add(valueIn);
                    }
                    params.putAll(paramIn, values);
                }
                ComponentContext ctx = new ComponentContext(handlerName, handlerInterfaceTypeName, handlerInterfaceTypeElement,
                        handlerInterfaceMethods, nonNullAnnotation, nullableAnnotation,
                        configType, params, getMethodBuilder, resetMethodBuilder, loadMethodBuilder, saveMethodBuilder, postLoadBuilder, postSaveBuilder, setMethodBuilder);
                provider.process(name, typeElement, fields, ctx, classBuilder);
            }

            if (gotPostLoad)
                loadMethodBuilder.addStatement("config = postLoad(config)");
            loadMethodBuilder.addCode(postLoadBuilder.build());
            if (gotPostSave)
                saveMethodBuilder.addStatement("postSave(config)");
            saveMethodBuilder.addCode(postSaveBuilder.build());

            classBuilder.addMethod(getMethodBuilder.addCode(
                    CodeBlock.builder()
                            .beginControlFlow("if (config == null)")
                            .addStatement("load()")
                            .endControlFlow()
                            .addStatement("return config")
                            .build())
                    .build());
            if (setMethodBuilder != null)
                classBuilder.addMethod(setMethodBuilder.build());
            classBuilder
                    .addMethod(resetMethodBuilder.build())
                    .addMethod(loadMethodBuilder.build())
                    .addMethod(saveMethodBuilder.build());
            try {
                JavaFile.builder(handlerName.packageName(), classBuilder.build())
                        .build()
                        .writeTo(processingEnv.getFiler());
            } catch (IOException e) {
                messager.printMessage(Diagnostic.Kind.ERROR, "Failed to write handler class");
                e.printStackTrace();
            }
        }
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(Config.class.getCanonicalName());
    }

    private @Nullable ClassName getAnnotationName(@NotNull TypeElement typeElement, @NotNull String @NotNull [] in, @NotNull String errName) {
        if (in.length == 0)
            return null;
        else if (in.length == 1) {
            String aName = in[0];
            String aPackage = "";
            int splitIndex = aName.lastIndexOf('.');
            if (splitIndex >= 0) {
                aPackage = aName.substring(0, splitIndex);
                aName = aName.substring(splitIndex + 1);
            }
            return ClassName.get(aPackage, aName);
        }
        messager.printMessage(Diagnostic.Kind.ERROR,
                "@Config annotation specifies more than one " + errName + " annotation", typeElement);
        return null;
    }
}
