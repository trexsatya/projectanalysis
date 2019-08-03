package com.satya.projectanalysis;

import java.util.*;

public enum Global {
    INSTANCE;

    class Graph {
        private Map<String, List<String>> adjList = new HashMap<>();

        void addEdge(String x, String y){
            List<String> list = adjList.getOrDefault(x, new ArrayList<>());
            list.add(y);
            adjList.put(x, list);

            List<String> list1 = adjList.getOrDefault(y, new ArrayList<>());
            list1.add(x);
            adjList.put(y, list1);
        }
    }

    public static class Pair {
        public Node node;
        public RelationshipType relationshipType;

        public Pair(Node node, RelationshipType relationshipType) {
            this.node = node;
            this.relationshipType = relationshipType;
        }

        @Override
        public String toString() {
            return node + " " + relationshipType;
        }
    }

    //Id -> Node
    private Map<String, Node> nodes = new HashMap<>();

    //NodeId -> NodeId
    private Map<Pair, String> relationshipMap = new HashMap<>();

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
        relationshipMap.put(new Pair(child, RelationshipType.IS_A), child.name);
    }

    public void addIsLikeLink(Node thisClass, Node isLikeThis) {

        relationshipMap.put(new Pair(thisClass, RelationshipType.IS_LIKE), isLikeThis.name);
    }

    public void addThisRefersToThatLink(Node thisClass, Node thatClass) {
        relationshipMap.put(new Pair(thisClass, RelationshipType.IS_COMPOSED_OF), thatClass.name);
    }

    public Map<Pair, String> getRelationships(){
        return relationshipMap;
    }
}
