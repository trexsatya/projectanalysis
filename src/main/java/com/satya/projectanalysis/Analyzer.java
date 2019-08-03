package com.satya.projectanalysis;

import spoon.Launcher;
import spoon.processing.ProcessingManager;
import spoon.reflect.declaration.CtType;
import spoon.reflect.factory.Factory;
import spoon.reflect.factory.TypeFactory;
import spoon.support.QueueProcessingManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Analyzer {
    public static void main(String[] args) {
        try {
            analyze("/home/satyendra/IdeaProjects/elasticsearch/modules/elasticsearch/");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void analyze(String path) throws IOException, InterruptedException {
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
        processingManager.process(factory.Class().getAll());

        Global.INSTANCE.getRelationships().forEach((k,v) -> System.out.println(k+" "+v));

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
