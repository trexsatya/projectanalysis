package com.satya.projectanalysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.Launcher;
import spoon.processing.ProcessingManager;
import spoon.reflect.factory.Factory;
import spoon.support.QueueProcessingManager;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.satya.projectanalysis.JavaUtils.wrapInRuntimeException;
import static com.satya.projectanalysis.MethodProcessor.TO_BE_REMOVED;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.text.StringEscapeUtils.escapeJava;

public class Analyzer {
     public static Logger LOG = LoggerFactory.getLogger(Analyzer.class);

    public static void main(String[] args) throws Exception {
        String sourceCodePath1 = "/Users/satyendra.kumar/Documents/DCPIPE/debt-collection-pipeline-service/dcpipe/dcpipe-persistence/src/main/java";
        String sourceCodePath2 = "/Users/satyendra.kumar/Documents/DCPIPE/debt-collection-pipeline-service/dcpipe/dcpipe-domain/src/main/java";
        String sourceCodePath3 = "/Users/satyendra.kumar/Documents/DCPIPE/debt-collection-pipeline-service/dcpipe/dcpipe-service/src/main/java";
        String sourceCodePath4 = "/Users/satyendra.kumar/Documents/DCPIPE/debt-collection-pipeline-service/kafka/corebanking-events/build/src-generated/main/avro-java";
        String sourceCodePath5 = "/Users/satyendra.kumar/Documents/DCPIPE/debt-collection-pipeline-service/kafka/dcpipe-internal-events/build/src-generated/main/avro-java";
        String sourceCodePath6 = "/Users/satyendra.kumar/Documents/DCPIPE/debt-collection-pipeline-service/kafka/sal-container-events/build/src-generated/main/avro-java";


        String byteCodePath1 = "/Users/satyendra.kumar/Documents/DCPIPE/debt-collection-pipeline-service/dcpipe/dcpipe-service/build/classes/java/main";
        String byteCodePath2 = "/Users/satyendra.kumar/Documents/DCPIPE/debt-collection-pipeline-service/dcpipe/dcpipe-domain/build/classes/java/main";
        String byteCodePath3 = "/Users/satyendra.kumar/Documents/DCPIPE/debt-collection-pipeline-service/dcpipe/dcpipe-persistence/build/classes/java/main";
        String classpathForJars = "/Users/satyendra.kumar/.gradle/caches/modules-2/files-2.1/";

        String testSourcePath = "/Users/satyendra.kumar/IdeaProjects/projectanalysis/src/main/java/sample/code";

        List<String> sourceCodePaths = getSourceCodePaths(
                sourceCodePath1, sourceCodePath2, sourceCodePath3
                , sourceCodePath4, sourceCodePath5, sourceCodePath6
        );

        sourceCodePaths = List.of("/Users/satyendra.kumar/Documents/DCPIPE/debt-collection-pipeline-service/testing/dcpipe-simulator/src/test/java",
                "/Users/satyendra.kumar/Documents/DCPIPE/debt-collection-pipeline-service/testing/dcpipe-simulator/src/main/java");

        analyze(
//                List.of(testSourcePath),
                sourceCodePaths,
                List.of("/Users/satyendra.kumar/Documents/DCPIPE/debt-collection-pipeline-service/testing/dcpipe-simulator/build/classes/java/test"),
                "com.klarna.dcpipe",
                classpathForJars);
    }

    private static List<String> getSourceCodePaths(String... sourceCodePaths) {
        AtomicInteger count = new AtomicInteger(0);
        return Arrays.stream(sourceCodePaths)
                .map(it -> delombok(it, count.incrementAndGet()))
                .collect(toList());
    }

    private static String delombok(String path, int n) {
        String jarPath =
                "/Users/satyendra.kumar/.gradle/caches/modules-2/files-2.1/org.projectlombok/lombok/1.18.24/13a394eed5c4f9efb2a6d956e2086f1d81e857d9/lombok-1.18.24.jar";
        String targetPath = "build/src-delomboked-2"+n;
        String command = "java -jar " + jarPath + " delombok " + path + " -d " + targetPath;
        try {
            Runtime.getRuntime().exec(command);
        } catch (IOException e) {
            LOG.info(e.getMessage());
            return path;
        }
        return targetPath;
    }

