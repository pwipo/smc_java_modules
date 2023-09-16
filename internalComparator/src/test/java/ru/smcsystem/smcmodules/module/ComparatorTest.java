package ru.smcsystem.smcmodules.module;

import org.junit.Test;
import ru.smcsystem.test.emulate.Action;
import ru.smcsystem.test.emulate.Message;
import ru.smcsystem.test.emulate.Value;
import ru.smcsystem.api.enumeration.ActionType;
import ru.smcsystem.api.enumeration.MessageType;
import ru.smcsystem.test.Process;
import ru.smcsystem.test.emulate.ConfigurationToolImpl;
import ru.smcsystem.test.emulate.ExecutionContextToolImpl;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class ComparatorTest {

    @Test
    public void process() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "moreResult", new Value("1")
                                , "lessResult", new Value("-1")
                                , "equalsResult", new Value("0")
                                , "useValue", new Value("false")
                                , "value", new Value("")
                        ),
                        null,
                        null
                ),
                new Comparator()
        );
        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(5))
                                                , new Message(MessageType.DATA, new Date(), new Value(2))
                                                , new Message(MessageType.DATA, new Date(), new Value(7.))
                                                , new Message(MessageType.DATA, new Date(), new Value(12.))
                                                , new Message(MessageType.DATA, new Date(), new Value("test"))
                                                , new Message(MessageType.DATA, new Date(), new Value("test"))
                                                , new Message(MessageType.DATA, new Date(), new Value("test1"))
                                                , new Message(MessageType.DATA, new Date(), new Value("test2"))
                                                , new Message(MessageType.DATA, new Date(), new Value(new byte[]{1,2,3,4,5,6}))
                                                , new Message(MessageType.DATA, new Date(), new Value(new byte[]{1,2,3,4,5}))
                                        ),
                                        ActionType.EXECUTE
                                ))),
                null,
                null);
        process.fullLifeCycle(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

    }
}