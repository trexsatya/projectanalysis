package com.satya.projectanalysis;


import com.google.gson.internal.Streams;
import javassist.CtClass;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.LocalVariableAttribute;
import javassist.bytecode.MethodInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtMethod;
import spoon.support.reflect.declaration.CtMethodImpl;
import spoon.support.reflect.declaration.CtParameterImpl;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ClassData {
    static Logger LOG = LoggerFactory.getLogger(ClassData.class);

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

        public static MethodData of(javassist.CtConstructor constructor)  {
            try {
                Map<String,String> params = new HashMap<>();
                CtClass[] parameterTypes = constructor.getParameterTypes();

                MethodInfo methodInfo = constructor.getMethodInfo();
                CodeAttribute codeAttribute = methodInfo.getCodeAttribute();
                if(codeAttribute != null) {
                    LocalVariableAttribute attr = (LocalVariableAttribute) codeAttribute.getAttribute(LocalVariableAttribute.tag);

                    String[] parameterNames = new String[parameterTypes.length];
                    int pos = Modifier.isStatic(constructor.getModifiers()) ? 0 : 1;

                    for (int i = 0; i < parameterNames.length; i++) {
                        setParamName(attr, parameterNames, pos, i);
                    }

                    Arrays.stream(JavaUtils.zipArrays(parameterNames, parameterTypes))
                            .forEach(stringCtClassTuple -> {
                                params.put(stringCtClassTuple._1, stringCtClassTuple._2.getName());
                            });
                } else {
                    LOG.warn("Could not find CodeAttribute for {}", constructor);
                }

                MethodData methodData = new MethodData(constructor.getName(), params, null);
                Object[] annotations = constructor.getAnnotations();

                methodData.annotations = Arrays.stream(annotations).map(Object::toString).collect(Collectors.toList());
                return methodData;
            } catch (Exception ex){
                ex.printStackTrace();
                return null;
            }
        }

        public static MethodData of(javassist.CtMethod method) {
            if(method.getLongName().startsWith("java.lang.")) return null;

            LOG.info("Method: {}", method.getLongName());
            try {
                Map<String,String> params = new HashMap<>();
                CtClass[] parameterTypes = method.getParameterTypes();

                MethodInfo methodInfo = method.getMethodInfo();
                CodeAttribute codeAttribute = methodInfo.getCodeAttribute();
                if(codeAttribute != null) {
                    LocalVariableAttribute attr = (LocalVariableAttribute) codeAttribute.getAttribute(LocalVariableAttribute.tag);

                    String[] parameterNames = new String[parameterTypes.length];
                    int pos = Modifier.isStatic(method.getModifiers()) ? 0 : 1;

                    for (int i = 0; i < parameterNames.length; i++) {
                        setParamName(attr, parameterNames, pos, i);
                    }

                    Arrays.stream(JavaUtils.zipArrays(parameterNames, parameterTypes))
                            .forEach(stringCtClassTuple -> {
                                params.put(stringCtClassTuple._1, stringCtClassTuple._2.getName());
                            });
                } else {
                    LOG.warn("Could not find CodeAttribute for {}", method);
                }

                MethodData methodData = new MethodData(method.getName(), params, method.getReturnType().getName());
                Object[] annotations = method.getAnnotations();

                methodData.annotations = Arrays.stream(annotations).map(Object::toString).collect(Collectors.toList());
                return methodData;
            } catch (Exception ex){
                ex.printStackTrace();
                return null;
            }
        }

        private static void setParamName(LocalVariableAttribute attr, String[] parameterNames, int pos, int i) {
            if(attr.index(i) < parameterNames.length)
            parameterNames[attr.index(i)] = attr.variableName(i + pos);
        }
    }

    public List<MethodData> methodDataList;

    public ClassData(List<MethodData> methodDataList) {
        this.methodDataList = methodDataList;
    }

}
