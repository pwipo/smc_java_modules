package ru.smcsystem.modules.flowControllerIfInteger;

import org.junit.Test;
import ru.smcsystem.api.dto.ObjectArray;
import ru.smcsystem.api.dto.ObjectElement;
import ru.smcsystem.api.dto.ObjectField;
import ru.smcsystem.api.enumeration.ActionType;
import ru.smcsystem.api.enumeration.MessageType;
import ru.smcsystem.api.enumeration.ObjectType;
import ru.smcsystem.api.enumeration.ValueType;
import ru.smcsystem.test.Process;
import ru.smcsystem.test.emulate.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class IfIntegerTest {

    @Test
    public void process() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "number", new Value(ValueType.DOUBLE, 1.),
                                "type", new Value(ValueType.STRING, "NUMBER_EQUAL"),
                                "hasElse", new Value(ValueType.STRING, "false"),
                                "isNeedReturnDataFromLast", new Value(ValueType.STRING, "true"),
                                "isNeedInsertSourceDataToExecutionContexts", new Value(ValueType.STRING, "false"),
                                "paths", new Value("value"),
                                "isNeedReturnErrorFromLast", new Value(false)
                        ),
                        null,
                        null
                ),
                new IfInteger()
        );
        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 0))),
                                        ActionType.EXECUTE)
                        ),
                        List.of(
                                new Action(
                                        List.of(new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 1))),
                                        ActionType.EXECUTE)
                        ),
                        List.of(
                                new Action(
                                        List.of(new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 2))),
                                        ActionType.EXECUTE)
                        )

                ),
                null,
                List.of(
                        new Action(
                                List.of(new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 0))),
                                ActionType.EXECUTE)
                )
        );
        process.start();

        process.execute(executionContextTool).forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        // executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().clear();

        process.getConfigurationTool().getAllSettings().put("type", new Value(ValueType.STRING, "NUMBER_MORE_THEN"));
        process.update();
        executionContextTool.getOutput().clear();

        process.execute(executionContextTool).forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        // executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().clear();

        process.getConfigurationTool().getAllSettings().put("type", new Value(ValueType.STRING, "NUMBER_MORE_THEN_OR_EQUAL"));
        process.update();
        executionContextTool.getOutput().clear();

        process.execute(executionContextTool).forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        // executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().clear();

        process.getConfigurationTool().getAllSettings().put("type", new Value(ValueType.STRING, "NUMBER_LESS_THEN"));
        process.update();
        executionContextTool.getOutput().clear();

        process.execute(executionContextTool).forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        // executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().clear();

        process.getConfigurationTool().getAllSettings().put("type", new Value(ValueType.STRING, "NUMBER_LESS_THEN_OR_EQUAL"));
        process.update();
        executionContextTool.getOutput().clear();

        process.execute(executionContextTool).forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        // executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().clear();

        process.getConfigurationTool().getAllSettings().put("type", new Value(ValueType.STRING, "EXIST"));
        process.update();
        executionContextTool.getOutput().clear();

        process.execute(executionContextTool).forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        // executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().clear();

        process.getConfigurationTool().getAllSettings().put("type", new Value(ValueType.STRING, "NOT_EXIST"));
        process.update();
        executionContextTool.getOutput().clear();

        process.execute(executionContextTool).forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        // executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().clear();

        process.getConfigurationTool().getAllSettings().put("type", new Value(ValueType.STRING, "EXIST_ANY"));
        process.update();
        executionContextTool.getOutput().clear();

        process.execute(executionContextTool).forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        // executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().clear();

        process.getConfigurationTool().getAllSettings().put("type", new Value(ValueType.STRING, "NOT_EXIST_ANY"));
        process.update();
        executionContextTool.getOutput().clear();

        process.execute(executionContextTool).forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        // executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().clear();

        process.getConfigurationTool().getAllSettings().put("type", new Value(ValueType.STRING, "EQUAL"));
        process.update();
        executionContextTool.getOutput().clear();

        process.execute(executionContextTool).forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        // executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().clear();

        process.stop();
    }

    @Test
    public void process2() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "number", new Value(ValueType.DOUBLE, 1.),
                                "type", new Value(ValueType.STRING, "EQUAL"),
                                "hasElse", new Value(ValueType.STRING, "false"),
                                "isNeedReturnDataFromLast", new Value(ValueType.STRING, "true"),
                                "isNeedInsertSourceDataToExecutionContexts", new Value(ValueType.STRING, "false"),
                                "paths", new Value("value"),
                                "isNeedReturnErrorFromLast", new Value(false)
                        ),
                        null,
                        null
                ),
                new IfInteger()
        );
        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(new Message(MessageType.DATA, new Date(), new Value("Project1"))),
                                        ActionType.EXECUTE)
                        ),
                        List.of(
                                new Action(
                                        List.of(new Message(MessageType.DATA, new Date(), new Value("Project1"))),
                                        ActionType.EXECUTE)
                        )

                ),
                null,
                List.of(
                        new Action(
                                List.of(new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 0))),
                                ActionType.EXECUTE)
                )
        );
        process.start();

        process.execute(executionContextTool).forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        // executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().clear();

        process.stop();
    }

    @Test
    public void process3() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "number", new Value(ValueType.DOUBLE, 1.),
                                "type", new Value(ValueType.STRING, "EQUAL"),
                                "hasElse", new Value(ValueType.STRING, "false"),
                                "isNeedReturnDataFromLast", new Value(ValueType.STRING, "true"),
                                "isNeedInsertSourceDataToExecutionContexts", new Value(ValueType.STRING, "false"),
                                "paths", new Value("value"),
                                "isNeedReturnErrorFromLast", new Value(true)
                        ),
                        null,
                        null
                ),
                new IfInteger()
        );
        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(new Message(MessageType.DATA, new Date(), new Value("one"))),
                                        ActionType.EXECUTE)
                        ),
                        List.of(
                                new Action(
                                        List.of(new Message(MessageType.DATA, new Date(), new Value(new ObjectArray(
                                                List.of(new ObjectElement(
                                                        List.of(
                                                                new ObjectField("value", "one")
                                                        ))), ObjectType.OBJECT_ELEMENT)))),
                                        ActionType.EXECUTE)
                        )

                ),
                null,
                List.of(
                        new Action(
                                List.of(
                                        new Message(new Value(0)),
                                        new Message(MessageType.ERROR, new Date(), new Value(1))
                                ),
                                ActionType.EXECUTE)
                )
        );
        process.start();

        process.execute(executionContextTool).forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        // executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().clear();

        process.stop();
    }
}