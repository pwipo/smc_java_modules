package ru.smcsystem.modules.configurationControllerExecutionContextEnableToggle;

import org.junit.Test;
import ru.smcsystem.api.dto.IMessage;
import ru.smcsystem.api.enumeration.ValueType;
import ru.smcsystem.test.Process;
import ru.smcsystem.test.emulate.*;
import ru.smcsystem.test.emulate.Module;

import java.util.List;
import java.util.Map;

public class ExecutionContextToggleTest {

    @Test
    public void process() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "names", new Value(ValueType.STRING, "ping.main")
                        ),
                        null,
                        null
                ),
                new ExecutionContextToggle()
        );
        process.start();

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                null,
                List.of(
                        new Configuration(
                                null
                                , null
                                , new Module("ping")
                                , "ping"
                                , null
                                , Map.of("address", new Value(ValueType.STRING, "google.com"))
                                , null
                                , List.of(new ExecutionContext("main", "default", -1))
                                , null)
                ),
                null
        );
        List<IMessage> iMessages = process.fullLifeCycle(executionContextTool);
        // executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        iMessages.forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
    }
}