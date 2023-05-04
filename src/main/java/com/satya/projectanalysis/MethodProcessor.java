package com.satya.projectanalysis;

import lombok.Builder;
import lombok.Value;
import spoon.processing.AbstractProcessor;
import spoon.reflect.code.*;
import spoon.reflect.declaration.*;
import spoon.reflect.reference.CtTypeReference;
import spoon.support.reflect.code.CtInvocationImpl;
import spoon.support.reflect.code.CtReturnImpl;
import spoon.support.reflect.declaration.CtClassImpl;
import spoon.support.reflect.declaration.CtNamedElementImpl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

public class MethodProcessor extends AbstractProcessor<CtMethod> {
    public static Map<String, List<String>> linesForClasses = new HashMap<>();

    public static final String TO_BE_REMOVED = "@TOBEREMOVED@";

    @Builder
    @Value
    public static class LocalVariableDetails {
        String name;
        String type;
        Boolean isUnderCondition;
        //Initialisation, assignments including conditions/loops etc
        String variableWriteSummary;

        public static LocalVariableDetails from(CtLocalVariable ctLocalVariable) {
            String locaVariableType = ctLocalVariable.getType().getQualifiedName();
            if(locaVariableType.endsWith(".var")) locaVariableType = null;

            String moreSpecificType = null;
            CtExpression assignment = ctLocalVariable.getAssignment();
            if(assignment instanceof CtInvocation) {
                CtInvocation invocation = (CtInvocation) assignment;
                moreSpecificType = ofNullable(invocation.getExecutable().getType())
                        .map(CtTypeInformation::getQualifiedName)
                        .orElse("");
                if(moreSpecificType.endsWith(".var")) {
                        CtExpression defaultExpression = ctLocalVariable.getDefaultExpression();
                        if(defaultExpression instanceof CtInvocation) {
                            moreSpecificType = ofNullable(RegexBasedAnalysis.tryToInferType((CtInvocation)defaultExpression))
                                    .map(MethodInvocationProcessor.ArgumentDetail::getType)
                                    .orElse(moreSpecificType);
                        }

                }
            }

            return LocalVariableDetails.builder()
                    .name(ctLocalVariable.getSimpleName())
                    .type(ofNullable(moreSpecificType).orElse(locaVariableType))
                    .build();
        }
    }

    @Builder
    @Value
    public static class MethodDetail {
        String fullyQualifiedClassName;
        String methodName;
//        Map<String, String> parameters;
        @Builder.Default
        List<String> parameters = new ArrayList<>();

        String methodSignature;

        String implementation;
        //Name, detail
        @Builder.Default
        Map<String, LocalVariableDetails> variableDetails = new HashMap<>();
        String returnType;
        String returnExpressionSummary;
        String summary;

        private static String returnType(CtMethod method) {
            CtTypeReference type = method.getType();
            if(type != null) {
                return type.getQualifiedName();
            }
            return null;
        }

        private static Optional<String> returnType(CtBlock body) {
            if(body == null || body.getStatements() == null) {
                return empty();
            }
            return body.getStatements().stream()
                    .filter(it -> it instanceof CtReturnImpl)
                    .findFirst()
                    .map(it -> (CtReturnImpl)it)
                    .map(CtReturnImpl::getReturnedExpression)
                    .map(MethodDetail::returnedExpressionType);
        }

        private static String  returnedExpressionType(CtExpression it) {
            if(it instanceof CtInvocation) {
                CtInvocation invocation = (CtInvocation) it;
                /*
                 * Enhance this
                 * It is null in case of, for example, ((T) (memoizedFactory.apply(avroEvent.getClass()).toDomain(avroEvent)))
                 */
                return ofNullable(invocation.getExecutable()
                        .getType())
                        .map(CtTypeInformation::getQualifiedName)
                        .orElse(null);
            }
            if(it instanceof CtVariableRead) {
                CtVariableRead variableRead = (CtVariableRead) it;
                return variableRead.getVariable().getType().getQualifiedName();
            }
            return null;
        }

        public static MethodDetail from(CtMethod method) {
            return MethodDetail.builder()
                    .methodName(method.getSimpleName())
                    .parameters(parameters(method))
                    .fullyQualifiedClassName(className(method))
                    .returnType(returnType(method.getBody()).orElse(returnType(method)))
                    .methodSignature(signature(method))
                    .variableDetails(variables(method))
                    .implementation(method.toString())
                    .build();
        }

