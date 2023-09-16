package ru.smcsystem.smcmodules.module.world;

import ru.smcsystem.api.tools.execution.ExecutionContextTool;

import java.awt.*;

public interface WorldObject {

    int damage(WorldObject object, int damage);

    void process();

    int getLife();

    Shape getShape();

    boolean mayIntersect(WorldObject object);

    boolean isTransparent();

    Integer getOrder();

    Direction getDirection();

    void draw(ExecutionContextTool executionContextTool);
}
