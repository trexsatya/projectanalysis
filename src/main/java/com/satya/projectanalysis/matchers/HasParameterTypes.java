package com.satya.projectanalysis.matchers;

import spoon.reflect.reference.CtTypeReference;

public class HasParameterTypes implements TypeMatcher{
    @Override
    public boolean test(CtTypeReference type) {
        return false;
    }
}
