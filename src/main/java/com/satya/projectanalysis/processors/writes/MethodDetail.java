package com.satya.projectanalysis.processors.writes;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

import com.satya.projectanalysis.MethodProcessor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.Builder;
import lombok.Value;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.code.CtVariableRead;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtTypeInformation;
import spoon.reflect.reference.CtTypeReference;
import spoon.support.reflect.code.CtReturnImpl;

@Builder
@Value
public class MethodDetail {
    String fullyQualifiedClassName;
    String methodName;
    //        Map<String, String> parameters;
    @Builder.Default
    List<String> parameters = new ArrayList<>();

    String methodSignature;

    String implementation;
    //Name, detail
    @Builder.Default
    Map<String, MethodProcessor.LocalVariableDetails> variableDetails = new HashMap<>();
    String returnType;
    String returnExpressionSummary;
    String summary;
    String path;

    private static String returnType(CtMethod<?> method) {
        CtTypeReference<?> type = method.getType();
        if (type != null) {
            return type.getQualifiedName();
        }
        return null;
    }

    private static Optional<String> returnType(CtBlock<?> body) {
        if (body == null || body.getStatements() == null) {
            return empty();
        }
        return body.getStatements().stream()
                   .filter(it -> it instanceof CtReturnImpl)
                   .findFirst()
                   .map(it -> (CtReturnImpl<?>) it)
                   .map(CtReturnImpl::getReturnedExpression)
                   .map(MethodDetail::returnedExpressionType);
    }

    private static String returnedExpressionType(CtExpression<?> it) {
        if (it instanceof CtInvocation) {
            CtInvocation<?> invocation = (CtInvocation<?>) it;
            /*
             * Enhance this
             * It is null in case of, for example, ((T) (memoizedFactory.apply(avroEvent.getClass()).toDomain(avroEvent)))
             */
            return ofNullable(invocation.getExecutable()
                                        .getType())
                    .map(CtTypeInformation::getQualifiedName)
                    .orElse(null);
        }
        if (it instanceof CtVariableRead) {
            CtVariableRead<?> variableRead = (CtVariableRead<?>) it;
            return variableRead.getVariable().getType().getQualifiedName();
        }
        return null;
    }

    public static MethodDetail from(CtMethod<?> method) {
        return MethodDetail.builder()
                           .methodName(method.getSimpleName())
                           .parameters(MethodProcessor.parameters(method))
                           .fullyQualifiedClassName(MethodProcessor.className(method))
                           .returnType(returnType(method.getBody()).orElse(returnType(method)))
                           .methodSignature(MethodProcessor.signature(method))
                           .path(method.getPath().toString())
                           .variableDetails(variables(method))
//                           .implementation(method.toString())
                           .build();
    }

    private static Map<String, MethodProcessor.LocalVariableDetails> variables(CtMethod<?> method) {
        Map<String, MethodProcessor.LocalVariableDetails> variableDetailsMap = new HashMap<>();
        method.getBody().getStatements().forEach(var -> {
            MethodProcessor.LocalVariableDetails details = null;
            if (var instanceof CtLocalVariable) {
                details = MethodProcessor.LocalVariableDetails.from((CtLocalVariable<?>) var);
            }

            if (details != null) {
                variableDetailsMap.put(details.getName(), details);
            }
        });
        return variableDetailsMap;
    }
}
