package com.satya.projectanalysis;


import org.apache.log4j.Level;
import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtCatch;


public class CatchProcessor extends AbstractProcessor<CtCatch> {
    public void process(CtCatch element) {
        if ((element.getBody().getStatements().size()) == 0) {
            getFactory().getEnvironment().report(this, Level.WARN, element, "empty catch clause");
            System.out.println("Empty catch");
        }
    }
}

