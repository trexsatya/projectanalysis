package com.satya.projectanalysis;

public class ClassRelationshipBuilder {
    private int node;
    private RelationshipType relationshipType;
    private String referenceName = "";
    private int target;

    public ClassRelationshipBuilder setNode(int node) {
        this.node = node;
        return this;
    }

    public ClassRelationshipBuilder setRelationshipType(RelationshipType relationshipType) {
        this.relationshipType = relationshipType;
        return this;
    }

    public ClassRelationshipBuilder setReferenceName(String referenceName) {
        this.referenceName = referenceName;
        return this;
    }

    public ClassRelationshipBuilder setTarget(int target) {
        this.target = target;
        return this;
    }

    public Global.ClassRelationship build() {
        return new Global.ClassRelationship(node, relationshipType, referenceName, target);
    }
}