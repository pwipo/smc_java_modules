package ru.smcsystem.modules.module;

import org.junit.Test;
import ru.smcsystem.api.dto.ObjectArray;
import ru.smcsystem.api.dto.ObjectElement;
import ru.smcsystem.api.dto.ObjectField;
import ru.smcsystem.api.enumeration.ActionType;
import ru.smcsystem.api.enumeration.MessageType;
import ru.smcsystem.test.Process;
import ru.smcsystem.test.emulate.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class ValueTransformerTest {

    @Test
    public void process() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "config", new Value("1::2::three::4\n" +
                                        "5::6\n" +
                                        // "hi::hello\n" +
                                        "h.*::hello"),
                                "configObj", new Value(new ObjectArray(new ObjectElement(
                                        new ObjectField("pattern", "2"),
                                        new ObjectField("values", "3::4")
                                )))
                        ),
                        null,
                        null
                ),
                new ValueTransformer()
        );

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(1)),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value(3)),
                                                new Message(MessageType.DATA, new Date(), new Value(5)),
                                                new Message(MessageType.DATA, new Date(), new Value("hi"))
                                        ),
                                        ActionType.EXECUTE
                                ))), null, null);
        process.fullLifeCycle(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
    }

    @Test
    public void getPatterns() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "config", new Value("1::2::three::4\n" +
                                        "5::6\n" +
                                        // "hi::hello\n" +
                                        "h.*::hello"),
                                "configObj", new Value(new ObjectArray(new ObjectElement(
                                        new ObjectField("pattern", "2"),
                                        new ObjectField("values", "3::4")
                                )))
                        ),
                        null,
                        null
                ),
                new ValueTransformer()
        );
        process.start();

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                null, null, null, null, "", "get_patterns");
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        process.stop();
    }

    @Test
    public void getValuesById() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "config", new Value("1::2::three::4\n" +
                                        "5::6\n" +
                                        // "hi::hello\n" +
                                        "h.*::hello"),
                                "configObj", new Value(new ObjectArray(new ObjectElement(
                                        new ObjectField("pattern", "2"),
                                        new ObjectField("values", "3::4")
                                )))
                        ),
                        null,
                        null
                ),
                new ValueTransformer()
        );
        process.start();

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(new Value(0))
                                        ),
                                        ActionType.EXECUTE
                                ))), null, null, null, "", "get_values_by_id");
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        process.stop();
    }

}
