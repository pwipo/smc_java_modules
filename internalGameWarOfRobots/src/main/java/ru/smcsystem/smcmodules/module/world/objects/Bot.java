package ru.smcsystem.smcmodules.module.world.objects;

import ru.smcsystem.smcmodules.module.world.Direction;
import ru.smcsystem.smcmodules.module.world.World;
import ru.smcsystem.smcmodules.module.world.WorldObject;
import ru.seits.utils.fsm.FiniteStateMachine;
import ru.seits.utils.fsm.FiniteStateMachineImpl;
import ru.seits.utils.fsm.TransitionImpl;

import java.util.*;

public class Bot extends Robot {

    private FiniteStateMachine finiteStateMachine;
    private boolean fire;
    // private boolean move;
    private LinkedList<Integer> path;

    private int common_counter;
    private final Random rnd;

    public Bot(World worldMap, int life, int width, int height, int speed, int reloadInterval, int bulletRadius, int bulletSpeed, int bulletLife, int color, java.util.List<Integer> imageIds) {
        super(worldMap, 100, life, width, height, speed, reloadInterval, bulletRadius, bulletSpeed, bulletLife, color, imageIds);
        fire = false;
        // move = false;
        path = new LinkedList<>();

        rnd = new Random();
        common_counter = 0;

        finiteStateMachine = new FiniteStateMachineImpl(
                "init",
                Set.of("end"),
                List.of(
                        new TransitionImpl(
                                null,
                                "init",
                                "ShotAndRun",
                                event -> {
                                    fire = true;
                                    path.clear();
                                    return true;
                                },
                                "findShelter", null,
                                "end", null
                                , null
                        ),
                        new TransitionImpl(
                                null,
                                "init",
                                (String) "RandomShotAndSleep",
                                event -> {
                                    setDirection(Direction.values()[rnd.nextInt(Direction.values().length)]);
                                    fire = true;
                                    common_counter = 0;
                                    path.clear();
                                    return true;
                                },
                                "sleep", null,
                                "end", null
                                , null
                        ),
                        new TransitionImpl(
                                null,
                                "init",
                                "WanderAround",
                                event -> {
                                    fire = false;
                                    path.clear();
                                    path.addAll(worldMap.getSectorsPathTo(this, worldMap.getRandomFreeSector()));
                                    if (!path.isEmpty())
                                        this.setDirection(worldMap.getDirection(this, path.peek()));
                                    if (path.size() > 4)
                                        path = new LinkedList<>(path.subList(0, 4));
                                    return true;
                                },
                                "walk", null,
                                "end", null
                                , null
                        ),
                        new TransitionImpl(
                                null,
                                "findShelter",
                                (String) null,
                                event -> {
                                    List<Direction> directions = null;
                                    switch (getDirection()) {
                                        case RIGHT:
                                        case LEFT:
                                            directions = List.of(Direction.UP, Direction.DOWN);
                                            break;
                                        case DOWN:
                                        case UP:
                                            directions = List.of(Direction.LEFT, Direction.RIGHT);
                                            break;
                                    }
                                    Direction oldDirection = getDirection();
                                    List<Integer> sectors = worldMap.findSectors(this);
                                    for (int j = 0; j < directions.size(); j++) {
                                        this.setDirection(directions.get(j));
                                        Optional<Integer> first = sectors.stream()
                                                .map(id -> worldMap.getNextSector(id, getDirection()))
                                                .filter(Objects::nonNull)
                                                .findFirst();
                                        first.ifPresent(id -> {
                                            if (worldMap.isAvailable(first.get(), this))
                                                path.add(first.get());
                                        });
                                        if (!path.isEmpty())
                                            break;
                                    }
                                    if (path.isEmpty()) {
                                        // иду на вы!
                                        this.setDirection(oldDirection);
                                        sectors.stream()
                                                .map(id -> worldMap.getNextSector(id, getDirection()))
                                                .filter(Objects::nonNull)
                                                .findFirst().ifPresent(path::add);
                                    }
                                    return !path.isEmpty();
                                },
                                "walk", null,
                                "end", null
                                , null
                        ),
                        new TransitionImpl(
                                null,
                                "walk",
                                (String) null,
                                event -> !path.isEmpty(),
                                "walk", null,
                                "end", null
                                , null
                        ),
                        new TransitionImpl(
                                null,
                                "sleep",
                                (String) null,
                                event -> common_counter++ < 10,
                                "sleep", null,
                                "end", null
                                , null
                        )
                )
        );
        finiteStateMachine.setCurrentState("end");

    }

    @Override
    protected boolean needMove() {
        ai();

        if (path.isEmpty())
            return false;
        List<Integer> sectors = worldMap.findSectors(this);
        if (sectors.size() == 1 && sectors.get(0).equals(path.peek())) {
            path.poll();
            if (!path.isEmpty()) {
                this.setDirection(worldMap.getDirection(this, path.peek()));
                //wrong path!!!
                if (getDirection() == null)
                    path.clear();
            }
        }
        //на случай наличия неожиданных препятствий
        if (!path.isEmpty() && !worldMap.isAvailable(path.peek(), this))
            path.clear();
        return !path.isEmpty();
    }

    @Override
    protected boolean needFire() {
        boolean result = fire;
        fire = false;
        return result;
    }

    private void ai() {
        Object event = null;
        if (finiteStateMachine.getFinalStates().contains(finiteStateMachine.getCurrentState())) {
            // если есть враг в поле видимости использовать стратегему - стрельнуть и бежать
            for (int i = 0; i < Direction.values().length; i++) {
                this.setDirection(Direction.values()[i]);
                List<WorldObject> visibleObjects = worldMap.getVisibleObjects(this);
                if (visibleObjects.stream().anyMatch(wo -> wo instanceof Player)) {
                    event = "ShotAndRun";
                    break;
                }
            }
            // рандомно выбрать цель и следовать до него
            if (event == null) {
                if (rnd.nextBoolean()) {
                    event = "RandomShotAndSleep";
                } else {
                    event = "WanderAround";
                }
            }
            finiteStateMachine.setCurrentState("init");
        }
        finiteStateMachine.fire(event);
    }


}
