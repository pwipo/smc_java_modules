package ru.smcsystem.modules.configurationControllerPrintSettings;

import org.junit.Test;
import ru.smcsystem.api.dto.IMessage;
import ru.smcsystem.api.enumeration.ValueType;
import ru.smcsystem.test.Process;
import ru.smcsystem.test.emulate.Configuration;
import ru.smcsystem.test.emulate.ConfigurationToolImpl;
import ru.smcsystem.test.emulate.ExecutionContextToolImpl;
import ru.smcsystem.test.emulate.Module;
import ru.smcsystem.test.emulate.Value;

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
}