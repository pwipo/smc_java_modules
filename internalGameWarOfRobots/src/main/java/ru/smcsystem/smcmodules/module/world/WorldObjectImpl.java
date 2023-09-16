package ru.smcsystem.smcmodules.module.world;

import ru.smcsystem.api.tools.execution.ExecutionContextTool;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.Objects;

abstract public class WorldObjectImpl implements WorldObject {

    protected final World worldMap;

    private Shape shape;
    protected int life;
    private final Integer order;
    private Direction direction;

    protected WorldObjectImpl(World worldMap, Integer order, Shape shape, int life, Direction direction) {
        Objects.requireNonNull(worldMap);
        this.worldMap = worldMap;
        this.order = order;
        this.life = life;
        this.direction = direction;
        setShape(shape);
    }

    /*
    public List<List<Point>> isNeedRepaint() {
        List<List<Point>> result = null;
        if (prevPoints != null) {
            result = List.of(prevPoints, points);
            prevPoints = null;
        }
        return result;
    }

    */

    protected boolean move(int dx, int dy) {
        // Objects.requireNonNull(points);
        AffineTransform at = new AffineTransform();
        at.translate(dx, dy);
        Shape transformedShape = at.createTransformedShape(shape);
        /*
        Polygon newBodyPosition = new Polygon(
                Arrays.stream(bodyPosition.xpoints).map(x -> x + dx).toArray(),
                Arrays.stream(bodyPosition.ypoints).map(x -> x + dy).toArray(),
                bodyPosition.npoints
        );
        // List<Point> tmp = new ArrayList<>(this.points);
        */
        if (worldMap.change(this, transformedShape)) {
            shape = transformedShape;
            return true;
        }
        return false;
    }

    @Override
    public int damage(WorldObject object, int damage) {
        damage = Math.abs(damage);
        int newLife = life - damage;
        int result = damage;
        if (newLife < 0) {
            result = damage + newLife;
            newLife = 0;
        }
        life = newLife;
        return Math.abs(result);
    }

    @Override
    public int getLife() {
        return life;
    }

    @Override
    public Shape getShape() {
        return shape;
    }

    protected void setShape(Shape shape) {
        this.shape = shape;
        if (shape != null && !worldMap.add(this))
            throw new RuntimeException("error while adding object to world");
    }

    @Override
    public boolean mayIntersect(WorldObject object) {
        return false;
    }

    @Override
    public boolean isTransparent() {
        return false;
    }

    @Override
    public Integer getOrder() {
        return order;
    }

    @Override
    public Direction getDirection() {
        return direction;
    }

    protected void setDirection(Direction direction) {
        this.direction = direction;
    }

    @Override
    public void process() {
    }

    @Override
    public void draw(ExecutionContextTool executionContextTool) {
        executionContextTool.addMessage(1);
        executionContextTool.addMessage(1);
        executionContextTool.addMessage(new Color(0, 0, 0).getRGB());
        executionContextTool.addMessage(1);
        Rectangle bounds = shape.getBounds();
        executionContextTool.addMessage(bounds.x);
        executionContextTool.addMessage(bounds.y);
        executionContextTool.addMessage(bounds.width);
        executionContextTool.addMessage(bounds.height);
    }

}
