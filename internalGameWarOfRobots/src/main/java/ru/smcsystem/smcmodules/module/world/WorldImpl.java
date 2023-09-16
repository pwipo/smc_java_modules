package ru.smcsystem.smcmodules.module.world;

import org.apache.commons.lang3.StringUtils;
import ru.smcsystem.api.tools.ConfigurationTool;
import ru.smcsystem.api.tools.execution.ExecutionContextTool;
import ru.smcsystem.smc.utils.ModuleUtils;
import ru.smcsystem.smcmodules.module.dijkstra.PathFinder;

import java.awt.*;
import java.awt.geom.Area;
import java.util.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WorldImpl implements World {
    private final List<Set<WorldObject>> map;

    private final List<WorldObject> objects;

    private final int mapWidth;
    private final int mapHeight;

    private final int mapWidthInView;
    private final int mapHeightInView;

    private final int sectorWidth;
    private final int sectorHeight;

    private final int backgroundColor;

    private final Set<Integer> forRepaint;

    private final Random rnd;

    private PathFinder pathFinder;

    public WorldImpl(int mapWidth, int mapHeight, int mapWidthInView, int mapHeightInView, int sectorWidth, int sectorHeight, int backgroundColor) {
        this.sectorWidth = sectorWidth;
        this.sectorHeight = sectorHeight;
        this.mapWidth = mapWidth;
        this.mapHeight = mapHeight;
        this.mapWidthInView = mapWidthInView;
        this.mapHeightInView = mapHeightInView;
        this.backgroundColor = backgroundColor;

        map = new CopyOnWriteArrayList<>();
        objects = new CopyOnWriteArrayList<>();
        forRepaint = new HashSet<>();

        //repaint all first time
        Stream.iterate(0, n -> n + 1)
                .limit(mapWidth * mapHeight)
                .forEach(forRepaint::add);

        rnd = new Random();

        pathFinder = new PathFinder(mapWidth * mapHeight);
        for (int i = 0; i < pathFinder.getVertices().size(); i++) {
            pathFinder.addEdge(i, i - mapWidth, 1);
            pathFinder.addEdge(i, i + mapWidth, 1);
            pathFinder.addEdge(i, i - 1, 1);
            pathFinder.addEdge(i, i + 1, 1);
        }

    }

    @Override
    public boolean add(WorldObject object) {
        Objects.requireNonNull(object);
        if (objects.contains(object))
            return true;
        if (!isAvailablePosition(object, null))
            return false;

        List<Integer> nearbyRectangles = findSectors(object);
        nearbyRectangles.forEach(i -> getSet(i).add(object));
        forRepaint.addAll(nearbyRectangles);

        objects.add(object);
        objects.sort(Comparator.comparingInt(a -> a.getOrder() != null ? a.getOrder() : Integer.MAX_VALUE));
        return true;
    }

    @Override
    public void process(ConfigurationTool configurationTool, ExecutionContextTool executionContextTool) {
        objects.stream()
                .filter(wo -> wo.getOrder() != null)
                .forEach(wo -> {
                    try {
                        wo.process();
                    } catch (Exception e) {
                        executionContextTool.addError(ModuleUtils.getErrorMessageOrClassName(e));
                        configurationTool.loggerWarn(ModuleUtils.getStackTraceAsString(e));
                    }
                });

        objects.stream()
                .filter(wo -> wo.getLife() <= 0)
                .forEach(wo -> {
                    map.forEach(list -> list.remove(wo));
                    objects.remove(wo);
                    forRepaint.addAll(findSectors(wo));
                });

    }

    @Override
    public void draw(ExecutionContextTool executionContextTool) {
        forRepaint.add(0);
        forRepaint.stream()
                .map(this::convertSectorIdToPoint)
                .forEach(p -> {
                    executionContextTool.addMessage(1);
                    executionContextTool.addMessage(1);
                    executionContextTool.addMessage(backgroundColor);
                    executionContextTool.addMessage(1);
                    executionContextTool.addMessage(p.x);
                    executionContextTool.addMessage(p.y);
                    executionContextTool.addMessage(sectorWidth);
                    executionContextTool.addMessage(sectorHeight);
                });
        forRepaint.stream()
                .flatMap(i -> getSet(i).stream())
                .distinct()
                .sorted(Comparator.comparingInt(a -> ((WorldObject) a).getOrder() != null ? ((WorldObject) a).getOrder() : Integer.MAX_VALUE).reversed())
                .forEach(wo -> wo.draw(executionContextTool));
        forRepaint.clear();
    }


    @Override
    public boolean change(WorldObject object, Shape shape) {
        Objects.requireNonNull(object);
        Objects.requireNonNull(shape);
        if (!objects.contains(object))
            throw new RuntimeException("object not present in world");
        if (!isAvailablePosition(object, shape))
            return false;

        /*
        List<Integer> nearbyRectangles = findSectors(object);
        nearbyRectangles.forEach(i -> getSet(i).remove(object));
        forRepaint.addAll(nearbyRectangles);
        */
        for (int i = 0; i < map.size(); i++) {
            Set<WorldObject> set = getSet(i);
            if (!set.contains(object))
                continue;
            set.remove(object);
            forRepaint.add(i);
        }

        List<Integer> nearbyRectangles = findSectors(object, shape);
        nearbyRectangles.forEach(i -> getSet(i).add(object));
        forRepaint.addAll(nearbyRectangles);

        return true;
    }

    private List<WorldObject> findNearbyObjects(WorldObject object) {
        return findSectors(object).stream()
                .flatMap(i -> getSet(i).stream())
                .filter(o -> !o.equals(object))
                .distinct()
                .collect(Collectors.toList());
    }

    private boolean isAvailablePosition(WorldObject object, Shape shape) {
        return findSectors(object, shape).stream().allMatch(id -> id >= 0 && id < mapWidth * mapHeight)
                && findIntersects(object, shape).stream().allMatch(insect -> insect.mayIntersect(object) || object.mayIntersect(insect));
    }

    @Override
    public List<WorldObject> findIntersects(WorldObject object) {
        return findIntersects(object, null);
    }

    private List<Point> getBoundPoints(Shape shape) {
        Objects.requireNonNull(shape);
        Rectangle bounds = shape.getBounds();
        return List.of(
                new Point(bounds.x, bounds.y)
                , new Point(bounds.x + bounds.width, bounds.y)
                , new Point(bounds.x, bounds.y + bounds.height)
                , new Point(bounds.x + bounds.width, bounds.y + bounds.height)
        );
    }

    private List<WorldObject> findIntersects(WorldObject object, Shape shape) {
        Objects.requireNonNull(object);
        Shape newShape = shape != null ? shape : object.getShape();
        Area area = new Area(newShape);
        return findSectors(object, newShape).stream()
                .filter(id -> id >= 0 && id < mapWidth * mapHeight)
                .flatMap(i -> getSet(i).stream())
                .distinct()
                .filter(o -> !o.equals(object))
                .filter(wo -> {
                    Area a = new Area(wo.getShape());
                    a.intersect(area);
                    return !a.isEmpty();
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Integer> findSectors(WorldObject object) {
        return findSectors(object, null);
    }

    private List<Integer> findSectors(WorldObject object, Shape shape) {
        Objects.requireNonNull(object);
        Shape newShape = shape != null ? shape : object.getShape();
        return getBoundPoints(newShape).stream()
                .map(this::convertPointToMapSectorId)
                .distinct()
                // .filter(id -> id >= 0 && id < mapWidth * mapHeight)
                .collect(Collectors.toList());
    }

    private int convertPointToMapSectorId(Point p) {
        int x = p.x;
        int y = p.y;
        int worldRectX = x / sectorWidth;
        if(worldRectX<0 || worldRectX>=mapWidth)
            return -1;
        int worldRectY = y / sectorHeight;
        if(worldRectY<0 || worldRectY>=mapHeight)
            return -1;
        // return getSet(worldRectCoordinate);
        return worldRectY * mapWidth + worldRectX;
    }

    @Override
    public Point convertSectorIdToPoint(int sectorId) {
        return new Point((sectorId % mapWidth) * sectorWidth, (sectorId / mapWidth) * sectorHeight);
    }

    private Set<WorldObject> getSet(int sectorId) {
        if (sectorId < 0 || sectorId >= mapWidth * mapHeight)
            throw new IllegalArgumentException("sectorId");
        while (map.size() <= sectorId)
            map.add(new HashSet<>());
        return map.get(sectorId);
    }

    @Override
    public int getRandomFreeSector() {
        int sectorId = 0;
        do {
            sectorId = rnd.nextInt(mapWidth * mapHeight);
        } while (!getSet(sectorId).stream().allMatch(wo -> wo.mayIntersect(null)));
        return sectorId;
    }

    @Override
    public Integer getNextSector(int sectorId, Direction direction) {
        Objects.requireNonNull(direction);
        if (sectorId < 0 || sectorId >= mapWidth * mapHeight)
            return null;

        int sectorX = sectorId % mapWidth;
        int sectorY = sectorId / mapWidth;

        int dx = 0;
        int dy = 0;
        switch (direction) {
            case LEFT:
                if (sectorX > 0)
                    dx = -1;
                break;
            case UP:
                if (sectorY > 0)
                    dy = -1;
                break;
            case RIGHT:
                if (sectorX + 1 < mapWidth)
                    dx = 1;
                break;
            case DOWN:
                if (sectorY + 1 < mapHeight)
                    dy = 1;
                break;
        }

        return (dx != 0 || dy != 0) ? ((sectorY + dy) * mapWidth + (sectorX + dx)) : null;
    }

    @Override
    public Direction getDirection(WorldObject object, int targetSectorId) {
        Integer sectorId = findSectors(object).stream().findFirst().orElseThrow(() -> new RuntimeException("object no on map"));
        Direction direction = null;
        for (int i = 0; i < Direction.values().length; i++) {
            Direction directionTmp = Direction.values()[i];
            Integer nextSector = getNextSector(sectorId, directionTmp);
            if (Objects.equals(targetSectorId, nextSector)) {
                direction = directionTmp;
                break;
            }
        }
        return direction;
    }

    @Override
    public List<WorldObject> getVisibleObjects(WorldObject object) {
        Objects.requireNonNull(object);

        List<Integer> visibleSectors = new ArrayList<>();
        findSectors(object).forEach(sectorId -> {
            Integer nextSectorId = sectorId;
            do {
                visibleSectors.add(nextSectorId);
                nextSectorId = getNextSector(nextSectorId, object.getDirection());
            } while (nextSectorId != null && getSet(nextSectorId).stream().allMatch(WorldObject::isTransparent));
        });

        return getObjects(visibleSectors, object);
    }

    private List<WorldObject> getObjects(List<Integer> sectorIds, WorldObject object) {
        return sectorIds.stream()
                .flatMap(sectorId -> this.getObjects(sectorId, object).stream())
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public List<WorldObject> getObjects(int sectorId, WorldObject object) {
        return this.getSet(sectorId).stream()
                .distinct()
                .filter(wo -> !wo.equals(object))
                .collect(Collectors.toList());
    }

    @Override
    public boolean isAvailable(int sectorId, WorldObject object) {
        return sectorId>=0 && sectorId<mapWidth*mapHeight &&
                getObjects(sectorId, object).stream().allMatch(insect -> insect.mayIntersect(object) || (object != null && object.mayIntersect(insect)));
    }

    @Override
    public List<Integer> getSectorsPathTo(WorldObject object, int sectorId) {
        for (int i = 0; i < map.size(); i++)
            pathFinder.changeAllEdgeWeightTo(pathFinder.findById(i), isAvailable(i, null) ? 1 : Integer.MAX_VALUE);

        findSectors(object).forEach(objectSectorId -> pathFinder.computePaths(objectSectorId));
        return pathFinder.getShortestPathTo(sectorId).stream()
                .map(v -> (Integer) v.getId())
                .collect(Collectors.toList());
    }

}
