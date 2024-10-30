package com.satya.projectanalysis.matchers;

import spoon.reflect.reference.CtTypeReference;

public class IsInPackageMatcher implements TypeMatcher {
    private final String packageName;

    public IsInPackageMatcher(String packageName) {
        this.packageName = packageName;
    }

    @Override
    public boolean test(CtTypeReference type) {
        return type.getQualifiedName().startsWith(packageName);
    }
}