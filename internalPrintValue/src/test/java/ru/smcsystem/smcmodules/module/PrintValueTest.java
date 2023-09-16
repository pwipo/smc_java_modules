package ru.smcsystem.smcmodules.module;

import org.junit.Test;
import ru.smcsystem.api.enumeration.ActionType;
import ru.smcsystem.api.enumeration.MessageType;
import ru.smcsystem.api.enumeration.ValueType;
import ru.smcsystem.test.Process;
import ru.smcsystem.test.emulate.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class PrintValueTest {

    @Test
    public void process() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "type", new Value(ValueType.STRING, "STRING"),
                                "appendType", new Value(ValueType.STRING, "LAST"),
                                "splitterValues", new Value(ValueType.STRING, "::"),
                                "value", new Value(ValueType.STRING, "HI")
                        ),
                        null,
                        null
                ),
                new PrintValue()
        );
        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(null,
                null,
                null);
        process.fullLifeCycle(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
    }

    @Test
    public void process2() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "type", new Value(ValueType.STRING, "STRING"),
                                "appendType", new Value(ValueType.STRING, "PLACEHOLDER"),
                                "splitterValues", new Value(ValueType.STRING, "::"),
                                "value", new Value(ValueType.STRING, "START::{0}::MIDDLE::{1}::END")
                        ),
                        null,
                        null
                ),
                new PrintValue()
        );
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
                                )),
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(4)),
                                                new Message(MessageType.DATA, new Date(), new Value(5)),
                                                new Message(MessageType.DATA, new Date(), new Value(6))
                                        ),
                                        ActionType.EXECUTE
                                )),
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(7)),
                                                new Message(MessageType.DATA, new Date(), new Value(8)),
                                                new Message(MessageType.DATA, new Date(), new Value(9))
                                        ),
                                        ActionType.EXECUTE
                                ))
                ),
                null,
                null);
        process.fullLifeCycle(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
    }

}