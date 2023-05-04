package com.satya.projectanalysis;

import spoon.processing.AbstractProcessor;
import spoon.reflect.declaration.*;
import spoon.reflect.reference.CtFieldReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.support.reflect.declaration.CtConstructorImpl;
import spoon.support.util.ImmutableMap;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

public class ClassProcessor extends AbstractProcessor<CtClass> {

    @Override
    public void process(CtClass element) {
        //TODO: Handle inner classes
        if (isInnerClass(element)) return;

//        System.out.println(element.getQualifiedName());

        CtTypeReference<?> superclass = element.getSuperclass();
        Set<CtTypeReference<?>> interfaces = element.getSuperInterfaces();

        Function<CtClass, Node.Type> nodeTypeFn = cls -> cls.isInterface() || cls.isAbstract() ? Node.Type.INTERFACE : Node.Type.CLASS;


        Set<CtConstructor> constructors = element.getConstructors();
        List<ClassData.MethodData> constructorsList = constructors.stream().filter(CtModifiable::isPublic).map(ClassData.MethodData::of).collect(toList());

        Set<CtMethod<?>> methods = element.getMethods();
        List<ClassData.MethodData> methodData = methods.stream().filter(CtModifiable::isPublic).map(ClassData.MethodData::of).collect(toList());
        constructorsList.addAll(methodData);

        List<CtAnnotation<? extends Annotation>> annotations = element.getAnnotations();
        List<ClassData.AnnotationType> annotationTypes = annotations.stream().map(ClassData.AnnotationType::of).collect(toList());

        ClassData classData = ClassData.builder()
                .methodDataList(constructorsList)
                .annotations(new HashSet<>(annotationTypes))
                .className(element.getQualifiedName())
                .implementss(
                        interfaces.stream().map(x -> ClassData.ImplementsType.builder()
                                        .name(x.getQualifiedName())
                                        .typeArguments(x.getActualTypeArguments()
                                                .stream()
                                                .map(CtTypeInformation::getQualifiedName)
                                                .collect(toList()))
                                        .build())
                                .collect(toSet()))
                .build();

        BiFunction<String, Node.Type, Node> nodeFn = Global.INSTANCE::getOrCreateNode;

        Node thisClass = Global.INSTANCE.getOrCreateNode(element.getQualifiedName(), nodeTypeFn.apply(element), classData);

        Stream<Node> implementsThese = interfaces.stream()
                .map(ifc -> nodeFn.apply(ifc.getQualifiedName(), Node.Type.INTERFACE));

        if (superclass != null) {
            Global.INSTANCE.addIsALink(nodeFn.apply(superclass.getQualifiedName(), thisClass.type), thisClass);
        }

        implementsThese.forEach(ifc -> Global.INSTANCE.addIsLikeLink(thisClass, ifc));

        Collection<CtFieldReference<?>> allFields = element.getAllFields();

        allFields.stream().filter(fieldRef -> !fieldRef.getType().isPrimitive())
                .forEach(ctFieldReference -> {
                    Tuple<Node, String> nodeAndRefName = getTypeParamAwareNode(ctFieldReference, nodeFn);

                    //referenceName, referenceName:list, referenceName:mappedBy[String]
                    Global.INSTANCE.addThisRefersToThatLink(thisClass, nodeAndRefName._1, nodeAndRefName._2);
                });

        Global.INSTANCE.addClassData(thisClass.name, classData);
    }

    private Tuple<Node, String> getTypeParamAwareNode(CtFieldReference<?> ctFieldReference, BiFunction<String, Node.Type, Node> nodeFn) {
        Tuple<String, CtTypeReference> fn = fn(ctFieldReference, null);
        Node node = nodeFn.apply(fn._2.getQualifiedName(), nodeType(fn._2));
        return Tuple.of(node, fn._1);
    }

    private Tuple<String, CtTypeReference> fn(CtFieldReference<?> ctFieldReference, Tuple<String, CtFieldReference> temp) {
        List<CtTypeReference<?>> actualTypeArguments = ctFieldReference.getType().getActualTypeArguments();
        String referenceSimpleName = ctFieldReference.getSimpleName();

        //Handle list, map for now; TODO: Enhance

        switch (ctFieldReference.getType().getSimpleName()) {
            case "List":
            case "ImmutableList":
            case "Set":
            case "Queue":
                CtTypeReference<?> ctTypeReference = actualTypeArguments.get(0);
                return Tuple.of(referenceSimpleName + ": listOf", ctTypeReference);
            case "Map":
            case "ImmutableMap":
                return Tuple.of(referenceSimpleName + ": mappedBy[" + actualTypeArguments.get(0) + "]", actualTypeArguments.get(1));
            default:
                return Tuple.of(referenceSimpleName, ctFieldReference.getType());
        }
    }

    private String containmentType(CtFieldReference<?> ctFieldReference) {
        return null;
    }

    private boolean isInnerClass(CtClass element) {
        return element.getQualifiedName().contains("$");
    }

    private static Node.Type nodeType(CtFieldReference ctFieldReference) {
        return nodeType(ctFieldReference.getType());
    }

    private static Node.Type nodeType(CtTypeReference type) {
        return (type.isInterface()) ? Node.Type.INTERFACE : Node.Type.CLASS;
    }
}