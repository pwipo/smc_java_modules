package ru.smcsystem.smcmodules.module;

import org.apache.commons.lang3.math.NumberUtils;
import ru.smcsystem.api.enumeration.ValueType;
import ru.smcsystem.api.exceptions.ModuleException;
import ru.smcsystem.api.module.Module;
import ru.smcsystem.api.tools.ConfigurationTool;
import ru.smcsystem.api.tools.execution.ExecutionContextTool;
import ru.smcsystem.smc.utils.ModuleUtils;
import ru.smcsystem.smcmodules.module.world.Direction;
import ru.smcsystem.smcmodules.module.world.World;
import ru.smcsystem.smcmodules.module.world.WorldImpl;
import ru.smcsystem.smcmodules.module.world.objects.Bot;
import ru.smcsystem.smcmodules.module.world.objects.Player;
import ru.smcsystem.smcmodules.module.world.objects.Wall;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GameWarOfRobots implements Module {

    private enum KYE_CODE {
        LEFT(37),
        UP(38),
        RIGHT(39),
        DOWN(40),
        SPACE(32);
        // ENTER(10);

        private int code;

        KYE_CODE(int code) {
            this.code = code;
        }

        static public Optional<KYE_CODE> parse(int code) {
            return Arrays.stream(KYE_CODE.values()).filter(element -> element.code == code).findAny();
        }
    }

    private int robotLife;
    private int robotWidth;
    private int robotHeight;
    private int robotSpeed;
    private int robotReloadInterval;
    private int robotBulletLife;
    private int robotCount;
    private int robotCountVisible;
    private int robotDefaultColor;
    private List<Integer> robotImageIds;

    private int bulletRadius;
    private int bulletSpeed;

    private int robotBulletSpeed;
    private int robotLevelOffset;

    private World world;

    private Player player;
    private List<Bot> bots;

    private int countCreatedRobots;

    private int score = 0;

    private int level = 1;

    @Override
    public void start(ConfigurationTool configurationTool) throws ModuleException {
        int barrierWallCount = (Integer) configurationTool.getSetting("barrierWallCount").orElseThrow(() -> new ModuleException("barrierWallCount setting")).getValue();
        int barrierWallSideLength = (Integer) configurationTool.getSetting("barrierWallSideLength").orElseThrow(() -> new ModuleException("barrierWallSideLength setting")).getValue();
        int barrierWallLife = (Integer) configurationTool.getSetting("barrierWallLife").orElseThrow(() -> new ModuleException("barrierWallLife setting")).getValue();
        int barrierDefaultColor = (Integer) configurationTool.getSetting("barrierDefaultColor").orElseThrow(() -> new ModuleException("barrierDefaultColor setting")).getValue();
        int barrierImageId = (Integer) configurationTool.getSetting("barrierImageId")
                // .map(m -> !((String) m.getValue()).isBlank() ? NumberUtils.toInt((String) m.getValue()) : null)
                .orElseThrow(() -> new ModuleException("barrierImageId setting")).getValue();

        int playerLife = (Integer) configurationTool.getSetting("playerLife").orElseThrow(() -> new ModuleException("playerLife setting")).getValue();
        int playerSpeed = (Integer) configurationTool.getSetting("playerSpeed").orElseThrow(() -> new ModuleException("playerSpeed setting")).getValue();
        int playerReloadInterval = (Integer) configurationTool.getSetting("playerReloadInterval").orElseThrow(() -> new ModuleException("playerReloadInterval setting")).getValue();
        int playerBulletLife = (Integer) configurationTool.getSetting("playerBulletLife").orElseThrow(() -> new ModuleException("playerBulletLife setting")).getValue();
        int playerWidth = (Integer) configurationTool.getSetting("playerWidth").orElseThrow(() -> new ModuleException("playerWidth setting")).getValue();
        int playerHeight = (Integer) configurationTool.getSetting("playerHeight").orElseThrow(() -> new ModuleException("playerHeight setting")).getValue();
        int playerDefaultColor = (Integer) configurationTool.getSetting("playerDefaultColor").orElseThrow(() -> new ModuleException("playerDefaultColor setting")).getValue();
        String tmpPlayerImageIds = (String) configurationTool.getSetting("playerImageIds").orElseThrow(() -> new ModuleException("playerImageIds setting")).getValue();
        List<Integer> playerImageIds = null;
        if (!tmpPlayerImageIds.isBlank()) {
            playerImageIds = Arrays.stream(tmpPlayerImageIds.split(","))
                    .map(String::trim)
                    .map(NumberUtils::toInt)
                    .collect(Collectors.toList());
        }

        robotLife = (Integer) configurationTool.getSetting("robotLife").orElseThrow(() -> new ModuleException("robotLife setting")).getValue();
        robotWidth = (Integer) configurationTool.getSetting("robotWidth").orElseThrow(() -> new ModuleException("robotWidth setting")).getValue();
        robotHeight = (Integer) configurationTool.getSetting("robotHeight").orElseThrow(() -> new ModuleException("robotHeight setting")).getValue();
        robotSpeed = (Integer) configurationTool.getSetting("robotSpeed").orElseThrow(() -> new ModuleException("robotSpeed setting")).getValue();
        robotReloadInterval = (Integer) configurationTool.getSetting("robotReloadInterval").orElseThrow(() -> new ModuleException("robotReloadInterval setting")).getValue();
        robotBulletLife = (Integer) configurationTool.getSetting("robotBulletLife").orElseThrow(() -> new ModuleException("robotBulletLife setting")).getValue();
        robotCount = (Integer) configurationTool.getSetting("robotCount").orElseThrow(() -> new ModuleException("robotCount setting")).getValue();
        robotCountVisible = (Integer) configurationTool.getSetting("robotCountVisible").orElseThrow(() -> new ModuleException("robotCountVisible setting")).getValue();
        robotDefaultColor = (Integer) configurationTool.getSetting("robotDefaultColor").orElseThrow(() -> new ModuleException("robotDefaultColor setting")).getValue();
        String tmpRobotImageIds = (String) configurationTool.getSetting("robotImageIds").orElseThrow(() -> new ModuleException("robotImageIds setting")).getValue();
        robotImageIds = null;
        if (!tmpRobotImageIds.isBlank()) {
            robotImageIds = Arrays.stream(tmpRobotImageIds.split(","))
                    .map(String::trim)
                    .map(NumberUtils::toInt)
                    .collect(Collectors.toList());
        }

        bulletRadius = (Integer) configurationTool.getSetting("bulletRadius").orElseThrow(() -> new ModuleException("bulletRadius setting")).getValue();
        bulletSpeed = (Integer) configurationTool.getSetting("bulletSpeed").orElseThrow(() -> new ModuleException("bulletSpeed setting")).getValue();

        robotLevelOffset = (Integer) configurationTool.getSetting("robotLevelOffset").orElseThrow(() -> new ModuleException("robotLevelOffset setting")).getValue();
        robotBulletSpeed = (Integer) configurationTool.getSetting("robotBulletSpeed").orElseThrow(() -> new ModuleException("robotBulletSpeed setting")).getValue();

        int backgroundColor = (Integer) configurationTool.getSetting("backgroundColor").orElseThrow(() -> new ModuleException("backgroundColor setting")).getValue();
        int mapWidth = (Integer) configurationTool.getSetting("mapWidth").orElseThrow(() -> new ModuleException("mapWidth setting")).getValue();
        int mapHeight = (Integer) configurationTool.getSetting("mapHeight").orElseThrow(() -> new ModuleException("mapHeight setting")).getValue();
        int worldMapSectorWidth = (Integer) configurationTool.getSetting("mapSectorWidth").orElseThrow(() -> new ModuleException("worldMapSectorWidth setting")).getValue();
        int worldMapSectorHeight = (Integer) configurationTool.getSetting("mapSectorHeight").orElseThrow(() -> new ModuleException("worldMapSectorHeight setting")).getValue();

        robotBulletSpeed = robotBulletSpeed + (level - 1) * robotLevelOffset;
        robotSpeed = robotSpeed + (level - 1) * robotLevelOffset;

        world = new WorldImpl(mapWidth, mapHeight, mapWidth, mapHeight, worldMapSectorWidth, worldMapSectorHeight, backgroundColor);

        // add objects to world
        for (int i = 0; i < barrierWallCount; i++)
            new Wall(world, barrierWallLife, barrierWallSideLength, barrierWallSideLength, barrierDefaultColor, barrierImageId >= 0 ? barrierImageId : null);

        countCreatedRobots = 0;
        bots = new ArrayList<>();
        while (bots.size() < robotCountVisible && countCreatedRobots < robotCount) {
            bots.add(new Bot(world, robotLife, robotWidth, robotHeight, robotSpeed, robotReloadInterval, bulletRadius, robotBulletSpeed, robotBulletLife, robotDefaultColor/*new Color(0, 0, 0).getRGB()*/, robotImageIds));
            countCreatedRobots++;
        }

        player = new Player(world, playerLife, playerWidth, playerHeight, playerSpeed, playerReloadInterval, bulletRadius, bulletSpeed, playerBulletLife, playerDefaultColor, playerImageIds);
    }

    @Override
    public void update(ConfigurationTool configurationTool) throws ModuleException {
        stop(configurationTool);
        start(configurationTool);
    }

    @Override
    public void process(ConfigurationTool configurationTool, ExecutionContextTool executionContextTool) throws ModuleException {
        Stream.iterate(0, n -> n + 1)
                .limit(executionContextTool.countSource())
                .flatMap(i -> executionContextTool.getMessages(i).stream())
                .forEach(a -> {
                    doPlayerActions(a.getMessages().stream()
                            .filter(ModuleUtils::isNumber)
                            .map(ModuleUtils::getNumber)
                            .map(n -> KYE_CODE.parse(n.intValue()))
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .collect(Collectors.toList())
                    );
                });

        //add bots
        List<Bot> newList = bots.stream()
                .filter(r -> r.getLife() > 0)
                .collect(Collectors.toList());
        if (newList.size() != bots.size())
            score += Math.abs(bots.size() - newList.size());
        bots = newList;
        while (bots.size() < robotCountVisible && countCreatedRobots < robotCount) {
            try {
                bots.add(new Bot(world, robotLife, robotWidth, robotHeight, robotSpeed, robotReloadInterval, bulletRadius, bulletSpeed, robotBulletLife, robotDefaultColor, robotImageIds));
                countCreatedRobots++;
            } catch (Exception e) {
                executionContextTool.addError(ModuleUtils.getErrorMessageOrClassName(e));
                configurationTool.loggerWarn(ModuleUtils.getStackTraceAsString(e));
            }
        }

        //main cycle
        world.process(configurationTool, executionContextTool);
        world.draw(executionContextTool);

        executionContextTool.addMessage(2);
        executionContextTool.addMessage(new Color(0, 0, 0).getRGB());
        executionContextTool.addMessage(5);
        executionContextTool.addMessage(20);
        executionContextTool.addMessage(String.format("level: %d, score: %d", level, score));
        executionContextTool.addMessage("SansSerif");
        executionContextTool.addMessage(20);

        //check for restart
        if (player.getLife() <= 0) {
            level = 1;
            score = 0;
            update(configurationTool);
        } else if (bots.isEmpty()) {
            level++;
            update(configurationTool);
        }

    }

    @Override
    public void stop(ConfigurationTool configurationTool) throws ModuleException {
        player = null;
        world = null;
    }

    private void doPlayerActions(List<KYE_CODE> values) {
        boolean doDirection = false;
        for (KYE_CODE key : values) {
            switch (key) {
                case LEFT:
                    if (doDirection)
                        continue;
                    player.changeCoordinates(Direction.valueOf(key.name()));
                    doDirection = true;
                    break;
                case UP:
                    if (doDirection)
                        continue;
                    player.changeCoordinates(Direction.valueOf(key.name()));
                    doDirection = true;
                    break;
                case RIGHT:
                    if (doDirection)
                        continue;
                    player.changeCoordinates(Direction.valueOf(key.name()));
                    doDirection = true;
                    break;
                case DOWN:
                    if (doDirection)
                        continue;
                    player.changeCoordinates(Direction.valueOf(key.name()));
                    doDirection = true;
                    break;
                case SPACE:
                    player.doFire();
                    break;
            }
        }
    }


}