        private static Map<String, LocalVariableDetails> variables(CtMethod method) {
            Map<String, LocalVariableDetails> variableDetailsMap = new HashMap<>();
            method.getBody().getStatements().forEach(var -> {
                        LocalVariableDetails details = null;
                        if(var instanceof CtLocalVariable) {
                            details = LocalVariableDetails.from((CtLocalVariable) var);
                        }

                        if(details != null) {
                            variableDetailsMap.put(details.name, details);
                        }
                    });
            return variableDetailsMap;
        }
    }

    static String className(CtMethod method) {
        String packageName = "";
        if(method.getParent() instanceof CtClass) {
            CtClass clazz = (CtClass) method.getParent();
             packageName = clazz.getParent().toString();
             return packageName + "." + clazz.getSimpleName();
        }
        if(method.getParent() instanceof CtInterface) {
            CtInterface clazz = (CtInterface) method.getParent();
            packageName = clazz.getParent().toString();
            return packageName + "." + clazz.getSimpleName();
        }
        return null;
    }

    static List<String> parameters(CtMethod method) {
        return (List<String>) method.getParameters().stream().map(Object::toString).collect(Collectors.toList());
    }

    static String signature(CtMethod method) {
        List types = new ArrayList();

        for (Object parameter : method.getParameters()) {
            CtParameter param = (CtParameter) parameter;
            types.add(param.getType().getQualifiedName());
        }

        return method.getSimpleName() + "("+ String.join(", ", types) + ")";
    }

    @Override
    public void process(CtMethod element) {
//        System.out.println("sa");
//        if(!isInterestingClass(element)) {
//            return;
//        }
        if(element.getParent(CtClassImpl.class) == null) {
            return;
        }
        System.out.println(element.getParent(CtClassImpl.class).getSimpleName() + element.getSimpleName());
        if(element.getBody() != null && isTest(element) && needsMigration(element)) { //So that we get specific types
            Global.ClassPool.addMethod(MethodDetail.from(element));
            element.getBody().getStatements()
                    .stream().filter(it -> it.toString().startsWith("verify("))
                    .forEach(this::processVerifyStatement);
            element.getBody().getStatements()
                    .stream().filter(it -> it.toString().startsWith("when("))
                    .forEach(this::processWhenStatement);
        }
    }

    private void processWhenStatement(CtStatement statement) {
        String modified = statement.toString()
                .replaceAll("when\\(", "given(")
                .replaceAll("\\.thenAnswer\\(", ".willAnswer(")
                .replaceAll("\\.thenReturn\\(", ".willReturn(");
        modifySourceCodeLines(statement, modified);
    }

    private static boolean isInterestingClass(CtMethod element) {
        return ofNullable(element.getParent(CtClassImpl.class))
                .map(CtNamedElementImpl::getSimpleName)
                .orElse("")
                .contains("BulkClaimSeedHandlerTest");
    }

    private void processVerifyStatement(CtStatement statement) {
        var verifyStatement = asInvocation(statement);
        List arguments = asInvocation(verifyStatement.getTarget()).getArguments();

        var obj = arguments.get(0);
        var secondArg = "";
        String times = "";
        if(arguments.size() > 1) {
            secondArg = arguments.get(1).toString();
        }
        if(secondArg.contains("times")) {
            times = secondArg;
        }
        var modified = "then(" + obj + ").should(" + times + ")." + verifyStatement.getExecutable().getSimpleName() + "("
                + verifyStatement.getArguments().stream().map(Object::toString).collect(Collectors.joining(",")) + ")";

        modifySourceCodeLines(statement, modified);
    }

    private void modifySourceCodeLines(CtStatement statement, String modified) {
        String path = statement.getParent(CtClassImpl.class).getPosition().getFile().getAbsolutePath();
        var lines = linesForClasses.computeIfAbsent(path, MethodProcessor::getLinesFromClassFile);

        int start = statement.getPosition().getLine() - 1;
        int endLine = statement.getPosition().getEndLine() -1;

        lines.set(start, modified + ";");
        if(endLine > start) {
            IntStream.range(start + 1, endLine + 1)
                    .forEach(ln -> lines.set(ln, TO_BE_REMOVED));
        }
    }

    private static List<String> getLinesFromClassFile(String path)  {
        try (Stream<String> lines= Files.lines(Path.of(path))){
            return lines.collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    CtInvocationImpl asInvocation(Object it) {
        return (CtInvocationImpl) it;
    }

    private boolean needsMigration(CtMethod element) {
        return element.getBody().getStatements().stream().map(Object::toString).anyMatch(it -> it.contains("verify(")
        || it.contains("when("));
    }

    private static boolean isTest(CtMethod element) {
        return element.getAnnotations().stream().map(CtAnnotation::getName).collect(Collectors.toList()).contains("Test");
    }
}