    /**
     *
     * @param sourceCodePath where to find .JAVA files
     * @param byteCodePath where to find .CLASS files
     * @param packageName package name to include classes
     * @param classpath directory containing JAR files
     * @throws Exception ex
     */
    private static void analyze(List<String> sourceCodePath, List<String> byteCodePath, String packageName, String classpath) throws Exception {
        try {
            performStaticAnalysis(sourceCodePath);
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
        performBytecodeAnalysis(byteCodePath, packageName, classpath);
    }

    private static void performBytecodeAnalysis(List<String> directories, String packageName, String classpath) throws NotFoundException, IOException {
        ClassPool classPool = new ClassPool();

        List<String> allJarFiles = getAllJarFiles(classpath);
        LOG.info("{} jar files.", allJarFiles.size());

        allJarFiles.forEach(jarFile -> wrapInRuntimeException(()-> classPool.appendClassPath(jarFile), false));

        directories.forEach(directory -> wrapInRuntimeException(() -> classPool.appendClassPath(directory)));

        classPool.appendSystemPath();

        List<String> fullClassNames = getClassNamesFromDirectory(directories, packageName);

        JavassistClassProcessor javassistClassProcessor = new JavassistClassProcessor();

        fullClassNames.stream().filter(x -> !x.isEmpty()).forEach(wrapInRuntimeException(fullClassName -> {
//            System.out.println("className:" + fullClassName);
            CtClass ctClass = classPool.get(fullClassName);
            try {
                javassistClassProcessor.process(ctClass);
            } catch (Exception | LinkageError ex) {
                LOG.info("Error {}", ex.toString());
            }
        }, false));

//        Global.INSTANCE.getRelationships().forEach((k, v) -> System.out.println(k));
    }

    private static List<String> getAllJarFiles(String classpath) throws IOException {
        try (Stream<Path> paths = Files.walk(Paths.get(classpath))) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(file -> file.toString().endsWith(".jar"))
                    .map(file -> file.toAbsolutePath().toString())
                    .collect(toList());
        }
    }

    private static List<String> getClassNamesFromDirectory(List<String> directories, String packageName) throws IOException {
        Function<String,String> classPathToFullyQualifiedClassName = p -> extractClassName(p, packageName);

        return directories.stream().map(wrapInRuntimeException(directory -> {
            try (Stream<Path> paths = Files.walk(Paths.get(directory))) {
                List<String> classFilePaths = paths
                        .filter(Files::isRegularFile)
                        .filter(file -> file.toString().endsWith(".class"))
                        .map(file -> file.toAbsolutePath().toString())
                        .collect(toList());
                return classFilePaths.stream().map(classPathToFullyQualifiedClassName).collect(toList());
            }
        })).flatMap(strings -> Stream.of(strings.toArray(new String[0]))).collect(toList());
    }

    private static String extractClassName(String filePath, String packageName){
        String separator = escapeJava(File.separator);
        String packageNameAsPath = packageName.replaceAll("\\.", separator);
        packageNameAsPath = escapeJava(packageNameAsPath);
        Pattern pattern = Pattern.compile(".*" + separator + "(" + packageNameAsPath + ".*)$");
        Matcher matcher = pattern.matcher(filePath);
        if (matcher.matches()) {
            return matcher.group(1).replaceAll(separator, ".").replaceAll(".class","");
        }
        LOG.warn("Could not extract fully qualified class name from: {} using packageName {}", filePath, packageName);
        return "";
    }

