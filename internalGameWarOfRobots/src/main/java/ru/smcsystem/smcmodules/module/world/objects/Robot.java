package ru.smcsystem.smcmodules.module.world.objects;

import org.apache.commons.lang3.ArrayUtils;
import ru.smcsystem.api.tools.execution.ExecutionContextTool;
import ru.smcsystem.smcmodules.module.world.Direction;
import ru.smcsystem.smcmodules.module.world.World;
import ru.smcsystem.smcmodules.module.world.WorldObject;
import ru.smcsystem.smcmodules.module.world.WorldObjectImpl;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.Random;

abstract public class Robot extends WorldObjectImpl {

    protected final int width;
    protected final int height;

    protected final int speed;

    protected final int bulletRadius;
    protected final int bulletSpeed;
    protected final int bulletLife;
    protected final int reloadInterval;
    protected int countIterationFromLastFire;
    private final int color;
    private final java.util.List<Integer> imageIds;

    // private Random rnd;

    protected Robot(World worldMap, Integer order, int life, int width, int height, int speed, int reloadInterval, int bulletRadius, int bulletSpeed, int bulletLife, int color, java.util.List<Integer> imageIds) {
        super(worldMap, order, null, life, Direction.values()[new Random().nextInt(Direction.values().length)]);
        this.width = width;
        this.height = height;
        this.speed = speed;
        this.reloadInterval = reloadInterval;
        this.bulletRadius = bulletRadius;
        this.bulletSpeed = bulletSpeed;
        this.bulletLife = bulletLife;
        this.color = color;
        this.imageIds = imageIds;
        countIterationFromLastFire = reloadInterval;
        // rnd = new Random();

        Point leftUpperPointOfRandomFreeSector = worldMap.convertSectorIdToPoint(worldMap.getRandomFreeSector());
        setShape(new Rectangle2D.Double(leftUpperPointOfRandomFreeSector.x, leftUpperPointOfRandomFreeSector.y, width, height));
    }

    @Override
    public void process() {
        if (needMove())
            move();

        if (needFire())
            fire();

        countIterationFromLastFire++;
    }

    abstract protected boolean needMove();

    abstract protected boolean needFire();

    private void move() {
        int dX = 0;
        int dY = 0;
        switch (getDirection()) {
            case LEFT:
                dX = -1 * speed;
                dY = 0;
                break;
            case UP:
                dX = 0;
                dY = -1 * speed;
                break;
            case RIGHT:
                dX = speed;
                dY = 0;
                break;
            case DOWN:
                dX = 0;
                dY = speed;
                break;
        }

        move(dX, dY);
    }

    private void fire() {
        if (reloadInterval > countIterationFromLastFire)
            return;

        Rectangle bounds = getShape().getBounds();
        Point first = new Point(bounds.x, bounds.y);

        worldMap.add(new Bullet(
                worldMap,
                (int) (first.getX() + width / 2),
                (int) (first.getY() + height / 2),
                bulletLife,
                this,
                bulletRadius,
                bulletSpeed
        ));

        countIterationFromLastFire = 0;
    }

    @Override
    public boolean mayIntersect(WorldObject object) {
        return object != null && this.getClass().isAssignableFrom(object.getClass());
    }

    @Override
    public void draw(ExecutionContextTool executionContextTool) {
        if (imageIds != null) {
            executionContextTool.addMessage(7);
            Direction direction = getDirection();
            executionContextTool.addMessage(imageIds.get(ArrayUtils.indexOf(Direction.values(), direction != null ? direction : Direction.DOWN)));
        } else {
            executionContextTool.addMessage(1);
            executionContextTool.addMessage(1);
            executionContextTool.addMessage(color/*new Color(0, 0, 0).getRGB()*/);
            executionContextTool.addMessage(1);
        }
        Rectangle bounds = getShape().getBounds();
        executionContextTool.addMessage(bounds.x);
        executionContextTool.addMessage(bounds.y);
        executionContextTool.addMessage(bounds.width);
        executionContextTool.addMessage(bounds.height);
    }

    @Override
    public boolean isTransparent() {
        return true;
    }

}
