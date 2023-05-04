package com.satya.projectanalysis;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtVariableRead;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtTypeInformation;
import spoon.reflect.reference.CtTypeReference;

import java.util.*;
import java.util.function.Supplier;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

/**
 * Get list of targets we are interested in for method call: - entities, eventProducer
 */
@SuppressWarnings("uncheked")
public class MethodInvocationProcessor extends AbstractProcessor<CtInvocation> {
    static Logger LOG = LoggerFactory.getLogger(MethodInvocationProcessor.class);

    //TODO: Add all the subclasses dynamically
    //Map of interesting class & interesting arguments used in invocation/method-calls
    Map<String, List<String>> interestingTargetClasses =
            new HashMap<>();

    public MethodInvocationProcessor addInterestingTarget(String targetType, List<String> parameterTypes) {
        interestingTargetClasses.put(targetType, parameterTypes);
        return this;
    }

    @Value
    @EqualsAndHashCode
    static class InvocationDetailSupplier implements Supplier<InvocationDetail> {
        InvocationDetail invocationDetail;

        @Override
        public InvocationDetail get() {
            return invocationDetail;
        }
    }
    //Output would be the classes which should be processed dynamically by javassist
    private final List<Supplier<InvocationDetail>> output = new ArrayList<>();

    public List<Supplier<InvocationDetail>> getOutput() {
        return output;
    }

    @Builder
    @Value
    public static class ArgumentDetail {
        //Method's return expression summary if the argument is a method invocation
        //Variables summary if it is a variable read
        String summary;
        String type;
    }

    @Builder
    @Value
    public static class InvocationDetail {
        String className;
        String summary;
        @Builder.Default
        List<ArgumentDetail> argumentDetails = new ArrayList<>();
        String line;
    }

    @Override
    public void process(CtInvocation element) {
        if (isInterestingTarget(element)) {
            //Argument can be method invocation, or fieldReference
            output.add(() ->
            {
                MethodProcessor.MethodDetail method = getMethod(element);
                int line = element.getPosition().getLine();
                int endLine = element.getPosition().getEndLine();
                return InvocationDetail.builder()
                        .line(line + ": " + element)
                        .summary(method.getImplementation()) //TODO: Fix this to have summary
                        .className(method.getFullyQualifiedClassName())
                        .argumentDetails((List<ArgumentDetail>) element.getArguments().stream()
                                .map(this::resolveArgument)
                                .filter(Objects::nonNull)
                                .collect(toList()))
                        .build();
            });
        }
    }

    public MethodProcessor.MethodDetail getMethod(CtElement element) {
        CtElement parent = element.getParent();
        while (parent != null && !(parent instanceof CtMethod)) {
            parent = parent.getParent();
        }
        if (parent != null) {
            return MethodProcessor.MethodDetail.from((CtMethod) parent);
        }
        return null;
    }

    private Object resolveArgument(Object it) {
        if (it instanceof CtInvocation) {
            CtInvocation invocation = (CtInvocation) it;
            return ofNullable(invocation.getExecutable().getType())
                    .map(CtTypeInformation::getQualifiedName)
                    .map(x -> ArgumentDetail.builder()
                            .type(x)
                            .summary(invocation.toString())
                            .build())
                    .orElse(RegexBasedAnalysis.tryToInferType(invocation));
        }
        if (it instanceof CtVariableRead) {
            CtVariableRead variableRead = (CtVariableRead) it;
            MethodProcessor.LocalVariableDetails details = getMethod((CtElement) it).getVariableDetails().get(variableRead.getVariable().getSimpleName());
            return ofNullable(details).map(x -> ArgumentDetail.builder()
                            .type(x.getType())
                            .summary(x.getVariableWriteSummary())
                            .build())
                    .orElse(null);
        }
        return null;
    }

    boolean isInterestingTarget(CtInvocation invocation) {
        CtExpression target = invocation.getTarget();
        if (target == null) return false;
        CtTypeReference referenceType = target.getType();
        if (target instanceof CtFieldRead) {
            referenceType = ((CtFieldRead<?>) target).getVariable().getType();
        }
        return referenceType != null &&
                isInteresting(referenceType);

    }
    private boolean isInteresting(CtTypeReference referenceType) {
        return interestingTargetClasses.entrySet().stream().anyMatch(e -> referenceType.getQualifiedName().contains(e.getKey()));
    }
}