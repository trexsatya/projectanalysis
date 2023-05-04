package com.satya.projectanalysis;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

public enum Global {
    INSTANCE;

    public static class ClassPool {
        private static final Map<String, Map<String, MethodProcessor.MethodDetail>> methods = new HashMap<>();

        public static void addMethod(MethodProcessor.MethodDetail detail) {
            methods.computeIfAbsent(detail.getFullyQualifiedClassName(), k -> new HashMap<>())
                    .put(detail.getMethodSignature(), detail);
        }

        /**
         * TODO: Add support for regex
         */
        public static MethodProcessor.MethodDetail getMethod(String className, String methodSignature) {
            return Optional.ofNullable(methods.get(className))
                    .map(it -> it.get(methodSignature))
                    .orElse(null);
        }

        public static MethodProcessor.LocalVariableDetails getMethodVariable(String className, String methodSignature, String variableName) {
            return Optional.ofNullable(getMethod(className, methodSignature))
                    .map(it -> it.getVariableDetails().get(variableName))
                    .orElse(null);
        }
    }

    public static class ClassRelationship {
        public int node;
        public RelationshipType relationshipType;
        public String referenceName = "";
        public int target;


        public ClassRelationship(int node, RelationshipType relationshipType, String referenceName, int target) {
            this.node = node;
            this.relationshipType = relationshipType;
            this.referenceName = referenceName;
            this.target = target;
        }

        @Override
        public String toString() {
            return node + " " + relationshipType + " " + referenceName + ":" + target;
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
    private final Map<String, Node> nodes = new HashMap<>();

    //NodeId -> NodeId
    private final Map<ClassRelationship, Boolean> relationshipMap = new HashMap<>();

    private final Map<String, ClassData> classDataMap = new HashMap<>();


    public Map<String, Node> getNodes(){
        return nodes;
    }

    final AtomicInteger counter = new AtomicInteger(0);
    public Node getOrCreateNode(String name, Node.Type nodeType){
        return getOrCreateNode(name, nodeType, null);
    }

    public Node getOrCreateNode(String name, Node.Type nodeType, ClassData classData){
        Node node = nodes.get(name);
        if(node == null) {
            node = newNode(name);
        }
        if(node.classData == null) {
            node.classData = classData;
        }
        node.type = nodeType;
        return node;
    }

    //Antipattern: Function having side effect
    private Node newNode(String name){
        Node newNode = new Node();
        newNode.name= name;
        newNode.id = counter.getAndIncrement();
        nodes.put(name, newNode);
        return newNode;
    }

    public void addIsALink(Node parent, Node child) {
        relationshipMap.put(new ClassRelationshipBuilder().setNode(child.id).setRelationshipType(RelationshipType.IS_A).setTarget(parent.id).build(), true);
    }

    public void addIsLikeLink(Node thisClass, Node isLikeThis) {
        relationshipMap.put(new ClassRelationshipBuilder().setNode(thisClass.id).setRelationshipType(RelationshipType.IS_LIKE).setTarget(isLikeThis.id).build(), true);
    }

    public void addThisRefersToThatLink(Node thisClass, Node thatClass, String simpleName) {
        relationshipMap.put(new ClassRelationshipBuilder().setNode(thisClass.id).setRelationshipType(RelationshipType.IS_COMPOSED_OF).setReferenceName(simpleName).setTarget(thatClass.id).build(), true);
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

    public Set<ClassData> implementationsOf(String qualifiedName) {
        return classDataMap.values().stream()
                .filter(classData -> classData.getImplementss().stream().map(x -> x.name).collect(toList())
                        .contains(qualifiedName))
                .collect(toSet());
    }

    private static <T> Set<T> intersection(List<T> list, List<T> otherList){
        return list.stream()
                .distinct()
                .filter(otherList::contains)
                .collect(Collectors.toSet());
    }
    public Set<ClassData> withAnnotationsLike(List<String> toMatch) {
        toMatch = toMatch.stream().map(it -> ".*" + it + ".*").collect(toList());
        List<String> finalToMatch = toMatch;
        return classDataMap.values().stream()
                .filter(classData -> !intersection(classData.getAnnotations().stream().map(x -> x.name).collect(toList()), finalToMatch).isEmpty())
                .collect(toSet());
    }

    public Set<ClassData> all(){
        return new HashSet<>(classDataMap.values());
    }
}
