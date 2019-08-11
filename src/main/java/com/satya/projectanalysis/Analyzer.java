package com.satya.projectanalysis;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.Launcher;
import spoon.processing.ProcessingManager;
import spoon.reflect.declaration.CtType;
import spoon.reflect.factory.Factory;
import spoon.reflect.factory.TypeFactory;
import spoon.support.QueueProcessingManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.satya.projectanalysis.JavaUtils.wrapInRuntimeException;
import static org.apache.commons.text.StringEscapeUtils.escapeJava;

public class Analyzer {
     public static Logger LOG = LoggerFactory.getLogger(Analyzer.class);

    public static void main(String[] args) throws Exception {

        String sourceCodePath = "/home/satyendra/IdeaProjects/elasticsearch/server/src/main/java";
        String byteCodePath = "/home/satyendra/IdeaProjects/elasticsearch/server/build-idea/classes/java/main";
        String classpathForJars = "/home/satyendra/.gradle/caches/modules-2/files-2.1/";
        analyze(sourceCodePath, Arrays.asList(byteCodePath), "org.elasticsearch", classpathForJars);
    }

    /**
     *
     * @param sourceCodePath where to find .JAVA files
     * @param byteCodePath where to find .CLASS files
     * @param packageName package name to include classes
     * @param classpath directory containing JAR files
     * @throws Exception ex
     */
    private static void analyze(String sourceCodePath, List<String> byteCodePath, String packageName, String classpath) throws Exception {
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

        allJarFiles.forEach(jarFile -> wrapInRuntimeException(()-> classPool.appendClassPath(jarFile)));

        directories.forEach(directory -> wrapInRuntimeException(() -> classPool.appendClassPath(directory)));

        classPool.appendSystemPath();

        List<String> fullClassNames = getClassNamesFromDirectory(directories, packageName);

        JavassistClassProcessor javassistClassProcessor = new JavassistClassProcessor();

        fullClassNames.stream().filter(x -> !x.isEmpty()).forEach(wrapInRuntimeException(fullClassName -> {
            System.out.println("className:" + fullClassName);
            CtClass ctClass = classPool.get(fullClassName);
            javassistClassProcessor.process(ctClass);
        }, false));

        Global.INSTANCE.getRelationships().forEach((k, v) -> System.out.println(k));
    }

    private static List<String> getAllJarFiles(String classpath) throws IOException {
        try (Stream<Path> paths = Files.walk(Paths.get(classpath))) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(file -> file.toString().endsWith(".jar"))
                    .map(file -> file.toAbsolutePath().toString())
                    .collect(Collectors.toList());
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
                        .collect(Collectors.toList());
                return classFilePaths.stream().map(classPathToFullyQualifiedClassName).collect(Collectors.toList());
            }
        })).flatMap(strings -> Stream.of(strings.toArray(new String[0]))).collect(Collectors.toList());
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

    private static void performStaticAnalysis(String path) {
        Launcher launcher = new Launcher();

        List<String> allJavaFiles = getAllJavaFiles(path);
        allJavaFiles.forEach(launcher::addInputResource);

        launcher.getEnvironment().setAutoImports(true); // optional

        launcher.getEnvironment().setNoClasspath(true); // optional

        launcher.run();
        final Factory factory = launcher.getFactory();
        final ProcessingManager processingManager = new QueueProcessingManager(factory);
        final ClassProcessor processor = new ClassProcessor();
        processingManager.addProcessor(processor);
        processingManager.addProcessor(new MethodProcessor());
        processingManager.addProcessor(new InterfaceProcessor());

        processingManager.process(factory.Class().getAll());
        processingManager.process(factory.Interface().getAll());

        Global.INSTANCE.getRelationships().forEach((k, v) -> System.out.println(k));

        CtType s = new TypeFactory().get(String.class);
//        s.getFields().forEach(System.out::println);
    }

    private static List<String> getAllJavaFiles(String path) {
        try (Stream<Path> paths = Files.walk(Paths.get(path))) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(file -> file.toString().endsWith(".java"))
                    .map(file -> file.toAbsolutePath().toString())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }
}
