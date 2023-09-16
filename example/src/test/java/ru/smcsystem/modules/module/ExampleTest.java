package ru.smcsystem.modules.module;

import org.junit.Assert;
import org.junit.Test;
import ru.smcsystem.api.dto.IMessage;
import ru.smcsystem.api.enumeration.ActionType;
import ru.smcsystem.api.enumeration.MessageType;
import ru.smcsystem.api.enumeration.ValueType;
import ru.smcsystem.test.Process;
import ru.smcsystem.test.emulate.*;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ExampleTest {

    @Test
    public void process() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "value", new Value("Hello world")
                                , "param", new Value("test value")
                        ),
                        ".",
                        null
                ),
                new Example()
        );
        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(null, null, null);
        process.fullLifeCycle(executionContextTool);
        // executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        List<IMessage> findMessages = executionContextTool.getOutput().stream()
                .peek(m -> System.out.println(m.getMessageType() + " " + m.getValue()))
                .filter(m -> ValueType.STRING.equals(m.getType()) && ((String) m.getValue()).equals("Hello world"))
                .collect(Collectors.toList());

        Assert.assertEquals(findMessages.size(), 1);
    }

    @Test
    public void process2() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "value", new Value("Hello world")
                                , "param", new Value("test value")
                        ),
                        ".",
                        null
                ),
                new Example()
        );
        process.start();

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(1)),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value(3))
                                        ),
                                        ActionType.EXECUTE
                                ))), null, null);

        process.execute(executionContextTool);

        List<IMessage> findMessages = executionContextTool.getOutput().stream()
                .peek(m -> System.out.println(m.getMessageType() + " " + m.getValue()))
                .filter(m -> (ValueType.STRING.equals(m.getType()) && ((String) m.getValue()).equals("Hello world")) || (ValueType.INTEGER.equals(m.getType()) && ((Number) m.getValue()).intValue() > 1 && ((Number) m.getValue()).intValue() <= 3))
                .collect(Collectors.toList());

        process.stop();

        Assert.assertEquals(findMessages.size(), 3);
    }

}
