package ru.smcsystem.smcmodules.module;

import org.junit.Test;
import ru.smcsystem.api.dto.ObjectArray;
import ru.smcsystem.api.dto.ObjectElement;
import ru.smcsystem.api.dto.ObjectField;
import ru.smcsystem.api.enumeration.ActionType;
import ru.smcsystem.api.enumeration.MessageType;
import ru.smcsystem.api.enumeration.ObjectType;
import ru.smcsystem.test.Process;
import ru.smcsystem.test.emulate.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class IteratorTest {

    @Test
    public void process() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "updateCacheStrategy", new Value("FORCE")
                                , "maxCacheSize", new Value(0)
                                , "incrementValue", new Value(1)
                                , "outputSize", new Value(2)
                        ),
                        null,
                        null
                ),
                new Iterator()
        );
        process.start();

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(1))
                                                , new Message(MessageType.DATA, new Date(), new Value(2))
                                                , new Message(MessageType.DATA, new Date(), new Value(3))
                                                , new Message(MessageType.DATA, new Date(), new Value(4))
                                                , new Message(MessageType.DATA, new Date(), new Value(5))
                                                , new Message(MessageType.DATA, new Date(), new Value(6))
                                                , new Message(MessageType.DATA, new Date(), new Value(7))
                                                , new Message(MessageType.DATA, new Date(), new Value(8))
                                                , new Message(MessageType.DATA, new Date(), new Value(9))
                                                , new Message(MessageType.DATA, new Date(), new Value(10))
                                        ),
                                        ActionType.EXECUTE
                                ))),
                null,
                null);
        ExecutionContextToolImpl executionContextTool2 = new ExecutionContextToolImpl(null,
                null,
                null);

        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().clear();

        do {
            executionContextTool2.getOutput().clear();
            process.execute(executionContextTool2);
            executionContextTool2.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        } while (!executionContextTool2.getOutput().isEmpty());

        process.getConfigurationTool().getAllSettings().put("updateCacheStrategy", new Value("ADD"));
        process.getConfigurationTool().getAllSettings().put("maxCacheSize", new Value(16));
        process.update();

        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().clear();

        executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(11))
                                                , new Message(MessageType.DATA, new Date(), new Value(12))
                                                , new Message(MessageType.DATA, new Date(), new Value(13))
                                                , new Message(MessageType.DATA, new Date(), new Value(14))
                                                , new Message(MessageType.DATA, new Date(), new Value(15))
                                                , new Message(MessageType.DATA, new Date(), new Value(16))
                                                , new Message(MessageType.DATA, new Date(), new Value(17))
                                                , new Message(MessageType.DATA, new Date(), new Value(18))
                                                , new Message(MessageType.DATA, new Date(), new Value(19))
                                                , new Message(MessageType.DATA, new Date(), new Value(20))
                                        ),
                                        ActionType.EXECUTE
                                ))),
                null,
                null);

        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().clear();

        do {
            executionContextTool2.getOutput().clear();
            process.execute(executionContextTool2);
            executionContextTool2.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        } while (!executionContextTool2.getOutput().isEmpty());

        process.stop();
    }

    @Test
    public void process2() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "updateCacheStrategy", new Value("FORCE")
                                , "maxCacheSize", new Value(0)
                                , "incrementValue", new Value(1)
                                , "outputSize", new Value(2)
                        ),
                        null,
                        null
                ),
                new Iterator()
        );
        process.start();

        ExecutionContextToolImpl executionContextToolAdd = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(1))
                                                , new Message(MessageType.DATA, new Date(), new Value(2))
                                                , new Message(MessageType.DATA, new Date(), new Value(3))
                                                , new Message(MessageType.DATA, new Date(), new Value(4))
                                                , new Message(MessageType.DATA, new Date(), new Value(5))
                                                , new Message(MessageType.DATA, new Date(), new Value(6))
                                                , new Message(MessageType.DATA, new Date(), new Value(7))
                                                , new Message(MessageType.DATA, new Date(), new Value(8))
                                                , new Message(MessageType.DATA, new Date(), new Value(9))
                                                , new Message(MessageType.DATA, new Date(), new Value(10))
                                        ),
                                        ActionType.EXECUTE
                                ))),
                null, null, null, "add", "add");
        ExecutionContextToolImpl executionContextToolGet = new ExecutionContextToolImpl(null, null, null, null, "get", "get");

        System.out.println("add");
        process.execute(executionContextToolAdd);
        executionContextToolAdd.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextToolAdd.getOutput().clear();

        System.out.println("get");
        do {
            System.out.println("get call");
            executionContextToolGet.getOutput().clear();
            process.execute(executionContextToolGet);
            executionContextToolGet.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        } while (!executionContextToolGet.getOutput().isEmpty());

        process.getConfigurationTool().getAllSettings().put("updateCacheStrategy", new Value("ADD"));
        process.getConfigurationTool().getAllSettings().put("maxCacheSize", new Value(16));
        process.update();

        System.out.println("add");
        process.execute(executionContextToolAdd);
        executionContextToolAdd.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextToolAdd.getOutput().clear();

        executionContextToolAdd = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(11))
                                                , new Message(MessageType.DATA, new Date(), new Value(12))
                                                , new Message(MessageType.DATA, new Date(), new Value(13))
                                                , new Message(MessageType.DATA, new Date(), new Value(14))
                                                , new Message(MessageType.DATA, new Date(), new Value(15))
                                                , new Message(MessageType.DATA, new Date(), new Value(16))
                                                , new Message(MessageType.DATA, new Date(), new Value(17))
                                                , new Message(MessageType.DATA, new Date(), new Value(18))
                                                , new Message(MessageType.DATA, new Date(), new Value(19))
                                                , new Message(MessageType.DATA, new Date(), new Value(20))
                                        ),
                                        ActionType.EXECUTE
                                ))),
                null, null, null, "add", "add");

        System.out.println("add new");
        process.execute(executionContextToolAdd);
        executionContextToolAdd.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextToolAdd.getOutput().clear();

        ExecutionContextToolImpl executionContextToolGetAll = new ExecutionContextToolImpl(null, null, null, null, "getAll", "get_all");

        System.out.println("get all");
        process.execute(executionContextToolGetAll);
        executionContextToolGetAll.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextToolGetAll.getOutput().clear();

        ExecutionContextToolImpl executionContextToolUpdate = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(new Value(new ObjectArray(new ObjectElement(
                                                        new ObjectField("maxSize", 15),
                                                        new ObjectField("updateSize", 10),
                                                        new ObjectField("startValues", new ObjectArray(List.of(1, 2, 3), ObjectType.VALUE_ANY)),
                                                        new ObjectField("maxSize", new ObjectArray(List.of(19, 20), ObjectType.VALUE_ANY))
                                                ))))
                                        ),
                                        ActionType.EXECUTE
                                ))), null, null, null, "update", "update");

        System.out.println("update");
        process.execute(executionContextToolUpdate);
        executionContextToolUpdate.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextToolUpdate.getOutput().clear();

        System.out.println("get all");
        process.execute(executionContextToolGetAll);
        executionContextToolGetAll.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextToolGetAll.getOutput().clear();

        System.out.println("update");
        process.execute(executionContextToolUpdate);
        executionContextToolUpdate.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextToolUpdate.getOutput().clear();

        System.out.println("get all");
        process.execute(executionContextToolGetAll);
        executionContextToolGetAll.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextToolGetAll.getOutput().clear();

        System.out.println("get");
        do {
            System.out.println("get call");
            executionContextToolGet.getOutput().clear();
            process.execute(executionContextToolGet);
            executionContextToolGet.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        } while (!executionContextToolGet.getOutput().isEmpty());

        ExecutionContextToolImpl executionContextToolClean = new ExecutionContextToolImpl(null, null, null, null, "clean", "clean");

        System.out.println("clean");
        process.execute(executionContextToolClean);
        executionContextToolClean.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextToolClean.getOutput().clear();

        process.stop();
    }
}