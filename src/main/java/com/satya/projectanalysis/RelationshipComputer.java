package com.satya.projectanalysis;

import spoon.reflect.declaration.CtClass;

public interface RelationshipComputer {
    default Relationship relationship(CtClass ctClassA, CtClass ctClassB){

        return null;
    }
}
