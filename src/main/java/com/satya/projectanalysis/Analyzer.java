package com.satya.projectanalysis;

import com.google.common.base.Predicate;
import com.google.common.reflect.ClassPath;
import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtPackage;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Properties;
import java.util.stream.Stream;

public class Analyzer {
    public static void main(String[] args) {
        try {
            analyze();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void analyze() throws IOException {
        Launcher launcher = new Launcher();

// path can be a folder or a file
// addInputResource can be called several times
        launcher.addInputResource("d:\\Projects\\weblog-backend\\src\\main\\");

// if true, the pretty-printed code is readable without fully-qualified names
        launcher.getEnvironment().setAutoImports(true); // optional

// if true, the model can be built even if the dependencies of the analyzed source code are not known or incomplete
// the classes that are in the current classpath are taken into account
        launcher.getEnvironment().setNoClasspath(true); // optional

        launcher.buildModel();
        CtModel model = launcher.getModel();

        // list all packages of the model
        for(CtPackage p : model.getAllPackages()) {
            System.out.println("package: "+p.getQualifiedName());
        }

        Stream<ClassPath.ClassInfo> clazzes = findClasses();
        analyzeSpringAnnotations(clazzes);
    }

    private static void analyzeSpringAnnotations(Stream<ClassPath.ClassInfo> clazzes) {
        clazzes.forEach(clazz-> {
            System.out.println(clazz);
            try {
                Annotation[] annotations = clazz.load().getAnnotations();
                System.out.println(Arrays.toString(annotations));
            } catch (Error e){ }
        });
    }

    static Predicate<ClassPath.ClassInfo> notToIgnore = clz -> packagesToIgnore().noneMatch(s -> clz.getPackageName().startsWith(s));

    static Stream<String> packagesToIgnore() {
        return configFile().entrySet().stream().filter(objectObjectEntry -> ((String)objectObjectEntry.getKey()).startsWith("ignore"))
                .flatMap(objectObjectEntry -> Arrays.asList(((String) objectObjectEntry.getValue()).split(",")).stream());
    }

    static Properties configFile() {
        Properties properties = new Properties();
        try {
            InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("projectanalysis.properties");
            properties.load(stream);
        } catch (Exception ex){}
        return properties;
    }


    static Stream<ClassPath.ClassInfo> findClasses() throws IOException {
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        return ClassPath.from(loader).getTopLevelClasses().stream().filter(notToIgnore);
    }
}
