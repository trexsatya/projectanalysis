package com.satya.projectanalysis;

import javassist.*;
import spoon.reflect.declaration.CtModifiable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.satya.projectanalysis.JavaUtils.wrapInRuntimeException;
import static java.util.stream.Collectors.toList;

public class JavassistClassProcessor {
    public void process(CtClass element) throws Exception {
        if(element.getPackageName().contains("java.lang.")) return;
        if(isInnerClass(element)) return;

        CtClass superclass = element.getSuperclass();
        CtClass[] interfaces = element.getInterfaces();

        Function<CtClass, Node.Type> nodeTypeFn = cls -> cls.isInterface() ? Node.Type.INTERFACE : Node.Type.CLASS;
        BiFunction<String, Node.Type, Node> nodeFn = Global.INSTANCE::getOrCreateNode;

        Node thisClass = nodeFn.apply(element.getName(), nodeTypeFn.apply(element));
        Stream<Node> implementsThese = Arrays.stream(interfaces).map(ifc -> nodeFn.apply(ifc.getName(), Node.Type.INTERFACE));

        if (superclass != null) {
            Global.INSTANCE.addIsALink(nodeFn.apply(superclass.getName(), thisClass.type), thisClass);
        }

        implementsThese.forEach(ifc -> Global.INSTANCE.addIsLikeLink(thisClass, ifc));

        CtField[] allFields = element.getDeclaredFields();

        Arrays.stream(allFields).filter(fieldRef -> {
                    CtClass ctClass = wrapInRuntimeException(fieldRef::getType, false);
                    return ctClass != null && !ctClass.isPrimitive();
                })
                .forEach(wrapInRuntimeException(ctFieldReference -> {
                    Tuple<Node, String> nodeAndRefName = getTypeParamAwareNode(ctFieldReference, nodeFn);

                    //referenceName, referenceName:list, referenceName:mappedBy[String]
                    Global.INSTANCE.addThisRefersToThatLink(thisClass, nodeAndRefName._1, nodeAndRefName._2);
                }, false));

        CtConstructor[] constructors = element.getConstructors();
        List<ClassData.MethodData> constructorsList = Arrays.stream(constructors).map(ClassData.MethodData::of).collect(toList());

        CtMethod[] methods = element.getMethods();
        List<ClassData.MethodData> methodData = Arrays.stream(methods).map(ClassData.MethodData::of).filter(Objects::nonNull).collect(toList());
        constructorsList.addAll(methodData);

        Global.INSTANCE.addClassData(thisClass.name,  ClassData.builder()
                        .className(element.getName())
                .methodDataList(constructorsList).build());
    }

    private boolean isInnerClass(CtClass element) {
        return element.getName().contains("$");
    }

    private Tuple<Node, String> getTypeParamAwareNode(CtField ctField, BiFunction<String, Node.Type, Node> nodeFn) throws NotFoundException {
        Tuple<String, CtClass> fn = fn(ctField, null);
        Node node = nodeFn.apply(fn._2.getName(), nodeType(fn._2));
        return Tuple.of(node, fn._1);
    }

    private Node.Type nodeType(CtClass ctClass) {
        return ctClass.isInterface() ? Node.Type.INTERFACE : Node.Type.CLASS;
    }

    private Tuple<String, CtClass> fn(CtField ctField, Tuple<String, CtField> temp) throws NotFoundException {
        String name = ctField.getName();
        return Tuple.of(name, ctField.getType());
    }
}
