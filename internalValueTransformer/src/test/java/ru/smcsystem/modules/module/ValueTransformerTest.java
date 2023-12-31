package ru.smcsystem.modules.module;

import org.junit.Assert;
import org.junit.Test;
import ru.smcsystem.api.dto.IMessage;
import ru.smcsystem.api.enumeration.ActionType;
import ru.smcsystem.api.enumeration.MessageType;
import ru.smcsystem.api.enumeration.ValueType;
import ru.smcsystem.test.Process;
import ru.smcsystem.test.emulate.*;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ValueTransformerTest {

    @Test
    public void process() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "config", new Value("1::2::three::4\n" +
                                        "5::6\n" +
                                        // "hi::hello\n" +
                                        "h.*::hello")
                        ),
                        null,
                        null
                ),
                new ValueTransformer()
        );

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(1)),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value(3)),
                                                new Message(MessageType.DATA, new Date(), new Value(5)),
                                                new Message(MessageType.DATA, new Date(), new Value("hi"))
                                        ),
                                        ActionType.EXECUTE
                                ))), null, null);
        process.fullLifeCycle(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
    }
}