    private static void performStaticAnalysis(List<String> paths) throws IOException {
        Launcher launcher = new Launcher();

        Collection<String> allJavaFiles = getAllJavaFiles(paths);
        allJavaFiles.forEach(launcher::addInputResource);

        launcher.getEnvironment().setAutoImports(true); // optional

        launcher.getEnvironment().setNoClasspath(true); // optional

        launcher.run();
        final Factory factory = launcher.getFactory();
        ProcessingManager processingManager = new QueueProcessingManager(factory);
        final ClassProcessor processor = new ClassProcessor();

        processingManager.addProcessor(processor);
        processingManager.addProcessor(new MethodProcessor());
        processingManager.addProcessor(new InterfaceProcessor());
        processingManager.addProcessor(new GenericProcessor());

        processingManager.process(factory.Class().getAll());
        processingManager.process(factory.Interface().getAll());

        processingManager = new QueueProcessingManager(factory);

        List<String> anyEvent = List.of("com.klarna.dcpipe.service.domain.event");
        MethodInvocationProcessor methodInvocationProcessor = new MethodInvocationProcessor()
                .addInterestingTarget("com.klarna.dcpipe.service.event.EventProducer", anyEvent);
        Global.INSTANCE.implementationsOf("com.klarna.dcpipe.service.event.EventProducer")
                        .forEach(eventProducer -> methodInvocationProcessor.addInterestingTarget(eventProducer.className, anyEvent));
        Global.INSTANCE.implementationsOf("com.klarna.dcpipe.service.handler.DomainEventHandler")
                        .forEach(handler -> methodInvocationProcessor.addInterestingTarget(handler.className, anyEvent));

        processingManager.addProcessor(methodInvocationProcessor);
//        Global.INSTANCE.getRelationships().forEach((k, v) -> System.out.println(k));
        processingManager.process(factory.Class().getAll());

        methodInvocationProcessor.getOutput().forEach(it -> {
            MethodInvocationProcessor.InvocationDetail invocationDetail = it.get();
//            System.out.println(invocationDetail);
            List<MethodInvocationProcessor.ArgumentDetail> argumentDetails = invocationDetail.getArgumentDetails();
            argumentDetails.forEach(argumentDetail -> EventFlowNetwork.add(argumentDetail.getType(), invocationDetail));
        });

        String domainEventHandlerName = "com.klarna.dcpipe.service.handler.DomainEventHandler";
        Global.INSTANCE.implementationsOf(domainEventHandlerName)
                .forEach(impl -> {
                    List<String> typeArgsForEventHandler = impl.getImplementss().stream().filter(x -> x.getName().equals(domainEventHandlerName))
                            .findFirst()
                            .map(ClassData.ImplementsType::getTypeArguments)
                            .orElse(Collections.emptyList());
                    //TODO: Add support for regex
                    Optional<String> eventType = typeArgsForEventHandler.stream().filter(x -> x.startsWith("com.klarna.dcpipe.service.domain.event.")).findFirst();
                    eventType.ifPresent(type -> {
                        MethodProcessor.MethodDetail methodDetail = Global.ClassPool.getMethod(impl.getClassName(), "handle(" + type + ")");
                        EventFlowNetwork.add(type, methodDetail);
                    });
                });

        File outFile = new File("/Users/satyendra.kumar/Library/Application Support/JetBrains/IntelliJIdea2022.1/scratches/event_network.json");
        ObjectMapper mapper = new ObjectMapper();

        System.out.println(Global.INSTANCE.implementationsOf("org.apache.avro.specific.SpecificRecord"));

        try(var writer = new BufferedWriter(new FileWriter(outFile))){
            writer.write(mapper.writeValueAsString(EventFlowNetwork.getEvents()));
            writer.flush();
        }

        MethodProcessor.linesForClasses.forEach((path, lines) -> {
            try {
                modifyFiles(path, lines);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static void modifyFiles(String path, List<String> lines) throws IOException {
        lines.add(1, "import static org.mockito.BDDMockito.given;");
        lines.add(1, "import static org.mockito.BDDMockito.then;");
        String modified = lines.stream().filter(it -> !it.contains(TO_BE_REMOVED)).collect(Collectors.joining("\n"));
        System.out.println(modified);
        BufferedWriter writer = new BufferedWriter(new FileWriter(path));
        writer.write(modified);
        writer.flush();
    }

    private static Set<String> getAllJavaFiles(List<String> paths) {
        return paths.stream().flatMap(Analyzer::getFiles).collect(Collectors.toSet());
    }

    private static Stream<String> getFiles(String path) {
        try (Stream<Path> paths = Files.walk(Paths.get(path))) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(file -> file.toString().endsWith(".java"))
                    .map(file -> file.toAbsolutePath().toString())
                    .collect(toList()).stream();
        } catch (IOException e) {
            return Stream.of();
        }
    }

    private void executeCommand(String command) {
        try {
            log(command);
            Process process = Runtime.getRuntime().exec(command);
            logOutput(process.getInputStream(), "");
            logOutput(process.getErrorStream(), "Error: ");
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void logOutput(InputStream inputStream, String prefix) {
        new Thread(() -> {
            Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8);
            while (scanner.hasNextLine()) {
                synchronized (this) {
                    log(prefix + scanner.nextLine());
                }
            }
            scanner.close();
        }).start();
    }

    private static SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss:SSS");

    private synchronized void log(String message) {
//        System.out.println(format.format(new Date()) + ": " + message);
    }
}
