package ru.smcsystem.modules.internalSymbolsGenerator;

import org.junit.Test;
import ru.smcsystem.api.enumeration.ActionType;
import ru.smcsystem.api.enumeration.MessageType;
import ru.smcsystem.test.Process;
import ru.smcsystem.test.emulate.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class SymbolsGeneratorTest {

    @Test
    public void process() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "split",
                        null,
                        Map.of(
                                // "type", new Value(ValueType.INTEGER, 6),
                                // "size", new Value(ValueType.INTEGER, 8)
                                "sizeNumber", new Value(5),
                                "sizeAlphaUp", new Value(5),
                                "sizeAlphaLow", new Value(5),
                                "sizeNonAlphaNum", new Value(2)
                        ),
                        null,
                        null
                ),
                new SymbolsGenerator()
        );
        process.start();

        ExecutionContextToolImpl executionContextTool1 = new ExecutionContextToolImpl(null,
                null,
                null);
        process.execute(executionContextTool1);
        executionContextTool1.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        process.update();

        ExecutionContextToolImpl executionContextTool2 = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(5)),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value(5)),
                                                new Message(MessageType.DATA, new Date(), new Value(2))
                                        ),
                                        ActionType.EXECUTE
                                ))),
                null,
                null);
        // process.fullLifeCycle(executionContextTool);
        process.execute(executionContextTool2);
        executionContextTool2.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        process.stop();
    }

    @Test
    public void format() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "split",
                        null,
                        Map.of(
                                // "type", new Value(ValueType.INTEGER, 6),
                                // "size", new Value(ValueType.INTEGER, 8)
                                "sizeNumber", new Value(5),
                                "sizeAlphaUp", new Value(5),
                                "sizeAlphaLow", new Value(5),
                                "sizeNonAlphaNum", new Value(2)
                        ),
                        null,
                        null
                ),
                new SymbolsGenerator()
        );
        process.start();

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(new Value("{4sS}-{5n}-{2o}"))
                                        ),
                                        ActionType.EXECUTE
                                ))),
                null, null, null, "format", "format");
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        process.stop();
    }

}