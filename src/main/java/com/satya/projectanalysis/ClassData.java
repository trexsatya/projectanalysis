package com.satya.projectanalysis;


import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtMethod;
import spoon.support.reflect.declaration.CtMethodImpl;
import spoon.support.reflect.declaration.CtParameterImpl;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ClassData {
    public static class MethodData {
        String name;
        Map<String,String> paramList;
        String returnType = "";
        String fullName;
        List<String> annotations;

        public MethodData(String fullName) {
            this.fullName = fullName;
        }

        public MethodData(String name, Map<String,String> paramList, String returnType) {
            this.name = name;
            this.paramList = paramList;
            this.returnType = returnType;
        }

        public static MethodData of(CtMethod<?> method) {
            String returnType = method.getType().getQualifiedName();
            Map<String,String> params = new HashMap<>();
            method.getParameters().forEach(p -> {
                if(p instanceof CtParameterImpl) {
                    CtParameterImpl parameter = (CtParameterImpl) p;
                    params.put(parameter.getSimpleName(), parameter.getType().getQualifiedName());
                }
            });
            MethodData methodData = new MethodData(method.getSimpleName(), params, returnType);
            List<CtAnnotation<? extends Annotation>> annotations = method.getAnnotations();

            List<String> annotationsList = annotations.stream().map(x -> x.getType().getQualifiedName()).collect(Collectors.toList());
            methodData.annotations = annotationsList;
            return methodData;
        }

        public static MethodData of(CtConstructor<?> method) {
            Map<String,String> params = new HashMap<>();
            method.getParameters().forEach(p -> {
                if(p instanceof CtParameterImpl) {
                    CtParameterImpl parameter = (CtParameterImpl) p;
                    params.put(parameter.getSimpleName(), parameter.getType().getQualifiedName());
                }
            });
            MethodData methodData = new MethodData(method.getSimpleName(), params, null);

            List<CtAnnotation<? extends Annotation>> annotations = method.getAnnotations();
            List<String> annotationsList = annotations.stream().map(x -> x.getType().getQualifiedName()).collect(Collectors.toList());
            methodData.annotations = annotationsList;

            return methodData;
        }
    }

    public List<MethodData> methodDataList = new ArrayList<>();

    public ClassData(List<MethodData> methodDataList) {
        this.methodDataList = methodDataList;
    }

}
