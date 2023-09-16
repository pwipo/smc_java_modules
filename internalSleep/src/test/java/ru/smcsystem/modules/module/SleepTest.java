package ru.smcsystem.modules.module;

import org.junit.Test;
import ru.smcsystem.test.Process;
import ru.smcsystem.test.emulate.ConfigurationToolImpl;
import ru.smcsystem.test.emulate.ExecutionContextToolImpl;
import ru.smcsystem.test.emulate.Value;

import java.util.Map;

public class SleepTest {

    @Test
    public void process() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "type", new Value("NEW_MINUTE"),
                                "value", new Value(1L)
                        ),
                        null,
                        null
                ),
                new Sleep()
        );
        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(null, null, null);
        process.fullLifeCycle(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
    }
}
