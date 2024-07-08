package org.example;

import lombok.Getter;

@Getter
public class Node<T> {
    private final T value;
    private final Node<T> parent;
    private final int level;

    public Node(T value, int level, Node<T> parent) {
        this.value = value;
        this.level = level;
        this.parent = parent;
    }
}
