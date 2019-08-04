package com.satya.projectanalysis;

import spoon.processing.AbstractProcessor;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtModifiable;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


public class InterfaceProcessor extends AbstractProcessor<CtInterface> {

    @Override
    public void process(CtInterface element) {
        if(element.getQualifiedName().endsWith(".IndexShard")) {
            System.out.println();
        }
        Set<CtMethod<?>> methods = element.getMethods();
        List<ClassData.MethodData> methodData = methods.stream().filter(CtModifiable::isPublic).map(ClassData.MethodData::of).collect(Collectors.toList());
        Global.INSTANCE.addClassData(element.getQualifiedName(), new ClassData(methodData));
    }
}
