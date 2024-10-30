package com.satya.projectanalysis.matchers;

import spoon.reflect.reference.CtTypeReference;

public class HasAnnotationMatcher implements TypeMatcher {
    private final String annotationName;

    public HasAnnotationMatcher(String annotationName) {
        this.annotationName = annotationName;
    }

    @Override
    public boolean test(CtTypeReference type) {
        return type.getAnnotations().stream().anyMatch(a -> a.getAnnotationType().getQualifiedName().equals(annotationName));
    }
}