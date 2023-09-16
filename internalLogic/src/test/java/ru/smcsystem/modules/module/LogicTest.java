package ru.smcsystem.modules.module;

import org.junit.Assert;
import org.junit.Test;
import ru.smcsystem.api.enumeration.ActionType;
import ru.smcsystem.api.enumeration.MessageType;
import ru.smcsystem.test.Process;
import ru.smcsystem.test.emulate.*;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class LogicTest {

    @Test
    public void process() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "type", new Value("NOT")
                        ),
                        null,
                        null
                ),
                new Logic()
        );
        process.start();

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        Collections.EMPTY_LIST,
                                        ActionType.EXECUTE
                                ))), null, null);
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        Assert.assertFalse(executionContextTool.getOutput().isEmpty());

        process.getConfigurationTool().getAllSettings().put("type", new Value("AND"));
        process.update();
        executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        Collections.EMPTY_LIST,
                                        ActionType.EXECUTE
                                ))), null, null);
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        Assert.assertTrue(executionContextTool.getOutput().isEmpty());

        process.getConfigurationTool().getAllSettings().put("type", new Value("AND"));
        process.update();
        executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(1)),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value(3))
                                        ),
                                        ActionType.EXECUTE
                                )),
                        List.of(
                                new Action(
                                        Collections.EMPTY_LIST,
                                        ActionType.EXECUTE
                                ))
                ), null, null);
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        Assert.assertTrue(executionContextTool.getOutput().isEmpty());

        executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(1)),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value(3))
                                        ),
                                        ActionType.EXECUTE
                                )),
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(1)),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value(3))
                                        ),
                                        ActionType.EXECUTE
                                ))
                ), null, null);
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        Assert.assertFalse(executionContextTool.getOutput().isEmpty());

        process.getConfigurationTool().getAllSettings().put("type", new Value("OR"));
        process.update();
        executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(1)),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value(3))
                                        ),
                                        ActionType.EXECUTE
                                )),
                        List.of(
                                new Action(
                                        Collections.EMPTY_LIST,
                                        ActionType.EXECUTE
                                ))
                ), null, null);
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        Assert.assertFalse(executionContextTool.getOutput().isEmpty());

        process.stop();
    }
}
