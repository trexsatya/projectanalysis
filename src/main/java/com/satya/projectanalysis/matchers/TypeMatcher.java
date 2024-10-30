package com.satya.projectanalysis.matchers;

import java.util.function.Predicate;
import spoon.reflect.reference.CtTypeReference;

public interface TypeMatcher extends Predicate<CtTypeReference<?>> {
    boolean test(CtTypeReference type);
}

