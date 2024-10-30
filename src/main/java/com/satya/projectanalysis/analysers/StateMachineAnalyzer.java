package com.satya.projectanalysis.analysers;

import static com.satya.projectanalysis.JavaUtils.wrapInRuntimeException;
import static com.satya.projectanalysis.MethodProcessor.TO_BE_REMOVED;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.text.StringEscapeUtils.escapeJava;

import com.satya.projectanalysis.ClassProcessor;
import com.satya.projectanalysis.GenericProcessor;
import com.satya.projectanalysis.InterfaceProcessor;
import com.satya.projectanalysis.JavassistClassProcessor;
import com.satya.projectanalysis.processors.writes.MethodInvocationProcessor;
import com.satya.projectanalysis.MethodProcessor;
import com.satya.projectanalysis.processors.writes.VariableWritesProcessor;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.Launcher;
import spoon.processing.ProcessingManager;
import spoon.reflect.factory.Factory;
import spoon.support.QueueProcessingManager;

public class StateMachineAnalyzer {
    public static final String GRADLE_JARS_CLASSPATH = "/Users/satyendra.kumar/.gradle/caches/modules-2/files-2.1/";
    public static final String PACKAGE_NAME = "sample.code";
    public static Logger LOG = LoggerFactory.getLogger(StateMachineAnalyzer.class);

    public static void main(String[] args) throws Exception {
        File outFile = new File("/Users/satyendra.kumar/Documents/PersonalProjects/projectanalysis/build/out/event_network.json");
        if(!outFile.exists()) {
            boolean madeDir = new File(outFile.getParent()).mkdirs();
        }
        analyze(List.of("/Users/satyendra.kumar/Documents/PersonalProjects/projectanalysis/src/main/java/sample/code"),
                List.of(),
                outFile
        );
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
        String targetPath = "build/src-delomboked-2" + n;
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
     * @param sourceCodePath where to find .JAVA files
     * @param byteCodePath   where to find .CLASS files
     * @throws Exception ex
     */
    private static void analyze(List<String> sourceCodePath, List<String> byteCodePath, File outFile) throws Exception {
        try {
            performStaticAnalysis(sourceCodePath, outFile);
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
        performBytecodeAnalysis(byteCodePath);
    }

    private static void performBytecodeAnalysis(List<String> directories) throws NotFoundException, IOException {
        ClassPool classPool = new ClassPool();

        List<String> allJarFiles = getAllJarFiles(StateMachineAnalyzer.GRADLE_JARS_CLASSPATH);
        LOG.info("{} jar files.", allJarFiles.size());

        allJarFiles.forEach(jarFile -> wrapInRuntimeException(() -> classPool.appendClassPath(jarFile), false));

        directories.forEach(directory -> wrapInRuntimeException(() -> classPool.appendClassPath(directory)));

        classPool.appendSystemPath();

        List<String> fullClassNames = getClassNamesFromDirectory(directories, PACKAGE_NAME);

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
        Function<String, String> classPathToFullyQualifiedClassName = p -> extractClassName(p, packageName);

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

    private static String extractClassName(String filePath, String packageName) {
        String separator = escapeJava(File.separator);
        String packageNameAsPath = packageName.replaceAll("\\.", separator);
        packageNameAsPath = escapeJava(packageNameAsPath);
        Pattern pattern = Pattern.compile(".*" + separator + "(" + packageNameAsPath + ".*)$");
        Matcher matcher = pattern.matcher(filePath);
        if (matcher.matches()) {
            return matcher.group(1).replaceAll(separator, ".").replaceAll(".class", "");
        }
        LOG.warn("Could not extract fully qualified class name from: {} using packageName {}", filePath, packageName);
        return "";
    }

    private static void performStaticAnalysis(List<String> paths, File outFile) throws IOException {
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

        MethodInvocationProcessor methodInvocationProcessor = new MethodInvocationProcessor();

        processingManager.addProcessor(methodInvocationProcessor);
        processingManager.addProcessor(new VariableWritesProcessor());
        processingManager.addProcessor(new MethodInvocationProcessor());
        processingManager.process(factory.Class().getAll());
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
        return paths.stream().flatMap(StateMachineAnalyzer::getFiles).collect(Collectors.toSet());
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

    private static final SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss:SSS");

    private synchronized void log(String message) {
//        System.out.println(format.format(new Date()) + ": " + message);
    }
}
