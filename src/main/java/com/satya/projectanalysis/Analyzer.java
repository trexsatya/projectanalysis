package com.satya.projectanalysis;

import spoon.Launcher;
import spoon.processing.ProcessingManager;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtType;
import spoon.reflect.factory.Factory;
import spoon.reflect.factory.TypeFactory;
import spoon.support.QueueProcessingManager;

import java.io.IOException;

public class Analyzer {
    public static void main(String[] args) {
        try {
            analyze("D:\\Projects\\takshLibrary\\projectanalysis\\src\\main\\");
        } catch (Exception e) {}
    }

    public static void analyze(String path) throws IOException {
        Launcher launcher = new Launcher();

// path can be a folder or a file
// addInputResource can be called several times
        launcher.addInputResource(path);

// if true, the pretty-printed code is readable without fully-qualified names
        launcher.getEnvironment().setAutoImports(true); // optional

// if true, the model can be built even if the dependencies of the analyzed source code are not known or incomplete
// the classes that are in the current classpath are taken into account
        launcher.getEnvironment().setNoClasspath(true); // optional

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
