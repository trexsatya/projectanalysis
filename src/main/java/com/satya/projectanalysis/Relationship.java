package com.satya.projectanalysis;

class Relationship {
    public String className;
    public RelationshipType relationshipType;

    public Relationship(String className, RelationshipType relationshipType) {
        this.className = className;
        this.relationshipType = relationshipType;
    }
}
