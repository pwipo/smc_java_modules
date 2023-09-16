package ru.smcsystem.modules.configurationControllerChangeSettingString;

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

public class ChangeSettingTest {

    @Test
    public void process() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of("names", new Value(ValueType.STRING, "ping"),
                                "key", new Value(ValueType.STRING, "address"),
                                "value", new Value(ValueType.STRING, "www.rbk.ru")
                        ),
                        null,
                        null
                ),
                new ChangeSetting()
        );
        process.start();

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                null,
                List.of(
                        new Configuration(null, null, new Module("ping"), "ping", null, Map.of("address", new Value(ValueType.STRING, "google.com")), null, null, null)
                ),
                null
        );
        List<IMessage> iMessages = process.fullLifeCycle(executionContextTool);
        // executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        iMessages.forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
    }

    @Test
    public void process2() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of("names", new Value(ValueType.STRING, "ping"),
                                "key", new Value(ValueType.STRING, "address"),
                                "value", new Value(ValueType.STRING, "www.rbk.ru")
                        ),
                        null,
                        null
                ),
                new ChangeSetting()
        );
        process.start();

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value("www.rbk2.ru"))
                                        ),
                                        ActionType.EXECUTE
                                ))),
                List.of(
                        new Configuration(null, null, new Module("ping"), "ping", null, Map.of("address", new Value(ValueType.STRING, "google.com")), null, null, null)
                ),
                null
        );
        List<IMessage> iMessages = process.fullLifeCycle(executionContextTool);
        // executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        iMessages.forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
    }

}