package com.satya.projectanalysis;

public class ClassRelationshipBuilder {
    private Node node;
    private RelationshipType relationshipType;
    private String referenceName = "";
    private String target;

    public ClassRelationshipBuilder setNode(Node node) {
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

    public ClassRelationshipBuilder setTarget(String target) {
        this.target = target;
        return this;
    }

    public Global.ClassRelationship build() {
        return new Global.ClassRelationship(node, relationshipType, referenceName, target);
    }
}