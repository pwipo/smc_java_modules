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

public class MathFuncsTest {

    @Test
    public void process() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "type", new Value(ValueType.STRING, "SIGMOID"),
                                "param", new Value(ValueType.DOUBLE, 2.0d),
                                "param2", new Value(ValueType.DOUBLE, 7.0d)
                        ),
                        null,
                        null
                ),
                new MathFuncs()
        );

        process.start();

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(List.of(
                List.of(
                        new Action(
                                List.of(
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.BYTE, (byte) 1)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.SHORT, (short) 2)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 3)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.LONG, 4L)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.FLOAT, 5.f)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.DOUBLE, 6.d)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 7)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 8)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 9)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 10))
                                ),
                                ActionType.EXECUTE
                        ))),
                null,
                null);
        // process.fullLifeCycle(executionContextTool);
        // executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().clear();

        process.getConfigurationTool().getAllSettings().put("type", new Value(ValueType.STRING, "SIGMOID_DERIVATIVE"));
        process.update();
        executionContextTool.getOutput().clear();
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        process.getConfigurationTool().getAllSettings().put("type", new Value(ValueType.STRING, "SUM"));
        process.update();
        executionContextTool.getOutput().clear();
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        process.getConfigurationTool().getAllSettings().put("type", new Value(ValueType.STRING, "SUM_BY_PARAM"));
        process.update();
        executionContextTool.getOutput().clear();
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        process.getConfigurationTool().getAllSettings().put("type", new Value(ValueType.STRING, "ABS"));
        process.update();
        executionContextTool.getOutput().clear();
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        process.getConfigurationTool().getAllSettings().put("type", new Value(ValueType.STRING, "MIN"));
        process.update();
        executionContextTool.getOutput().clear();
        process.execute(executionContextTool);
        System.out.println("MIN");
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        process.getConfigurationTool().getAllSettings().put("type", new Value(ValueType.STRING, "MIN_INDEX"));
        process.update();
        executionContextTool.getOutput().clear();
        process.execute(executionContextTool);
        System.out.println("MIN_INDEX");
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        process.getConfigurationTool().getAllSettings().put("type", new Value(ValueType.STRING, "MAX"));
        process.update();
        executionContextTool.getOutput().clear();
        process.execute(executionContextTool);
        System.out.println("MAX");
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        process.getConfigurationTool().getAllSettings().put("type", new Value(ValueType.STRING, "MAX_INDEX"));
        process.update();
        executionContextTool.getOutput().clear();
        process.execute(executionContextTool);
        System.out.println("MAX_INDEX");
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        process.getConfigurationTool().getAllSettings().put("type", new Value(ValueType.STRING, "MULTIPLY"));
        process.update();
        executionContextTool.getOutput().clear();
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        process.getConfigurationTool().getAllSettings().put("type", new Value(ValueType.STRING, "MULTIPLY_BY_PARAM"));
        process.update();
        executionContextTool.getOutput().clear();
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        process.getConfigurationTool().getAllSettings().put("type", new Value(ValueType.STRING, "AVERAGE"));
        process.update();
        executionContextTool.getOutput().clear();
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        process.getConfigurationTool().getAllSettings().put("type", new Value(ValueType.STRING, "RMS"));
        process.update();
        executionContextTool.getOutput().clear();
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        process.getConfigurationTool().getAllSettings().put("type", new Value(ValueType.STRING, "DIFFERENCE"));
        process.update();
        executionContextTool.getOutput().clear();
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        process.getConfigurationTool().getAllSettings().put("type", new Value(ValueType.STRING, "DIFFERENCE_BY_PARAM"));
        process.update();
        executionContextTool.getOutput().clear();
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        process.getConfigurationTool().getAllSettings().put("type", new Value(ValueType.STRING, "MIN_MAX"));
        process.update();
        executionContextTool.getOutput().clear();
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        process.getConfigurationTool().getAllSettings().put("type", new Value(ValueType.STRING, "EXP4J"));
        process.update();
        executionContextTool.getOutput().clear();
        process.execute(
                new ExecutionContextToolImpl(List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value("10+7.2973525698e-3 * 40"))
                                        ),
                                        ActionType.EXECUTE
                                ))),
                        null,
                        null))
                .forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        // executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        process.getConfigurationTool().getAllSettings().put("type", new Value(ValueType.STRING, "EXP4J"));
        process.update();
        executionContextTool.getOutput().clear();
        process.execute(
                new ExecutionContextToolImpl(List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value("3 * sin(y) - 2 / (x - 2)")),
                                                new Message(MessageType.DATA, new Date(), new Value("x")),
                                                new Message(MessageType.DATA, new Date(), new Value("y")),
                                                new Message(MessageType.DATA, new Date(), new Value(2.3)),
                                                new Message(MessageType.DATA, new Date(), new Value(3.14)),
                                                new Message(MessageType.DATA, new Date(), new Value(3.3)),
                                                new Message(MessageType.DATA, new Date(), new Value(4.14))
                                        ),
                                        ActionType.EXECUTE
                                ))),
                        null,
                        null))
                .forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        // executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        process.stop();
    }
}