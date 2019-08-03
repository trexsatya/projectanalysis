package com.satya.projectanalysis;

import spoon.processing.AbstractProcessor;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.reference.CtFieldReference;
import spoon.reflect.reference.CtTypeReference;

import java.util.Collection;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

public class ClassProcessor extends AbstractProcessor<CtClass> {

    @Override
    public void process(CtClass element) {
        if(isInnerClass(element)) return;

        if(element.getQualifiedName().contains("InternalIndexService")) {
            System.out.println();
        }
//        System.out.println(element.getQualifiedName());

        CtTypeReference<?> superclass = element.getSuperclass();
        Set<CtTypeReference<?>> interfaces = element.getSuperInterfaces();

        Function<CtClass, Node.Type> nodeTypeFn = cls -> cls.isInterface() || cls.isAbstract() ? Node.Type.INTERFACE : Node.Type.CLASS;

        BiFunction<String, Node.Type, Node> nodeFn = Global.INSTANCE::getOrCreateNode;

        Node thisClass = nodeFn.apply(element.getQualifiedName(), nodeTypeFn.apply(element));

        Stream<Node> implementsThese = interfaces.stream().map(ifc -> {
            return nodeFn.apply(ifc.getQualifiedName(), Node.Type.INTERFACE);
        });

        if (superclass != null) {
            Global.INSTANCE.addIsALink(nodeFn.apply(superclass.getQualifiedName(), thisClass.type), thisClass);
        }

        implementsThese.forEach(ifc -> Global.INSTANCE.addIsLikeLink(thisClass, ifc));

        Collection<CtFieldReference<?>> allFields = element.getAllFields();

        allFields.stream().filter(fieldRef -> !fieldRef.getType().isPrimitive())
                .forEach(ctFieldReference -> {
                    Node node = nodeFn.apply(ctFieldReference.getType().getQualifiedName(), nodeType(ctFieldReference));
                    Global.INSTANCE.addThisRefersToThatLink(thisClass, node, ctFieldReference.getSimpleName());
                });
    }

    private boolean isInnerClass(CtClass element) {
        return element.getQualifiedName().contains("$");
    }

    private static Node.Type nodeType(CtFieldReference ctFieldReference) {
        CtTypeReference type = ctFieldReference.getType();
        return (type.isInterface()) ? Node.Type.INTERFACE : Node.Type.CLASS;
    }
}