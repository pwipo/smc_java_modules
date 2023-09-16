package ru.smcsystem.modules.flowControllerCron;

import org.junit.Test;
import ru.smcsystem.api.enumeration.ActionType;
import ru.smcsystem.api.enumeration.MessageType;
import ru.smcsystem.api.enumeration.ValueType;
import ru.smcsystem.test.Process;
import ru.smcsystem.test.emulate.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class CronTest {

    @Test
    public void process() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of("time", new Value(ValueType.STRING, "* * * * *")),
                        null,
                        null
                ),
                new Cron()
        );
        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                null,
                null,
                null
        );
        process.fullLifeCycle(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
    }
}