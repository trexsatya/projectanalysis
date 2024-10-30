package com.satya.projectanalysis;

import com.satya.projectanalysis.processors.writes.MethodDetail;
import com.satya.projectanalysis.processors.writes.MethodInvocationProcessor;
import lombok.Builder;
import lombok.Value;
import spoon.processing.AbstractProcessor;
import spoon.reflect.code.*;
import spoon.reflect.declaration.*;
import spoon.support.reflect.code.CtInvocationImpl;
import spoon.support.reflect.declaration.CtClassImpl;
import spoon.support.reflect.declaration.CtNamedElementImpl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;

public class MethodProcessor extends AbstractProcessor<CtMethod<?>> {
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

        public static LocalVariableDetails from(CtLocalVariable<?> ctLocalVariable) {
            String locaVariableType = ctLocalVariable.getType().getQualifiedName();
            if(locaVariableType.endsWith(".var")) locaVariableType = null;

            String moreSpecificType = null;
            CtExpression<?> assignment = ctLocalVariable.getAssignment();
            if(assignment instanceof CtInvocation) {
                CtInvocation<?> invocation = (CtInvocation<?>) assignment;
                moreSpecificType = ofNullable(invocation.getExecutable().getType())
                        .map(CtTypeInformation::getQualifiedName)
                        .orElse("");
                if(moreSpecificType.endsWith(".var")) {
//                        CtExpression<?> defaultExpression = ctLocalVariable.getDefaultExpression();
//                        if(defaultExpression instanceof CtInvocation) {
//                            moreSpecificType = ofNullable(RegexBasedAnalysis.tryToInferType((CtInvocation<?>)defaultExpression))
//                                    .map(MethodInvocationProcessor.ArgumentDetail::getType)
//                                    .orElse(moreSpecificType);
//                        }
                }
            }

            return LocalVariableDetails.builder()
                    .name(ctLocalVariable.getSimpleName())
                    .type(ofNullable(moreSpecificType).orElse(locaVariableType))
                    .build();
        }
    }

    public static String className(CtMethod<?> method) {
        String packageName = "";
        if(method.getParent() instanceof CtClass) {
             var clazz = (CtClass<?>) method.getParent();
             packageName = clazz.getParent().toString();
             return packageName + "." + clazz.getSimpleName();
        }
        if(method.getParent() instanceof CtInterface) {
            var clazz = (CtInterface<?>) method.getParent();
            packageName = clazz.getParent().toString();
            return packageName + "." + clazz.getSimpleName();
        }
        return null;
    }

    public static List<String> parameters(CtMethod<?> method) {
        return method.getParameters().stream().map(Object::toString).collect(Collectors.toList());
    }

    public static String signature(CtMethod<?> method) {
        List<String> types = new ArrayList<>();

        for (CtParameter<?> parameter : method.getParameters()) {
            types.add(parameter.getType().getQualifiedName());
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
        System.out.println(element.getParent(CtClassImpl.class).getSimpleName() + "."+ element.getSimpleName());
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
