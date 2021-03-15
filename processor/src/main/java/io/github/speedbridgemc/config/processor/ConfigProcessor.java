package io.github.speedbridgemc.config.processor;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.squareup.javapoet.*;
import io.github.speedbridgemc.config.Component;
import io.github.speedbridgemc.config.Config;
import io.github.speedbridgemc.config.processor.api.ComponentContext;
import io.github.speedbridgemc.config.processor.api.ComponentProvider;
import io.github.speedbridgemc.config.processor.api.TypeUtils;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Generated;
import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import java.io.*;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@ApiStatus.Internal
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor.class)
public final class ConfigProcessor extends AbstractProcessor {
    private String version;
    private HashMap<String, ComponentProvider> componentProviders;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

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
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "Speedbridge Config's annotation processor couldn't find its \"version.properties\" file.");
        } catch (IOException e) {
            // couldn't read from version.properties
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "Speedbridge Config's annotation processor couldn't read its \"version.properties\" file due to an IO error: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            // version.properties is malformed
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "Speedbridge Config's annotation processor couldn't parse its \"version.properties\" file: " + e.getMessage());
        } catch (IllegalStateException e) {
            // version.properties doesn't contain a "version" property
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "Speedbridge Config's annotation processor's \"version.properties\" file doesn't contain a \"version\" property.");
        }
        if (version == null) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
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
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
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
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "@Config annotation applied to non-class element", element);
                continue;
            }
            TypeElement typeElement = (TypeElement) element;

            if (!TypeUtils.hasDefaultConstructor(typeElement)) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "@Config annotation applied to class with no public 0-parameter constructor", typeElement);
            }

            Config config = typeElement.getAnnotation(Config.class);
            String configPackage = processingEnv.getElementUtils().getPackageOf(typeElement).getQualifiedName().toString();
            String name = config.name();
            String handlerInterfaceName;
            String handlerInterfaceNameIn = config.handlerInterface();
            if (handlerInterfaceNameIn.contains("."))
                handlerInterfaceName = handlerInterfaceNameIn;
            else
                handlerInterfaceName = configPackage + "." + handlerInterfaceNameIn;
            TypeElement handlerInterfaceTypeElement = processingEnv.getElementUtils().getTypeElement(handlerInterfaceName);
            if (handlerInterfaceTypeElement == null) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "Missing handler interface class \"" + handlerInterfaceName + "\"", typeElement);
                continue;
            }
            ImmutableList<ExecutableElement> handlerInterfaceMethods = ImmutableList.copyOf(TypeUtils.getMethodsIn(handlerInterfaceTypeElement));
            TypeName handlerInterfaceTypeName = TypeName.get(handlerInterfaceTypeElement.asType());
            String handlerInterfacePackage = "";
            int splitIndex = handlerInterfaceName.lastIndexOf('.');
            if (splitIndex >= 0) {
                handlerInterfacePackage = handlerInterfaceName.substring(0, splitIndex);
                handlerInterfaceName = handlerInterfaceName.substring(splitIndex + 1);
            }
            String handlerName;
            String[] handlerNameIn = config.handlerName();
            if (handlerNameIn.length == 0) {
                handlerName = handlerInterfaceName + "Impl";
                if (!handlerInterfacePackage.isEmpty())
                    handlerName = handlerInterfacePackage + "." + handlerName;
            } else if (handlerNameIn.length == 1) {
                if (handlerNameIn[0].contains("."))
                    handlerName = handlerNameIn[0];
                else
                    handlerName = configPackage + handlerNameIn[0];
            }
            else {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "@Config annotation specifies more than one handler name", typeElement);
                continue;
            }

            boolean gotGet = false, gotReset = false, gotLoad = false, gotSave = false, gotLog = false;
            TypeMirror configTM = typeElement.asType();
            TypeMirror stringTM = TypeUtils.getTypeMirror(processingEnv, String.class.getCanonicalName());
            TypeMirror exceptionTM = TypeUtils.getTypeMirror(processingEnv, Exception.class.getCanonicalName());
            if (stringTM == null || exceptionTM == null)
                continue;
            for (ExecutableElement method : handlerInterfaceMethods) {
                String methodName = method.getSimpleName().toString();
                List<? extends VariableElement> params = method.getParameters();
                TypeMirror returnType = method.getReturnType();
                switch (methodName) {
                case "get":
                    if (params.size() == 0 && processingEnv.getTypeUtils().isSameType(returnType, configTM))
                        gotGet = true;
                    break;
                case "reset":
                case "load":
                case "save":
                    if (params.size() == 0 && returnType.getKind() == TypeKind.VOID) {
                        switch (methodName) {
                        case "reset":
                            gotReset = true;
                            break;
                        case "load":
                            gotLoad = true;
                            break;
                        case "save":
                            gotSave = true;
                            break;
                        }
                    }
                    break;
                case "log":
                    if (method.isDefault()
                            && params.size() == 2
                            && processingEnv.getTypeUtils().isSameType(params.get(0).asType(), stringTM)
                            && processingEnv.getTypeUtils().isSameType(params.get(1).asType(), exceptionTM))
                        gotLog = true;
                    break;
                }
            }
            if (!gotGet)
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "Handler interface is missing required method: " + typeElement.getSimpleName() + " get()", handlerInterfaceTypeElement);
            if (!gotReset)
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "Handler interface is missing required method: void reset()", handlerInterfaceTypeElement);
            if (!gotSave)
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "Handler interface is missing required method: void save()", handlerInterfaceTypeElement);
            if (!gotLoad)
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "Handler interface is missing required method: void load()", handlerInterfaceTypeElement);
            if (!gotLog)
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "Handler interface is missing required default method: <ignored> log(String, Exception)", handlerInterfaceTypeElement);
            if (!gotGet || !gotReset || !gotLoad || !gotSave || !gotLog)
                continue;

            ClassName nonNullAnnotation = getAnnotationName(typeElement, config.nonNullAnnotation(), "non-null");
            ClassName nullableAnnotation = getAnnotationName(typeElement, config.nullableAnnotation(), "nullable");
            String handlerPackage = "";
            splitIndex = handlerName.lastIndexOf('.');
            if (splitIndex >= 0) {
                handlerPackage = handlerName.substring(0, splitIndex);
                handlerName = handlerName.substring(splitIndex + 1);
            }
            TypeSpec.Builder classBuilder;
            try {
                classBuilder = TypeSpec.classBuilder(handlerName);
            } catch (IllegalArgumentException e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Handler name \"" + handlerName + "\" is invalid", typeElement);
                continue;
            }
            if (!handlerInterfacePackage.equals(handlerPackage))
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
            TypeName configTypeName = TypeName.get(typeElement.asType());
            FieldSpec.Builder configFieldBuilder = FieldSpec.builder(configTypeName, "config", Modifier.PRIVATE);
            if (nullableAnnotation != null)
                configFieldBuilder.addAnnotation(nullableAnnotation);
            classBuilder.addField(configFieldBuilder.build());
            MethodSpec.Builder getMethodBuilder = MethodSpec.methodBuilder("get")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(configTypeName);
            if (nonNullAnnotation != null)
                getMethodBuilder.addAnnotation(nonNullAnnotation);
            MethodSpec.Builder resetMethodBuilder = MethodSpec.methodBuilder("reset")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .addCode("config = new $T();\n", configTypeName);
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

            ImmutableList<@NotNull VariableElement> fields = ImmutableList.copyOf(TypeUtils.getFieldsIn(typeElement));

            Component[] components = config.components();
            for (Component component : components) {
                ComponentProvider provider = componentProviders.get(component.value());
                if (provider == null) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
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
                        configTypeName, params, getMethodBuilder, resetMethodBuilder, loadMethodBuilder, saveMethodBuilder);
                provider.process(name, typeElement, fields, ctx, classBuilder);
            }
            classBuilder.addMethod(getMethodBuilder.addCode(CodeBlock.builder()
                    .beginControlFlow("if (config == null)")
                    .addStatement("load()")
                    .endControlFlow()
                    .addStatement("return config")
                    .build()).build())
                    .addMethod(resetMethodBuilder.build())
                    .addMethod(loadMethodBuilder.build())
                    .addMethod(saveMethodBuilder.build());
            try {
                JavaFile.builder(handlerPackage, classBuilder.build())
                        .build()
                        .writeTo(processingEnv.getFiler());
            } catch (IOException e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Failed to write handler class");
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
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                "@Config annotation specifies more than one " + errName + " annotation", typeElement);
        return null;
    }
}
