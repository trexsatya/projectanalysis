package com.satya.projectanalysis.matchers;

import static java.util.stream.Collectors.toList;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import spoon.reflect.reference.CtTypeReference;

public class ImplementsInterfacesMatcher implements TypeMatcher {
    private final List<String> interfaces;

    public ImplementsInterfacesMatcher(List<String> interfaces) {
        this.interfaces = interfaces;
    }

    public ImplementsInterfacesMatcher(String... interfaces) {
        this.interfaces = Arrays.stream(interfaces).collect(toList());
    }

    @Override
    public boolean test(CtTypeReference type) {
        Set<CtTypeReference> implementedInterfaces = type.getSuperInterfaces();
        return interfaces.stream().allMatch(i -> implementedInterfaces.stream().anyMatch(si -> si.getQualifiedName().equals(i)));
    }
}