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

public class GetTest {

    @Test
    public void process() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "processingType", new Value(ValueType.STRING, "EACH_SOURCE"),
                                "type", new Value(ValueType.STRING, "WORK_DATA"),
                                "valueType", new Value(ValueType.STRING, "ALL"),
                                "ids", new Value(ValueType.STRING, "1,4"),
                                "defaultValues", new Value(""),
                                "defaultValueType", new Value("STATIC")
                        ),
                        null,
                        null
                ),
                new Get()
        );
        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(List.of(
                List.of(
                        new Action(
                                List.of(
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 1)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 2)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 3))
                                ),
                                ActionType.EXECUTE
                        ),
                        new Action(
                                List.of(
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 3)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 4)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 5))
                                ),
                                ActionType.EXECUTE
                        ))),
                null,
                null);
        process.fullLifeCycle(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
    }

    @Test
    public void process2() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "processingType", new Value(ValueType.STRING, "EACH_ACTION"),
                                "type", new Value(ValueType.STRING, "WORK_DATA"),
                                "valueType", new Value(ValueType.STRING, "ALL"),
                                "ids", new Value(ValueType.STRING, "0, 3:4, 3:1, -1:-2, -3:-2"),
                                "defaultValues", new Value(""),
                                "defaultValueType", new Value("STATIC")
                        ),
                        null,
                        null
                ),
                new Get()
        );
        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(List.of(
                List.of(
                        new Action(
                                List.of(
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 1)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 2)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 3)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 4)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.DOUBLE, 5.3)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 6)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 7)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 8)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 9))
                                ),
                                ActionType.EXECUTE
                        ),
                        new Action(
                                List.of(
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 11)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 12)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 13)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 14)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.DOUBLE, 15.3)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 16)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 17)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 18)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 19))
                                ),
                                ActionType.EXECUTE
                        ))),
                null,
                null);
        process.fullLifeCycle(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
    }

    @Test
    public void process3() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "processingType", new Value(ValueType.STRING, "EACH_ACTION"),
                                "type", new Value(ValueType.STRING, "WORK_DATA"),
                                "valueType", new Value(ValueType.STRING, "ALL"),
                                "ids", new Value(ValueType.STRING, "1,5"),
                                "defaultValues", new Value(ValueType.STRING, "1::default_value"),
                                "defaultValueType", new Value("STATIC")
                        ),
                        null,
                        null
                ),
                new Get()
        );
        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 1)),
                                                new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 2)),
                                                new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 3))
                                        ),
                                        ActionType.EXECUTE
                                )
                        )),
                null,
                null);
        process.fullLifeCycle(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
    }

    @Test
    public void process4() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "processingType", new Value(ValueType.STRING, "EACH_ACTION"),
                                "type", new Value(ValueType.STRING, "WORK_DATA"),
                                "valueType", new Value(ValueType.STRING, "ALL"),
                                "ids", new Value(ValueType.STRING, "1,5"),
                                "defaultValues", new Value(ValueType.STRING, "0::0"),
                                "defaultValueType", new Value("DYNAMIC")
                        ),
                        null,
                        null
                ),
                new Get()
        );
        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 1)),
                                                new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 2)),
                                                new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 3))
                                        ),
                                        ActionType.EXECUTE
                                )
                        )),
                null,
                null);
        process.fullLifeCycle(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
    }

    @Test
    public void process5() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "processingType", new Value(ValueType.STRING, "EACH_ACTION"),
                                "type", new Value(ValueType.STRING, "SOURCE_ID"),
                                "valueType", new Value(ValueType.STRING, "ALL"),
                                "ids", new Value(ValueType.STRING, ""),
                                "defaultValues", new Value(ValueType.STRING, ""),
                                "defaultValueType", new Value("STATIC")
                        ),
                        null,
                        null
                ),
                new Get()
        );
        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(1)),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value(3))
                                        ),
                                        ActionType.EXECUTE
                                )
                        ),
                        List.of(),
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.ERROR, new Date(), new Value(1)),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.ERROR, new Date(), new Value(3))
                                        ),
                                        ActionType.EXECUTE
                                )
                        )),
                null,
                null);
        process.fullLifeCycle(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
    }

    @Test
    public void process6() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "processingType", new Value(ValueType.STRING, "EACH_ACTION"),
                                "type", new Value(ValueType.STRING, "RANDOM"),
                                "valueType", new Value(ValueType.STRING, "ALL"),
                                "ids", new Value(ValueType.STRING, ""),
                                "defaultValues", new Value(""),
                                "defaultValueType", new Value("STATIC")
                        ),
                        null,
                        null
                ),
                new Get()
        );
        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(List.of(
                List.of(
                        new Action(
                                List.of(
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 1)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 2)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 3)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 4)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.DOUBLE, 5.3)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 6)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 7)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 8)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 9))
                                ),
                                ActionType.EXECUTE
                        ))),
                null,
                null);
        process.fullLifeCycle(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
    }

    @Test
    public void processErrors() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "processingType", new Value(ValueType.STRING, "EACH_ACTION"),
                                "type", new Value(ValueType.STRING, "DATA_AND_ERROR"),
                                "valueType", new Value(ValueType.STRING, "ALL"),
                                "ids", new Value(ValueType.STRING, ""),
                                "defaultValues", new Value(""),
                                "defaultValueType", new Value("STATIC"),
                                "outputErrorAsData", new Value(false)
                        ),
                        null,
                        null
                ),
                new Get()
        );
        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(List.of(
                List.of(
                        new Action(
                                List.of(
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 1)),
                                        new Message(MessageType.ERROR, new Date(), new Value(ValueType.INTEGER, 2)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 3)),
                                        new Message(MessageType.ERROR, new Date(), new Value(ValueType.INTEGER, 4))
                                ),
                                ActionType.EXECUTE
                        ))),
                null,
                null);
        process.fullLifeCycle(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
    }

}