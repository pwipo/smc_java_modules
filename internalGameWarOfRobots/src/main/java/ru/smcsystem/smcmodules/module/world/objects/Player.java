package ru.smcsystem.smcmodules.module.world.objects;

import ru.smcsystem.smcmodules.module.world.Direction;
import ru.smcsystem.smcmodules.module.world.World;

import java.util.Objects;

public class Player extends Robot {

    private boolean move;
    private boolean fire;

    public Player(World worldMap, int life, int width, int height, int speed, int reloadInterval, int bulletRadius, int bulletSpeed, int bulletLife, int color, java.util.List<Integer> imageIds) {
        super(worldMap, 10, life, width, height, speed, reloadInterval, bulletRadius, bulletSpeed, bulletLife, color, imageIds);
    }

    public void changeCoordinates(Direction direction) {
        Objects.requireNonNull(direction);
        this.setDirection(direction);
        move = true;
    }

    public void doFire() {
        fire = true;
    }

    @Override
    protected boolean needMove() {
        boolean result = move;
        move = false;
        return result;
    }

    @Override
    protected boolean needFire() {
        boolean result = fire;
        fire = false;
        return result;
    }

}
