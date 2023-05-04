package com.satya.projectanalysis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtMethod;
import spoon.support.reflect.declaration.CtMethodImpl;

import java.io.File;
import java.util.Optional;

/**
 *  Get list of targets we are interested in for method call: - entities, eventProducer
 *
 */
public class GenericProcessor extends AbstractProcessor<CtElement>{
    static Logger LOG = LoggerFactory.getLogger(GenericProcessor.class);

    @Override
    public void process(CtElement element) {
        if(Optional.ofNullable(element.getPosition())
                .map(SourcePosition::getCompilationUnit)
                .map(CtCompilationUnit::getFile)
                .map(File::getAbsolutePath)
                .filter(it -> it.contains("ExternalApiV2Impl")).isPresent()) {
//            System.out.println("\nel...." + element.getClass() + "....." + getPosition(element) + "\n" + element + "");
        }
        if(element instanceof CtLocalVariable) {
            if(element.toString().contains("changeOwnership")) {
//                System.out.println();
            }
        }
    }

    private String getPosition(CtElement element) {
        return Optional.ofNullable(element.getPosition()).map(SourcePosition::toString).orElse(null);
    }
}