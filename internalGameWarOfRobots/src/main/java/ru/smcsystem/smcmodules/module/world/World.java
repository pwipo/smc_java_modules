package ru.smcsystem.smcmodules.module.world;

import ru.smcsystem.api.tools.ConfigurationTool;
import ru.smcsystem.api.tools.execution.ExecutionContextTool;

import java.awt.*;
import java.util.List;

public interface World {

    boolean add(WorldObject object);

    void process(ConfigurationTool configurationTool, ExecutionContextTool executionContextTool);

    void draw(ExecutionContextTool executionContextTool);

    boolean change(WorldObject object, Shape shape);

    List<WorldObject> findIntersects(WorldObject object);

    List<Integer> findSectors(WorldObject object);

    Point convertSectorIdToPoint(int sectorId);

    int getRandomFreeSector();

    Integer getNextSector(int sectorId, Direction direction);

    Direction getDirection(WorldObject object, int targetSectorId);

    List<WorldObject> getVisibleObjects(WorldObject object);

    List<WorldObject> getObjects(int sectorId, WorldObject object);

    boolean isAvailable(int sectorId, WorldObject object);

    List<Integer> getSectorsPathTo(WorldObject object, int sectorId);
}
