package ru.smcsystem.smcmodules.module.dijkstra;

import java.util.Objects;

public class Edge {

    private final Vertex target;
    private double weight;

    Edge(Vertex target, double weight) {
        Objects.requireNonNull(target);
        this.target = target;
        this.weight = weight;
    }

    public Vertex getTarget() {
        return target;
    }

    public double getWeight() {
        return weight;
    }

    void setWeight(double weight) {
        this.weight = weight;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Edge{");
        sb.append("target=").append(target);
        sb.append(", weight=").append(weight);
        sb.append('}');
        return sb.toString();
    }

}
