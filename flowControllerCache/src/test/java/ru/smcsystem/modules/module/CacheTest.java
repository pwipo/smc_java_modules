package ru.smcsystem.modules.module;

import org.junit.Test;
import ru.smcsystem.api.dto.IAction;
import ru.smcsystem.api.enumeration.ActionType;
import ru.smcsystem.api.enumeration.MessageType;
import ru.smcsystem.test.Process;
import ru.smcsystem.test.emulate.*;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class CacheTest {

    @Test
    public void process() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "cacheSize", new Value(2)
                                , "expireTime", new Value(0)
                                , "cacheNull", new Value("false")
                                , "maxValueSize", new Value(0)
                        ),
                        null,
                        null
                ),
                new Cache()
        );

        process.start();

        List<List<IAction>> lists = List.of(
                List.of(
                        new Action(
                                List.of(
                                        new Message(MessageType.DATA, new Date(), new Value(1)),
                                        new Message(MessageType.DATA, new Date(), new Value(2)),
                                        new Message(MessageType.DATA, new Date(), new Value(3))
                                ),
                                ActionType.EXECUTE
                        ),
                        new Action(
                                List.of(
                                        new Message(MessageType.DATA, new Date(), new Value(4)),
                                        new Message(MessageType.DATA, new Date(), new Value(5)),
                                        new Message(MessageType.DATA, new Date(), new Value(6))
                                ),
                                ActionType.EXECUTE
                        ),
                        new Action(
                                List.of(
                                        new Message(MessageType.DATA, new Date(), new Value(7)),
                                        new Message(MessageType.DATA, new Date(), new Value(8)),
                                        new Message(MessageType.DATA, new Date(), new Value(9))
                                ),
                                ActionType.EXECUTE
                        )));

        List<Function<List<Object>, IAction>> funcs = List.of(
                params -> {
                    System.out.println("func1");
                    return new Action(
                            List.of(
                                    new Message(
                                            MessageType.DATA,
                                            new Date(),
                                            new Value(params.stream()
                                                    .mapToDouble(v -> ((Number) v).doubleValue())
                                                    .sum()))
                            ),
                            ActionType.EXECUTE
                    );
                }
        );

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(lists, null, null, funcs);
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        executionContextTool = new ExecutionContextToolImpl(lists, null, null, funcs);
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(1))
                                                // new Message(MessageType.DATA, new Date(), new Value(2))
                                        ),
                                        ActionType.EXECUTE
                                ))), null, null);
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(3)),
                                                new Message(MessageType.DATA, new Date(), new Value("4_5_6")),
                                                new Message(MessageType.DATA, new Date(), new Value(4)),
                                                new Message(MessageType.DATA, new Date(), new Value("test")),
                                                new Message(MessageType.DATA, new Date(), new Value("test_value")),
                                                new Message(MessageType.DATA, new Date(), new Value("test_value2"))
                                        ),
                                        ActionType.EXECUTE
                                ))), null, null);
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(3)),
                                                new Message(MessageType.DATA, new Date(), new Value("test")),
                                                new Message(MessageType.DATA, new Date(), new Value(6)),
                                                new Message(MessageType.DATA, new Date(), new Value(5)),
                                                new Message(MessageType.DATA, new Date(), new Value("test")),
                                                new Message(MessageType.DATA, new Date(), new Value(3)),
                                                new Message(MessageType.DATA, new Date(), new Value("test"))
                                        ),
                                        ActionType.EXECUTE
                                ))), null, null);
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                // new Message(MessageType.DATA, new Date(), new Value(1))
                                                new Message(MessageType.DATA, new Date(), new Value(2))
                                        ),
                                        ActionType.EXECUTE
                                ))), null, null);
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        process.stop();
    }

    @Test
    public void processFiles() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "cacheSize", new Value(10)
                                , "expireTime", new Value(0)
                                , "cacheNull", new Value("false")
                                , "maxValueSize", new Value(2)
                        ),
                        null,
                        null
                ),
                new Cache()
        );

        process.start();

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value("file1"))
                                        ),
                                        ActionType.EXECUTE
                                ))), null, null,
                List.of(
                        params -> {
                            System.out.println("func1");
                            System.out.println(params);
                            return new Action(
                                    List.of(
                                            new Message(MessageType.DATA, new Date(), new Value("t123".getBytes()))
                                    ),
                                    ActionType.EXECUTE
                            );
                        }
                ), "default", "get_or_load_file_part");
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().clear();

        executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value("file1"))
                                        ),
                                        ActionType.EXECUTE
                                ))), null, null,
                List.of(
                        params -> {
                            System.out.println("func1");
                            System.out.println(params);
                            return new Action(
                                    List.of(
                                            new Message(MessageType.DATA, new Date(), new Value(4))
                                    ),
                                    ActionType.EXECUTE
                            );
                        }
                ), "default", "get_size");
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().clear();

        process.stop();
    }

}
