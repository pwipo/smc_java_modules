package ru.smcsystem.smcmodules.module.dijkstra;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Vertex {

    private final Object id;
    private final List<Edge> adjacencies;
    private double minDistance;
    private Vertex previous;

    Vertex(Object id) {
        // Objects.requireNonNull(id);
        this.id = id;
        adjacencies = new ArrayList<>();
        clear();
    }

    public Object getId() {
        return id;
    }

    void changeMinDistance(Vertex previous, double minDistance) {
        this.previous = previous;
        this.minDistance = minDistance;
    }

    public List<Edge> getAdjacencies() {
        return Collections.unmodifiableList(adjacencies);
    }

    void addEdge(Edge edge) {
        Objects.requireNonNull(edge);
        if (!adjacencies.contains(edge))
            adjacencies.add(edge);
    }

    public double getMinDistance() {
        return minDistance;
    }

    public Vertex getPrevious() {
        return previous;
    }

    void clear(){
        minDistance = Double.POSITIVE_INFINITY;
        previous=null;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Vertex{");
        sb.append("id=").append(id);
        sb.append(", ");
        sb.append("minDistance=").append(minDistance);
        sb.append('}');
        return sb.toString();
    }
}
