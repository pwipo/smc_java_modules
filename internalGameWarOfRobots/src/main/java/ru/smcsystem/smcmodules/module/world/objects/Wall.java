package ru.smcsystem.smcmodules.module.world.objects;

import ru.smcsystem.api.tools.execution.ExecutionContextTool;
import ru.smcsystem.smcmodules.module.world.Direction;
import ru.smcsystem.smcmodules.module.world.World;
import ru.smcsystem.smcmodules.module.world.WorldObjectImpl;

import java.awt.*;
import java.awt.geom.Rectangle2D;

public class Wall extends WorldObjectImpl implements Barrier {

    private final int color;
    private final Integer imageId;

    public Wall(World worldMap, int life, int width, int height, int color, Integer imageId) {
        super(worldMap, null, null, life, Direction.DOWN);
        this.color = color;
        this.imageId = imageId;

        Point leftUpperPointOfRandomFreeSector = worldMap.convertSectorIdToPoint(worldMap.getRandomFreeSector());
        setShape(new Rectangle2D.Double(leftUpperPointOfRandomFreeSector.x, leftUpperPointOfRandomFreeSector.y, width, height) {
        });
    }

    @Override
    public void draw(ExecutionContextTool executionContextTool) {
        if (imageId != null) {
            executionContextTool.addMessage(7);
            executionContextTool.addMessage(imageId);
        } else {
            executionContextTool.addMessage(1);
            executionContextTool.addMessage(1);
            executionContextTool.addMessage(color);
            executionContextTool.addMessage(1);
        }
        Rectangle bounds = getShape().getBounds();
        executionContextTool.addMessage(bounds.x);
        executionContextTool.addMessage(bounds.y);
        executionContextTool.addMessage(bounds.width);
        executionContextTool.addMessage(bounds.height);
    }

}
