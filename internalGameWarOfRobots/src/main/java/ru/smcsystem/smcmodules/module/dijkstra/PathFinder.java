package ru.smcsystem.smcmodules.module.dijkstra;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class PathFinder {

    private final List<Vertex> vertices;

    public PathFinder(List<Object> ids) {
        this.vertices = ids.stream().map(Vertex::new).collect(Collectors.toList());
    }

    public PathFinder(int size) {
        this.vertices = new ArrayList<>(size);
        for (int i = 0; i < size; i++)
            this.vertices.add(new Vertex(i));
    }

    public void addEdge(int id, int targetId, double weight) {
        if (id >= 0 && id < getVertices().size() && targetId >= 0 && targetId < getVertices().size())
            vertices.get(id).addEdge(new Edge(vertices.get(targetId), weight));
    }

    public void changeEdge(int id, int targetId, double weight) {
        Vertex target = vertices.get(targetId);
        vertices.get(id).getAdjacencies().stream().filter(e -> e.getTarget().equals(target)).forEach(e -> e.setWeight(weight));
    }

    public void computePaths(int id) {
        vertices.forEach(Vertex::clear);

        Vertex source = vertices.get(id);
        source.changeMinDistance(null, 0.);
        LinkedList<Vertex> vertexQueue = new LinkedList<Vertex>();
        vertexQueue.add(source);

        while (!vertexQueue.isEmpty()) {
            Vertex u = vertexQueue.poll();

            // Visit each edge exiting u
            for (Edge e : u.getAdjacencies()) {
                Vertex v = e.getTarget();
                double weight = e.getWeight();
                double distanceThroughU = u.getMinDistance() + weight;
                if (distanceThroughU < v.getMinDistance()) {
                    v.changeMinDistance(u, distanceThroughU);
                    if (!vertexQueue.contains(v))
                        vertexQueue.add(v);
                }
            }
        }
    }

    public List<Vertex> getShortestPathTo(int id) {
        Vertex target = vertices.get(id);
        if (target.getPrevious() == null)
            return null;
        List<Vertex> path = new ArrayList<Vertex>();
        for (Vertex vertex = target; vertex != null; vertex = vertex.getPrevious())
            path.add(vertex);
        Collections.reverse(path);
        return path;
    }

    public List<Vertex> getVertices() {
        return Collections.unmodifiableList(vertices);
    }

    public Vertex findById(int id) {
        // vertices.stream().filter(v -> v.getId().equals(id)).findAny()
        return vertices.get(id);
    }

    public void changeAllEdgeWeightTo(Vertex vertex, double newWeight) {
        vertices.forEach(v -> {
            v.getAdjacencies().stream()
                    .filter(e -> e.getTarget().equals(vertex))
                    .forEach(e -> e.setWeight(newWeight));
        });
    }

}
