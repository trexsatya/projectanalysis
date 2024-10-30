package com.satya.projectanalysis.processors.writes;

import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtVariableWrite;

public class VariableWritesProcessor extends AbstractProcessor<CtVariableWrite> {

    @Override
    public void process(CtVariableWrite element) {
        System.out.println();
    }
}
