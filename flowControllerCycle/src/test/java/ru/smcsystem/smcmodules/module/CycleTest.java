package ru.smcsystem.smcmodules.module;

import org.junit.Test;
import ru.smcsystem.api.dto.IMessage;
import ru.smcsystem.api.enumeration.ActionType;
import ru.smcsystem.api.enumeration.MessageType;
import ru.smcsystem.api.enumeration.ValueType;
import ru.smcsystem.test.Process;
import ru.smcsystem.test.emulate.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class CycleTest {

    @Test
    public void process() throws InterruptedException {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "type", new Value(ValueType.STRING, "WHILE")
                                , "idForBreak", new Value(ValueType.STRING, " ")
                                , "idForContinue", new Value(ValueType.STRING, "1")
                                , "checkType", new Value(ValueType.STRING, "NO_DATA")
                                , "forCount", new Value(ValueType.INTEGER, 10)
                                , "sleepTime", new Value(ValueType.INTEGER, 0)
                                , "sleepTimeType", new Value("FIXED")
                        ),
                        null,
                        null
                ),
                new Cycle()
        );
        process.start();

        List<ExecutionContextToolImpl> arrayList = new ArrayList<>();

        Action action = new Action(
                List.of(new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 0))),
                ActionType.EXECUTE);
        Thread thread = new Thread(() -> {
            ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                    null,
                    null,
                    List.of(
                            action,
                            new Action(
                                    List.of(new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 2))),
                                    ActionType.EXECUTE),
                            new Action(
                                    List.of(new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 3))),
                                    ActionType.EXECUTE)
                    )
            );
            arrayList.add(executionContextTool);
            process.execute(executionContextTool);
        });
        thread.start();
        List<IMessage> output;
        while (arrayList.isEmpty())
            Thread.sleep(1);
        output = arrayList.get(0).getOutput();
        while (output.size() < 2)
            Thread.sleep(1);
        action.getMessages().clear();
        thread.join();
        output.forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        process.stop();
    }

    @Test
    public void process2() throws InterruptedException {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "type", new Value(ValueType.STRING, "FOR_EACH")
                                , "idForBreak", new Value(ValueType.STRING, " ")
                                , "idForContinue", new Value(ValueType.STRING, " ")
                                , "checkType", new Value(ValueType.STRING, "NO_DATA")
                                , "forCount", new Value(ValueType.INTEGER, 1)
                                , "sleepTime", new Value(ValueType.INTEGER, 0)
                                , "sleepTimeType", new Value("FIXED")
                        ),
                        null,
                        null
                ),
                new Cycle()
        );
        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(1)),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value(3)),
                                                new Message(MessageType.DATA, new Date(), new Value(4))
                                        ),
                                        ActionType.EXECUTE
                                ))),
                null,
                null,
                List.of(
                        list -> {
                            System.out.println(list);
                            return new Action(
                                    List.of(
                                            new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 0))
                                    ),
                                    ActionType.EXECUTE);
                        }
                )
        );
        process.fullLifeCycle(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
    }

    @Test
    public void process3() throws InterruptedException {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "type", new Value(ValueType.STRING, "FOR_EACH_DYNAMIC")
                                , "idForBreak", new Value(ValueType.STRING, " ")
                                , "idForContinue", new Value(ValueType.STRING, " ")
                                , "checkType", new Value(ValueType.STRING, "NO_DATA")
                                , "forCount", new Value(ValueType.INTEGER, 1)
                                , "sleepTime", new Value(ValueType.INTEGER, 0)
                                , "sleepTimeType", new Value("FIXED")
                        ),
                        null,
                        null
                ),
                new Cycle()
        );
        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value(3)),
                                                new Message(MessageType.DATA, new Date(), new Value(4))
                                        ),
                                        ActionType.EXECUTE
                                ))),
                null,
                null,
                List.of(
                        list -> {
                            System.out.println(list);
                            return new Action(
                                    List.of(
                                            new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 0))
                                    ),
                                    ActionType.EXECUTE);
                        }
                )
        );
        process.fullLifeCycle(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
    }

    @Test
    public void process4() throws InterruptedException {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "type", new Value(ValueType.STRING, "FOR_EACH_OBJECT")
                                , "idForBreak", new Value(ValueType.STRING, " ")
                                , "idForContinue", new Value(ValueType.STRING, " ")
                                , "checkType", new Value(ValueType.STRING, "NO_DATA")
                                , "forCount", new Value(ValueType.INTEGER, 10)
                                , "sleepTime", new Value(ValueType.INTEGER, 0)
                                , "sleepTimeType", new Value("FIXED")
                        ),
                        null,
                        null
                ),
                new Cycle()
        );
        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(1)),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value("id")),
                                                new Message(MessageType.DATA, new Date(), new Value(7)),
                                                new Message(MessageType.DATA, new Date(), new Value(1)),
                                                new Message(MessageType.DATA, new Date(), new Value("field")),
                                                new Message(MessageType.DATA, new Date(), new Value(4)),
                                                new Message(MessageType.DATA, new Date(), new Value("test")),
                                                new Message(MessageType.DATA, new Date(), new Value(3)),
                                                new Message(MessageType.DATA, new Date(), new Value("id")),
                                                new Message(MessageType.DATA, new Date(), new Value(7)),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value("field1")),
                                                new Message(MessageType.DATA, new Date(), new Value(4)),
                                                new Message(MessageType.DATA, new Date(), new Value("test1")),
                                                new Message(MessageType.DATA, new Date(), new Value("field2")),
                                                new Message(MessageType.DATA, new Date(), new Value(0)),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value("field1")),
                                                new Message(MessageType.DATA, new Date(), new Value(1)),
                                                new Message(MessageType.DATA, new Date(), new Value("field2")),
                                                new Message(MessageType.DATA, new Date(), new Value("test1")),
                                                new Message(MessageType.DATA, new Date(), new Value("field1")),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value("field2")),
                                                new Message(MessageType.DATA, new Date(), new Value("test2"))
                                        ),
                                        ActionType.EXECUTE
                                ))),
                null,
                null,
                List.of(
                        list -> {
                            System.out.println(list);
                            return new Action(
                                    List.of(
                                            new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 0))
                                    ),
                                    ActionType.EXECUTE);
                        }
                )
        );
        process.fullLifeCycle(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
    }

    @Test
    public void process5() throws InterruptedException {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "type", new Value(ValueType.STRING, "FOR_EACH_OBJECT_PARALLEL")
                                , "idForBreak", new Value(ValueType.STRING, " ")
                                , "idForContinue", new Value(ValueType.STRING, " ")
                                , "checkType", new Value(ValueType.STRING, "NO_DATA")
                                , "forCount", new Value(ValueType.INTEGER, 10)
                                , "sleepTime", new Value(ValueType.INTEGER, 0)
                                , "sleepTimeType", new Value("FIXED")
                                , "countThreads", new Value(ValueType.INTEGER, 2)
                        ),
                        null,
                        null
                ),
                new Cycle()
        );
        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(1)),
                                                new Message(MessageType.DATA, new Date(), new Value(5)),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value("id")),
                                                new Message(MessageType.DATA, new Date(), new Value(7)),
                                                new Message(MessageType.DATA, new Date(), new Value(1)),
                                                new Message(MessageType.DATA, new Date(), new Value("field")),
                                                new Message(MessageType.DATA, new Date(), new Value(4)),
                                                new Message(MessageType.DATA, new Date(), new Value("test")),
                                                new Message(MessageType.DATA, new Date(), new Value(3)),
                                                new Message(MessageType.DATA, new Date(), new Value("id")),
                                                new Message(MessageType.DATA, new Date(), new Value(7)),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value("field1")),
                                                new Message(MessageType.DATA, new Date(), new Value(4)),
                                                new Message(MessageType.DATA, new Date(), new Value("test1")),
                                                new Message(MessageType.DATA, new Date(), new Value("field2")),
                                                new Message(MessageType.DATA, new Date(), new Value(0)),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value("field1")),
                                                new Message(MessageType.DATA, new Date(), new Value(1)),
                                                new Message(MessageType.DATA, new Date(), new Value("field2")),
                                                new Message(MessageType.DATA, new Date(), new Value("test1")),
                                                new Message(MessageType.DATA, new Date(), new Value("field1")),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value("field2")),
                                                new Message(MessageType.DATA, new Date(), new Value("test2")),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value("id")),
                                                new Message(MessageType.DATA, new Date(), new Value(7)),
                                                new Message(MessageType.DATA, new Date(), new Value(3)),
                                                new Message(MessageType.DATA, new Date(), new Value("field")),
                                                new Message(MessageType.DATA, new Date(), new Value(4)),
                                                new Message(MessageType.DATA, new Date(), new Value("test")),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value("id")),
                                                new Message(MessageType.DATA, new Date(), new Value(7)),
                                                new Message(MessageType.DATA, new Date(), new Value(4)),
                                                new Message(MessageType.DATA, new Date(), new Value("field")),
                                                new Message(MessageType.DATA, new Date(), new Value(4)),
                                                new Message(MessageType.DATA, new Date(), new Value("test")),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value("id")),
                                                new Message(MessageType.DATA, new Date(), new Value(7)),
                                                new Message(MessageType.DATA, new Date(), new Value(5)),
                                                new Message(MessageType.DATA, new Date(), new Value("field")),
                                                new Message(MessageType.DATA, new Date(), new Value(4)),
                                                new Message(MessageType.DATA, new Date(), new Value("test"))
                                        ),
                                        ActionType.EXECUTE
                                ))),
                null,
                null,
                List.of(
                        list -> {
                            System.out.println(list);
                            return new Action(
                                    List.of(
                                            new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 0))
                                    ),
                                    ActionType.EXECUTE);
                        }
                )
        );
        process.fullLifeCycle(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
    }

}
