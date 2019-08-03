package com.satya.projectanalysis;

import java.lang.FunctionalInterface;

@FunctionalInterface
public interface ExceptionThrowingSupplier<T> {
    T get() throws Exception;
}
