package ru.smcsystem.modules.configurationControllerPrintSettings;

import org.junit.Test;
import ru.smcsystem.api.dto.IMessage;
import ru.smcsystem.api.enumeration.ActionType;
import ru.smcsystem.api.enumeration.MessageType;
import ru.smcsystem.api.enumeration.ValueType;
import ru.smcsystem.test.Process;
import ru.smcsystem.test.emulate.Module;
import ru.smcsystem.test.emulate.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class PrintSettingsTest {

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
                new PrintSettings()
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
    public void testSettings() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        null,
                        null,
                        null
                ),
                new PrintSettings()
        );
        process.start();

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(new Message(new Value("address"))),
                                        ActionType.EXECUTE)
                        )
                ),
                List.of(
                        new Configuration(null, null, new Module("ping"), "ping", null,
                                Map.of("address", new Value("google.com")),
                                null, null, null)
                ),
                null, null, "ec", "setting"
        );
        List<IMessage> iMessages = process.execute(executionContextTool);
        // executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        iMessages.forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        process.stop();
    }

}