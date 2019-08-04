package com.satya.projectanalysis;

import spoon.processing.AbstractProcessor;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtMethod;

public class MethodProcessor extends AbstractProcessor<CtMethod> {

    @Override
    public void process(CtMethod element) {
        if(element.getDeclaringType().getQualifiedName().contains("InternalServer")){
            System.out.println();
        }
    }
}
