package ru.smcsystem.modules.flowControllerSwitch;

import org.junit.Test;
import ru.smcsystem.api.enumeration.ActionType;
import ru.smcsystem.api.enumeration.MessageType;
import ru.smcsystem.api.enumeration.ValueType;
import ru.smcsystem.test.Process;
import ru.smcsystem.test.emulate.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class SwitchTest {

    @Test
    public void process() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "isNeedReturnDataFromLast", new Value("true"),
                                "patterns", new Value(""),
                                "countPatternsInPack", new Value(1),
                                "isNeedReturnErrorFromLast",new Value(false)
                        ),
                        null,
                        null
                ),
                new Switch()
        );
        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 0))),
                                        ActionType.EXECUTE)
                        ),
                        List.of(
                                new Action(
                                        List.of(new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 1))),
                                        ActionType.EXECUTE)
                        )

                ),
                null,
                List.of(
                        new Action(
                                List.of(new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 0))),
                                ActionType.EXECUTE)
                )
        );
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
                                "isNeedReturnDataFromLast", new Value("true"),
                                "patterns", new Value("str1::str2::val.*"),
                                "countPatternsInPack", new Value(1),
                                "isNeedReturnErrorFromLast",new Value(false)
                        ),
                        null,
                        null
                ),
                new Switch()
        );
        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value("str1")),
                                                new Message(MessageType.DATA, new Date(), new Value("str2")),
                                                new Message(MessageType.DATA, new Date(), new Value("str3")),
                                                new Message(MessageType.DATA, new Date(), new Value("val")),
                                                new Message(MessageType.DATA, new Date(), new Value("value"))
                                        ),
                                        ActionType.EXECUTE)
                        )
                ),
                null,
                List.of(
                        new Action(
                                List.of(new Message(MessageType.DATA, new Date(), new Value(0))),
                                ActionType.EXECUTE),
                        new Action(
                                List.of(new Message(MessageType.DATA, new Date(), new Value(1))),
                                ActionType.EXECUTE),
                        new Action(
                                List.of(new Message(MessageType.DATA, new Date(), new Value(2))),
                                ActionType.EXECUTE)
                )
        );
        process.fullLifeCycle(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
    }

    @Test
    public void process3() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "isNeedReturnDataFromLast", new Value("true"),
                                "patterns", new Value("str1::val.*::str2::val.*::str3::val.*"),
                                "countPatternsInPack", new Value(2),
                                "isNeedReturnErrorFromLast",new Value(true)
                        ),
                        null,
                        null
                ),
                new Switch()
        );
        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value("str1")),
                                                new Message(MessageType.DATA, new Date(), new Value("value")),
                                                new Message(MessageType.DATA, new Date(), new Value("str2")),
                                                new Message(MessageType.DATA, new Date(), new Value("value")),
                                                new Message(MessageType.DATA, new Date(), new Value("str3")),
                                                new Message(MessageType.DATA, new Date(), new Value("value")),
                                                new Message(MessageType.DATA, new Date(), new Value("val")),
                                                new Message(MessageType.DATA, new Date(), new Value("value"))
                                        ),
                                        ActionType.EXECUTE)
                        )
                ),
                null,
                List.of(
                        new Action(
                                List.of(
                                        new Message(new Value(0)),
                                        new Message(MessageType.ERROR, new Date(), new Value(50))
                                ),
                                ActionType.EXECUTE),
                        new Action(
                                List.of(
                                        new Message(new Value(1)),
                                        new Message(MessageType.ERROR, new Date(), new Value(100))
                                ),
                                ActionType.EXECUTE),
                        new Action(
                                List.of(new Message(MessageType.DATA, new Date(), new Value(2))),
                                ActionType.EXECUTE)
                )
        );
        process.fullLifeCycle(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
    }

}