package ru.smcsystem.modules.module;

import org.junit.Assert;
import org.junit.Test;
import ru.smcsystem.api.dto.IMessage;
import ru.smcsystem.api.enumeration.ActionType;
import ru.smcsystem.api.enumeration.MessageType;
import ru.smcsystem.api.enumeration.ValueType;
import ru.smcsystem.test.Process;
import ru.smcsystem.test.emulate.*;
import ru.smcsystem.test.emulate.Module;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CfgsToObjTest {

    @Test
    public void process() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "params", new Value("0,0,enable,enable::0,0,name,name::0,1,param1,param")
                        ),
                        null,
                        null
                ),
                new CfgsToObj()
        );

        process.start();

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(1))
                                        ),
                                        ActionType.EXECUTE
                                ))),
                List.of(
                        new Configuration(null, null, new Module("test"), "test", null, Map.of("param1", new Value(ValueType.STRING, "google.com")), null, null, null)
                ),
                null
        );
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().clear();

        ExecutionContextToolImpl executionContextToolSet = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value(1)),
                                                new Message(MessageType.DATA, new Date(), new Value(1)),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value("enable")),
                                                new Message(MessageType.DATA, new Date(), new Value(4)),
                                                new Message(MessageType.DATA, new Date(), new Value("false")),
                                                new Message(MessageType.DATA, new Date(), new Value("param")),
                                                new Message(MessageType.DATA, new Date(), new Value(4)),
                                                new Message(MessageType.DATA, new Date(), new Value("google.com1"))
                                        ),
                                        ActionType.EXECUTE
                                ))),
                List.of(
                        new Configuration(null, null, new Module("test"), "test", null, Map.of("param1", new Value(ValueType.STRING, "google.com")), null, null, null)
                ),
                null
        );
        process.execute(executionContextToolSet);
        executionContextToolSet.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextToolSet.getOutput().clear();

        process.stop();
    }
}
