package com.satya.projectanalysis;

import java.util.*;

public enum Global {
    INSTANCE;

    public static class ClassRelationship {
        public Node node;
        public RelationshipType relationshipType;
        public String referenceName = "";
        public String target;


        public ClassRelationship(Node node, RelationshipType relationshipType, String referenceName, String target) {
            this.node = node;
            this.relationshipType = relationshipType;
            this.referenceName = referenceName;
            this.target = target;
        }

        @Override
        public String toString() {
            return node + " " + relationshipType + " " + referenceName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ClassRelationship classRelationship = (ClassRelationship) o;
            return Objects.equals(node, classRelationship.node) &&
                    relationshipType == classRelationship.relationshipType &&
                    Objects.equals(referenceName, classRelationship.referenceName) &&
                    Objects.equals(target, classRelationship.target);
        }

        @Override
        public int hashCode() {
            return Objects.hash(node, relationshipType, referenceName, target);
        }
    }

    //Id -> Node
    private Map<String, Node> nodes = new HashMap<>();

    //NodeId -> NodeId
    private Map<ClassRelationship, Boolean> relationshipMap = new HashMap<>();

    private Map<String, ClassData> classDataMap = new HashMap<>();

    public void addNode(Node node){
        nodes.put(node.name, node);
    }


    public Map<String, Node> getNodes(){
        return nodes;
    }

    public Node getOrCreateNode(String name, Node.Type nodeType){
        Node node = nodes.getOrDefault(name, newNode(name));
        node.type = nodeType;
        return node;
    }

    private Node newNode(String name){
        Node newNode = new Node();
        newNode.name= name;
        nodes.put(name, newNode);
        return newNode;
    }

    public void addIsALink(Node parent, Node child) {
        relationshipMap.put(new ClassRelationshipBuilder().setNode(child).setRelationshipType(RelationshipType.IS_A).setTarget(parent.name).build(), true);
    }

    public void addIsLikeLink(Node thisClass, Node isLikeThis) {

        relationshipMap.put(new ClassRelationshipBuilder().setNode(thisClass).setRelationshipType(RelationshipType.IS_LIKE).setTarget(isLikeThis.name).build(), true);
    }

    public void addThisRefersToThatLink(Node thisClass, Node thatClass, String simpleName) {
        relationshipMap.put(new ClassRelationshipBuilder().setNode(thisClass).setRelationshipType(RelationshipType.IS_COMPOSED_OF).setReferenceName(simpleName).setTarget(thatClass.name).build(), true);
    }

    public Map<ClassRelationship, Boolean> getRelationships(){
        return relationshipMap;
    }

    public void addClassData(String classNm, ClassData classData){
        classDataMap.put(classNm, classData);
    }

    public ClassData getClassData(String id){
        return classDataMap.get(id);
    }
}
