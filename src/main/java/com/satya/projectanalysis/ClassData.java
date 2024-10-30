package com.satya.projectanalysis;


import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javassist.CtClass;
import javassist.Modifier;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.LocalVariableAttribute;
import javassist.bytecode.MethodInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtMethod;
import spoon.support.reflect.declaration.CtParameterImpl;

@Builder
@Data
public class ClassData {
    @Data @Builder
    public static class ImplementsType {
        String name;
        List<String> typeArguments;
    }

    static Logger LOG = LoggerFactory.getLogger(ClassData.class);

    @Builder.Default
    Set<ImplementsType> implementss = new HashSet<>();

    @Builder.Default
    Set<ImplementsType> extendss = new HashSet<>();

    @Builder.Default
    Set<AnnotationType> annotations = new HashSet<>();

    @Builder.Default
    Set<String> imports = new HashSet<>();

    @Data
    @AllArgsConstructor
    public static class AnnotationType {
        String name;

        //Name, value
        Map<String, List<String>> parameters;

        public static AnnotationType of(CtAnnotation<? extends Annotation> ctAnnotation) {

            String qualifiedName = ctAnnotation.getAnnotationType().getQualifiedName();
            Map<String, List<String>> params = new HashMap<>();
            ctAnnotation.getValues().forEach((k, v) -> {

            });
            return new AnnotationType(qualifiedName, Map.of(), "");
        }

        String summary;
    }


    String className;

    public String className() {
        return className;
    }

    @Data
    public static class MethodData {
        String name;
        Map<String,String> paramList;
        String returnType = "";
        String fullName;
        List<String> annotations;

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
//                    LOG.warn("Could not find CodeAttribute for {}", constructor);
                }

                MethodData methodData = new MethodData(constructor.getName(), params, null);
                Object[] annotations = constructor.getAnnotations();

                methodData.annotations = Arrays.stream(annotations).map(Object::toString).collect(Collectors.toList());
                return methodData;
            } catch (Exception | LinkageError ex){
//                ex.printStackTrace();
                LOG.info("Exception {}", ex.toString());
                return null;
            }
        }

        public static MethodData of(javassist.CtMethod method) {
            if(method.getLongName().startsWith("java.lang.")) return null;

//            LOG.info("Method: {}", method.getLongName());
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
//                    LOG.warn("Could not find CodeAttribute for {}", method);
                }

                MethodData methodData = new MethodData(method.getName(), params, method.getReturnType().getName());
                Object[] annotations = method.getAnnotations();

                methodData.annotations = Arrays.stream(annotations).map(Object::toString).collect(Collectors.toList());
                return methodData;
            } catch (Exception | LinkageError ex){
//                ex.printStackTrace();
                LOG.info("Error {} for method {}", ex.toString(), method.getLongName());
                return null;
            }
        }

        private static void setParamName(LocalVariableAttribute attr, String[] parameterNames, int pos, int i) {
            if(attr.index(i) < parameterNames.length) {
                try {
                    parameterNames[attr.index(i)] = attr.variableName(i + pos);
                } catch (Exception ex) {
                    //TODO: Fix this
                    //Ignore for now
                }
            }
        }
    }

    List<MethodData> methodDataList;
}
