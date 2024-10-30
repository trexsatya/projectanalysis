package com.satya.projectanalysis.processors.writes;

import com.satya.projectanalysis.MethodProcessor;
import java.util.function.Predicate;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtVariableRead;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtTypeInformation;
import spoon.reflect.path.CtRole;
import spoon.reflect.reference.CtTypeReference;

import java.util.*;
import java.util.function.Supplier;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

/**
 * Get list of targets we are interested in for method call: - entities, eventProducer
 */
@SuppressWarnings("uncheked")
public class MethodInvocationProcessor extends AbstractProcessor<CtInvocation<?>> {
    static Logger LOG = LoggerFactory.getLogger(MethodInvocationProcessor.class);

    //TODO: Add all the subclasses dynamically
    //Map of interesting class & interesting arguments used in invocation/method-calls
    private Predicate<CtTypeReference<?>> filter;

    public MethodInvocationProcessor addFilter(Predicate<CtTypeReference<?>> typeMatcher) {
        filter = typeMatcher;
        return this;
    }

    @Value
    @EqualsAndHashCode
    static class InvocationDetailSupplier implements Supplier<MethodInvocationDetail> {
        MethodInvocationDetail methodInvocationDetail;

        @Override
        public MethodInvocationDetail get() {
            return methodInvocationDetail;
        }
    }

    //Output would be the classes which should be processed dynamically by javassist
    @Getter private final Set<MethodInvocationDetail> output = new HashSet<>();

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
    public static class MethodInvocationDetail {
//        MethodDetail methodDetail;
//        MethodDetail invokedInsideMethod;
        String invokedMethod;
        String invokedInsideMethod;
        String className;
        String summary;
        @Builder.Default
        List<ArgumentDetail> argumentDetails = new ArrayList<>();
        String line;
    }

    public String getInvokedMethod(CtInvocation<?> element) {
        try {
            String typeName = element.getExecutable().getDeclaringType().getQualifiedName();
            String signature = element.getExecutable().getSignature();
            return typeName + "#" + signature;
        } catch (Exception e) {
            LOG.error("Error finding method name", e);
            return null;
        }
    }

    @Override
    public void process(CtInvocation element) {
        if (isInterestingTarget(element)) {
            MethodDetail methodDetail = getMethod(element);
            int line = element.getPosition().getLine();
            int endLine = element.getPosition().getEndLine();
            String invokerMethod = getInvokerMethod(element);
            var detail = MethodInvocationDetail.builder()
                                                                 .invokedMethod(getInvokedMethod(element))
                                                                 .argumentDetails(getArgumentDetails(element))
                                                                 .invokedInsideMethod(invokerMethod)
                                                                 .build();
            //Argument can be method invocation, or fieldReference
            LOG.info("MID: {}", detail);
            output.add(detail);
        }
    }

    private String getInvokerMethod(CtInvocation<?> element) {
        CtElement invokerMethod = element.getParent(it -> it.getRoleInParent().getSubRoles().contains(CtRole.METHOD));
        return ofNullable(invokerMethod).filter(it -> it instanceof CtMethod).map(it -> {
            var method = (CtMethod<?>) it;
            String typeName = method.getDeclaringType().getQualifiedName();
            String signature = method.getSignature();
            return typeName + "#" + signature;
        }).orElse(null);
    }

    private List<ArgumentDetail> getArgumentDetails(CtInvocation<?> element) {
        return element.getArguments().stream()
                      .map(this::resolveArgument)
                      .filter(Objects::nonNull)
                      .collect(toList());
    }

    public MethodDetail getMethod(CtElement element) {
        CtElement parent = element.getParent();
        while (parent != null && !(parent instanceof CtMethod)) {
            parent = parent.getParent();
        }
        if (parent != null) {
            return MethodDetail.from((CtMethod<?>) parent);
        }
        return null;
    }

    private ArgumentDetail resolveArgument(Object it) {
        if(it instanceof CtLiteral<?> literal) {
            return ArgumentDetail.builder()
                                 .type(literal.getType().getQualifiedName())
                                 .summary(literal.toString())
                                 .build();
        }

        if (it instanceof CtInvocation<?> invocation) {
            return ofNullable(invocation.getExecutable().getType())
                    .map(CtTypeInformation::getQualifiedName)
                    .map(x -> ArgumentDetail.builder()
                                            .type(x)
                                            .summary(invocation.toString())
                                            .build())
                    .orElse(null);
        }

        if(it instanceof CtFieldRead<?> fieldRead) {
            return ofNullable(fieldRead.getType())
                    .map(CtTypeInformation::getQualifiedName)
                    .map(x -> ArgumentDetail.builder()
                                            .type(x)
                                            .summary(fieldRead.toString())
                                            .build())
                    .orElse(null);
        }

        if (it instanceof CtVariableRead<?> variableRead) {
            MethodDetail method = getMethod((CtElement) it);
            if(method == null) {
                return null;
            }
            MethodProcessor.LocalVariableDetails details = method.getVariableDetails().get(variableRead.getVariable().getSimpleName());
            return ofNullable(details).map(x -> ArgumentDetail.builder()
                                                              .type(x.getType())
                                                              .summary(x.getVariableWriteSummary())
                                                              .build())
                                      .orElse(null);
        }
        return null;
    }

    public static Optional<CtTypeReference<?>> targetType(CtInvocation<?> invocation) {
        var target = invocation.getTarget();
        if (target == null) {
            return empty();
        }

        var referenceType = target.getType();
        return ofNullable(referenceType);
    }

    boolean isInterestingTarget(CtInvocation<?> invocation) {
        var target = invocation.getTarget();
        if (target == null) {
            return false;
        }

        var referenceType = target.getType();
        if (target instanceof CtFieldRead) {
            referenceType = ((CtFieldRead<?>) target).getVariable().getType();
        }
        return referenceType != null && this.filter.test(referenceType);
    }
}