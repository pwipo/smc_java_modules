package ru.smcsystem.smcmodules.module;

import org.junit.Test;
import ru.smcsystem.api.enumeration.ActionType;
import ru.smcsystem.api.enumeration.MessageType;
import ru.smcsystem.api.enumeration.ValueType;
import ru.smcsystem.test.Process;
import ru.smcsystem.test.emulate.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class PrintDateTest {

    @Test
    public void process() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "format", new Value(ValueType.STRING, "HH:mm:ss dd MM yyyy")
                                , "local", new Value(ValueType.STRING, "en")
                                , "zoneId", new Value(ValueType.STRING, "+3")
                                , "type", new Value(ValueType.STRING, "TO_STRING")
                        ),
                        null,
                        null
                ),
                new PrintDate()
        );
        process.start();

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(null,
                null,
                null);
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().clear();

        process.getConfigurationTool().getAllSettings().put("type", new Value(ValueType.STRING, "TO_NUMBER"));
        process.update();

        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().clear();

        executionContextTool = new ExecutionContextToolImpl(List.of(
                List.of(
                        new Action(
                                List.of(
                                        new Message(MessageType.DATA, new Date(), new Value("00:00:00 01 01 2000"))
                                ),
                                ActionType.EXECUTE
                        ))),
                null,
                null);
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().clear();

        process.stop();
    }
}