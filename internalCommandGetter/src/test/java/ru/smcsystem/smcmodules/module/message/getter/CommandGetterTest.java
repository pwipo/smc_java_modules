package ru.smcsystem.smcmodules.module.message.getter;

import org.junit.Test;
import ru.smcsystem.api.enumeration.ActionType;
import ru.smcsystem.api.enumeration.MessageType;
import ru.smcsystem.api.enumeration.ValueType;
import ru.smcsystem.test.Process;
import ru.smcsystem.test.emulate.*;

import java.util.Date;
import java.util.List;

public class CommandGetterTest {

    @Test
    public void process() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        null,
                        null,
                        null
                ),
                new CommandGetter()
        );

        process.start();

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(List.of(
                List.of(
                        new Action(
                                List.of(
                                        new Message(MessageType.DATA, new Date(), new Value(1)),
                                        new Message(MessageType.DATA, new Date(), new Value(2)),
                                        new Message(MessageType.DATA, new Date(), new Value(3)),
                                        new Message(MessageType.DATA, new Date(), new Value(2)),
                                        new Message(MessageType.DATA, new Date(), new Value(4)),
                                        new Message(MessageType.DATA, new Date(), new Value(4)),
                                        new Message(MessageType.DATA, new Date(), new Value(2)),
                                        new Message(MessageType.DATA, new Date(), new Value(6))
                                ),
                                ActionType.EXECUTE)),
                List.of(
                        new Action(
                                List.of(
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.BYTE, (byte) 1)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.SHORT, (short) 2)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 3)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.LONG, 4L)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.FLOAT, 5.f)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.DOUBLE, 6.d)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 7)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 8)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 9)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 10))
                                ),
                                ActionType.EXECUTE
                        ))),
                null,
                null);
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        process.stop();
    }

}