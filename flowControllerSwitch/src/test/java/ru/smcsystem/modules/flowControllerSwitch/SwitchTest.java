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
                                "isNeedReturnDataFromLast", new Value(ValueType.STRING, "true"),
                                "patterns", new Value(ValueType.STRING, "")
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
    public void process3() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "isNeedReturnDataFromLast", new Value("true"),
                                "patterns", new Value("str1::str2::val.*")
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

}