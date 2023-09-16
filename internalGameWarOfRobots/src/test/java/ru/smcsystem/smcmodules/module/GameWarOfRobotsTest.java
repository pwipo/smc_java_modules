package ru.smcsystem.smcmodules.module;

import org.junit.Test;
import ru.smcsystem.api.enumeration.ActionType;
import ru.smcsystem.api.enumeration.MessageType;
import ru.smcsystem.test.Process;
import ru.smcsystem.test.emulate.*;

import java.awt.*;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameWarOfRobotsTest {

    @Test
    public void process() {
        Map map = new HashMap();
        map.putAll(Map.of(
                "backgroundColor", new Value(new Color(0, 255, 0).getRGB())
                , "mapWidth", new Value(12)
                , "mapHeight", new Value(10)
                , "mapSectorWidth", new Value(100)
                , "mapSectorHeight", new Value(100)
        ));
        map.putAll(Map.of(
                "barrierWallCount", new Value(30)
                , "barrierWallSideLength", new Value(80)
                , "barrierWallLife", new Value(5)
                , "barrierDefaultColor", new Value(new Color(0, 0, 255).getRGB())
                , "barrierImageId", new Value(-1)
        ));
        map.putAll(Map.of(
                "playerLife", new Value(5)
                , "playerSpeed", new Value(10)
                , "playerReloadInterval", new Value(5)
                , "playerBulletLife", new Value(1)
                , "playerWidth", new Value(50)
                , "playerHeight", new Value(50)
                , "playerDefaultColor", new Value(new Color(255, 0, 0).getRGB())
                , "playerImageIds", new Value("1,2,3,4")
        ));
        map.putAll(Map.of(
                "robotLife", new Value(3)
                , "robotWidth", new Value(50)
                , "robotHeight", new Value(50)
                , "robotSpeed", new Value(10)
                , "robotReloadInterval", new Value(5)
                , "robotBulletLife", new Value(1)
                , "robotCount", new Value(100)
                , "robotCountVisible", new Value(2)
                , "robotDefaultColor", new Value(new Color(0, 0, 0).getRGB())
                , "robotImageIds", new Value("5,6,7,8")
        ));
        map.putAll(Map.of(
                "bulletRadius", new Value(10)
                , "bulletSpeed", new Value(20)
                , "robotLevelOffset", new Value(5)
                , "robotBulletSpeed", new Value(20)
        ));
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        map,
                        null,
                        null
                ),
                new GameWarOfRobots()
        );
        process.start();

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(null,
                null,
                null);
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().clear();

        executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(40))
                                                , new Message(MessageType.DATA, new Date(), new Value(32))
                                        ),
                                        ActionType.EXECUTE
                                ))),
                null,
                null
        );
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().clear();

        executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(39))
                                                , new Message(MessageType.DATA, new Date(), new Value(32))
                                        ),
                                        ActionType.EXECUTE
                                ))),
                null,
                null
        );
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().clear();

        executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(38))
                                                , new Message(MessageType.DATA, new Date(), new Value(32))
                                        ),
                                        ActionType.EXECUTE
                                ))),
                null,
                null
        );
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().clear();

        executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(37))
                                                , new Message(MessageType.DATA, new Date(), new Value(32))
                                        ),
                                        ActionType.EXECUTE
                                ))),
                null,
                null
        );
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().clear();

        process.stop();
    }
}