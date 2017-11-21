package com.satya.projectanalysis;


import java.io.IOException;
import java.util.function.Consumer;
import spoon.Launcher;
import spoon.processing.ProcessingManager;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtType;
import spoon.reflect.factory.Factory;
import spoon.reflect.factory.TypeFactory;
import spoon.support.QueueProcessingManager;


public class Analyzer {
    public static void main(String[] args) {
        try {
            Analyzer.analyze("D:\\Projects\\takshLibrary\\projectanalysis\\src\\main\\");
        } catch (Exception e) {
        }
    }

    public static void analyze(String path) throws IOException {
        Launcher launcher = new Launcher();
        launcher.addInputResource(path);
        launcher.getEnvironment().setAutoImports(true);
        launcher.getEnvironment().setNoClasspath(true);
        launcher.run();
        final Factory factory = launcher.getFactory();
        final ProcessingManager processingManager = new QueueProcessingManager(factory);
        final CatchProcessor processor = new CatchProcessor();
        processingManager.addProcessor(processor);
        processingManager.process(factory.Class().getAll());
        CtType s = new TypeFactory().get(String.class);
        s.getFields().forEach(System.out::println);
    }
}

