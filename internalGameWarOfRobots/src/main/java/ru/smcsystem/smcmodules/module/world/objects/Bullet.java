package ru.smcsystem.smcmodules.module.world.objects;

import ru.smcsystem.api.tools.execution.ExecutionContextTool;
import ru.smcsystem.smcmodules.module.world.World;
import ru.smcsystem.smcmodules.module.world.WorldObject;
import ru.smcsystem.smcmodules.module.world.WorldObjectImpl;

import java.awt.*;
import java.awt.geom.Ellipse2D;

public class Bullet extends WorldObjectImpl {

    private final WorldObject owner;
    private final int radius;
    private final int speed;

    public Bullet(World worldMap, int x, int y, int life, WorldObject owner, int radius, int speed) {
        super(worldMap, 200, new Ellipse2D.Double(x-(radius/2), y-(radius/2), radius, radius), life, owner.getDirection());
        this.owner = owner;
        this.radius = radius;
        this.speed = speed;
    }

    @Override
    public void process() {
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
        if (!move(dX, dY)) {
            damage(null, getLife());
            return;
        }
        worldMap.findIntersects(this).stream()
                .filter(wo -> !wo.equals(owner))
                .filter(wo -> wo.getLife() > 0)
                .forEach(wo -> damage(null, wo.damage(this, life)));
    }

    @Override
    public boolean mayIntersect(WorldObject object) {
        return true;
    }

    @Override
    public void draw(ExecutionContextTool executionContextTool) {
        executionContextTool.addMessage(5);
        executionContextTool.addMessage(1);
        executionContextTool.addMessage(new Color(255, 0, 0).getRGB());
        executionContextTool.addMessage(1);
        Rectangle bounds = getShape().getBounds();
        executionContextTool.addMessage(bounds.x + bounds.width / 2);
        executionContextTool.addMessage(bounds.y + bounds.height / 2);
        executionContextTool.addMessage(radius);
        executionContextTool.addMessage(radius);
    }

    @Override
    public boolean isTransparent() {
        return true;
    }

}
