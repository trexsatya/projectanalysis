package com.satya.projectanalysis;

import java.util.List;
import java.util.Objects;

public class Node {
    public enum Type {
        CLASS, INTERFACE
    }

    int id;
    public String name;

    public String simpleName;

    public Type type;

    ClassData classData;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node node = (Node) o;
        return name.equals(node.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return this.name;
    }
}
