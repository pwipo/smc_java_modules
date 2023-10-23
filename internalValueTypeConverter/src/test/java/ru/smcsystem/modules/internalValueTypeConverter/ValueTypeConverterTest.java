package ru.smcsystem.modules.internalValueTypeConverter;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import ru.smcsystem.api.dto.IMessage;
import ru.smcsystem.api.enumeration.ActionType;
import ru.smcsystem.api.enumeration.MessageType;
import ru.smcsystem.api.enumeration.ValueType;
import ru.smcsystem.test.Process;
import ru.smcsystem.test.emulate.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class ValueTypeConverterTest {

    @Test
    public void test1() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "type", new Value(ValueType.INTEGER, 3),
                                "charsetName", new Value(ValueType.STRING, "UTF-8"),
                                "param", new Value(ValueType.STRING, " ")

                        ),
                        null,
                        null
                ),
                new ValueTypeConverter()
        );

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(null,
                null,
                null);
        process.fullLifeCycle(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

    }

    @Test
    public void test2() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "type", new Value(ValueType.INTEGER, 3),
                                "charsetName", new Value(ValueType.STRING, "UTF-8"),
                                "param", new Value(ValueType.STRING, " ")

                        ),
                        null,
                        null
                ),
                new ValueTypeConverter()
        );
        process.start();

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(null,
                null,
                null);
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        process.update();

        executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "6")),
                                                new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "20"))
                                        ),
                                        ActionType.EXECUTE
                                ))),
                null,
                null);
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        process.getConfigurationTool().getAllSettings().put("type", new Value(ValueType.INTEGER, 8));
        process.getConfigurationTool().getAllSettings().put("param", new Value(""));
        process.update();
        executionContextTool.getOutput().clear();

        executionContextTool = new ExecutionContextToolImpl(List.of(
                List.of(
                        new Action(
                                List.of(
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 2)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 2)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 2)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "name")),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "jknadb")),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "value")),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "arejrsyj")),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "name")),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "dyulky.liotgultug")),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "value")),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "aethjrtsykjyilyli"))
                                ),
                                ActionType.EXECUTE),
                        new Action(
                                List.of(
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 2)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 1)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 2)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "name")),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "jknadb")),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "value")),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "arejrsyj"))
                                ),
                                ActionType.EXECUTE)
                )
        ),
                null,
                null);

        process.execute(executionContextTool);//.forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().clear();

        executionContextTool = new ExecutionContextToolImpl(List.of(
                List.of(
                        new Action(
                                List.of(
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 1)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 2)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 2)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "name")),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 4)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "test")),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "value")),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 2)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 2)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "field1")),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "name1")),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "field2")),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "value1")),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 2)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "name")),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 4)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "dyulky.liotgultug")),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "value")),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 4)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "aethjrtsykjyilyli"))
                                ),
                                ActionType.EXECUTE)
                )
        ),
                null,
                null);

        process.execute(executionContextTool);//.forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().clear();

        process.getConfigurationTool().getAllSettings().put("type", new Value(ValueType.INTEGER, 13));
        process.update();
        executionContextTool.getOutput().clear();

        process.execute(executionContextTool);//.forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().clear();

        process.getConfigurationTool().getAllSettings().put("type", new Value(ValueType.INTEGER, 8));
        // process.getConfigurationTool().getAllSettings().put("param", new Value("==start==::==end=="));
        process.update();
        executionContextTool.getOutput().clear();

        executionContextTool = new ExecutionContextToolImpl(List.of(
                List.of(
                        new Action(
                                List.of(
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 1)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 2)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 3)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "name")),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 4)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "test")),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "field1")),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 7)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 1)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "value")),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 2)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 2)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "field1")),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "name1")),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "field2")),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "value1")),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 2)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "value")),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 0)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 3)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 6)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "value1")),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "value2")),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 3)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "value3")),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "value4")),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "value5")),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "name")),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 4)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "dyulky.liotgultug"))
                                ),
                                ActionType.EXECUTE)
                )
        ),
                null,
                null);

        process.execute(executionContextTool);//.forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().clear();

        process.getConfigurationTool().getAllSettings().put("type", new Value(ValueType.INTEGER, 9));
        process.update();
        executionContextTool.getOutput().clear();

        executionContextTool = new ExecutionContextToolImpl(List.of(
                List.of(
                        new Action(
                                List.of(
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "[{\"name\":\"jknadb\",\"value\":1},{\"name\":\"dyulky.liotgultug\",\"value\":2}]"))
                                ),
                                ActionType.EXECUTE)
                )
        ),
                null,
                null);

        process.execute(executionContextTool);//.forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getType() + " " + m.getValue()));
        executionContextTool.getOutput().clear();

        executionContextTool = new ExecutionContextToolImpl(List.of(
                List.of(
                        new Action(
                                List.of(
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "[{\"name\":\"test\",\"value\":{\"field1\":\"name1\",\"field2\":\"value1\"}},{\"name\":\"dyulky.liotgultug\",\"value\":\"aethjrtsykjyilyli\"}]"))
                                ),
                                ActionType.EXECUTE)
                )
        ),
                null,
                null);

        process.execute(executionContextTool);//.forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getType() + " " + m.getValue()));
        executionContextTool.getOutput().clear();

        process.getConfigurationTool().getAllSettings().put("type", new Value(ValueType.INTEGER, 9));
        // process.getConfigurationTool().getAllSettings().put("param", new Value("==start==::==end=="));
        process.update();
        executionContextTool.getOutput().clear();

        executionContextTool = new ExecutionContextToolImpl(List.of(
                List.of(
                        new Action(
                                List.of(
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "[{\"field1\":1,\"name\":\"test\",\"value\":[{\"field1\":\"name1\",\"field2\":\"value1\"}]},{\"name\":\"dyulky.liotgultug\",\"value\":\"aethjrtsykjyilyli\"}]"))
                                ),
                                ActionType.EXECUTE)
                )
        ),
                null,
                null);

        process.execute(executionContextTool);//.forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getType() + " " + m.getValue()));
        executionContextTool.getOutput().clear();

        process.update();
        executionContextTool = new ExecutionContextToolImpl(List.of(
                List.of(
                        new Action(
                                List.of(
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "[{\"field1\":1,\"name\":\"test\",\"value\":{\"field1\":\"name1\",\"field2\":\"value1\"}},{\"name\":\"dyulky.liotgultug\",\"value\":[[\"value1\",\"value2\"],[\"value3\",\"value4\",\"value5\"]]}]"))
                                ),
                                ActionType.EXECUTE)
                )
        ),
                null,
                null);

        process.execute(executionContextTool);//.forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getType() + " " + m.getValue()));
        executionContextTool.getOutput().clear();

        process.getConfigurationTool().getAllSettings().put("type", new Value(ValueType.INTEGER, 2));
        process.update();

        executionContextTool = new ExecutionContextToolImpl(List.of(
                List.of(
                        new Action(
                                List.of(
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.BYTE, (byte) 1)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.SHORT, (short) 2)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 3)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.LONG, 4L)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.FLOAT, 5.f)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.DOUBLE, 6.d))
                                ),
                                ActionType.EXECUTE)
                )
        ),
                null,
                null);

        process.execute(executionContextTool);//.forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + StringUtils.join((byte[]) m.getValue(), ',')));
        executionContextTool.getOutput().clear();

        process.getConfigurationTool().getAllSettings().put("type", new Value(ValueType.INTEGER, 10));
        process.update();

        executionContextTool = new ExecutionContextToolImpl(List.of(
                List.of(
                        new Action(
                                List.of(
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.BYTE, (byte) 1))
                                        , new Message(MessageType.DATA, new Date(), new Value(ValueType.SHORT, (short) 2))
                                        , new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 3))
                                        , new Message(MessageType.DATA, new Date(), new Value(ValueType.LONG, 4L))
                                        , new Message(MessageType.DATA, new Date(), new Value(ValueType.FLOAT, 5.f))
                                        , new Message(MessageType.DATA, new Date(), new Value(ValueType.DOUBLE, 6.d))
                                        , new Message(MessageType.DATA, new Date(), new Value("1"))
                                        , new Message(MessageType.DATA, new Date(), new Value("1.1"))
                                        , new Message(MessageType.DATA, new Date(), new Value("16777215"))
                                        , new Message(MessageType.DATA, new Date(), new Value(new byte[]{1, 2, 3, 4, 5}))
                                ),
                                ActionType.EXECUTE)
                )
        ),
                null,
                null);

        process.execute(executionContextTool);//.forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().forEach(m -> System.out.println(String.format("%s %s %s", m.getMessageType(), m.getType(), m.getValue())));
        executionContextTool.getOutput().clear();

        process.getConfigurationTool().getAllSettings().put("type", new Value(ValueType.INTEGER, 11));
        process.getConfigurationTool().getAllSettings().put("param", new Value(ValueType.STRING, ","));
        process.update();

        executionContextTool = new ExecutionContextToolImpl(List.of(
                List.of(
                        new Action(
                                List.of(
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "SPFB.RTS,1,20190813,100100,129590.0000000,129590.0000000,128970.0000000,129040.0000000,4690\nSPFB.RTS,1,20190813,100200,129040.0000000,129040.0000000,128700.0000000,128880.0000000,7063"))
                                ),
                                ActionType.EXECUTE)
                )
        ),
                null,
                null);

        process.execute(executionContextTool);//.forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().forEach(m -> System.out.println(String.format("%s %s %s", m.getMessageType(), m.getType(), m.getValue())));
        executionContextTool.getOutput().clear();

        process.getConfigurationTool().getAllSettings().put("type", new Value(ValueType.INTEGER, 12));
        process.getConfigurationTool().getAllSettings().put("param", new Value(ValueType.STRING, "3::,"));
        process.update();

        executionContextTool = new ExecutionContextToolImpl(List.of(
                List.of(
                        new Action(
                                List.of(
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.BYTE, (byte) 1))
                                        , new Message(MessageType.DATA, new Date(), new Value(ValueType.SHORT, (short) 2))
                                        , new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 3))
                                        , new Message(MessageType.DATA, new Date(), new Value(ValueType.LONG, 4L))
                                        , new Message(MessageType.DATA, new Date(), new Value(ValueType.FLOAT, 5.f))
                                        , new Message(MessageType.DATA, new Date(), new Value(ValueType.DOUBLE, 6.d))
                                        , new Message(MessageType.DATA, new Date(), new Value("1"))
                                        , new Message(MessageType.DATA, new Date(), new Value("1.1"))
                                        , new Message(MessageType.DATA, new Date(), new Value("16777215"))
                                        , new Message(MessageType.DATA, new Date(), new Value("SPB"))
                                        , new Message(MessageType.DATA, new Date(), new Value("1.1"))
                                        , new Message(MessageType.DATA, new Date(), new Value(new byte[]{1, 2, 3, 4, 5}))
                                ),
                                ActionType.EXECUTE)
                )
        ),
                null,
                null);

        process.execute(executionContextTool);//.forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().forEach(m -> System.out.println(String.format("%s %s %s", m.getMessageType(), m.getType(), m.getValue())));
        executionContextTool.getOutput().clear();

        process.stop();
    }

    @Test
    public void test3() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "type", new Value(ValueType.INTEGER, 14),
                                "charsetName", new Value(ValueType.STRING, "UTF-8"),
                                "param", new Value(ValueType.STRING, " ")

                        ),
                        null,
                        null
                ),
                new ValueTypeConverter()
        );

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value("6")),
                                                new Message(MessageType.DATA, new Date(), new Value(7)),
                                                new Message(MessageType.DATA, new Date(), new Value(8.1d)),
                                                new Message(MessageType.DATA, new Date(), new Value(9.1f)),
                                                new Message(MessageType.DATA, new Date(), new Value("test")),
                                                new Message(MessageType.DATA, new Date(), new Value("test2")),
                                                new Message(MessageType.DATA, new Date(), new Value(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9})),
                                                new Message(MessageType.DATA, new Date(), new Value(10L))
                                        ),
                                        ActionType.EXECUTE
                                ))),
                null,
                null);
        process.fullLifeCycle(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
    }

    @Test
    public void test4() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "type", new Value(ValueType.INTEGER, 8),
                                "charsetName", new Value(ValueType.STRING, "UTF-8"),
                                "param", new Value(ValueType.STRING, " ")

                        ),
                        null,
                        null
                ),
                new ValueTypeConverter()
        );

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(1)),
                                                new Message(MessageType.DATA, new Date(), new Value(1)),
                                                new Message(MessageType.DATA, new Date(), new Value(5)),
                                                new Message(MessageType.DATA, new Date(), new Value("resultCode")),
                                                new Message(MessageType.DATA, new Date(), new Value(7)),
                                                new Message(MessageType.DATA, new Date(), new Value(0)),
                                                new Message(MessageType.DATA, new Date(), new Value("resultText")),
                                                new Message(MessageType.DATA, new Date(), new Value(4)),
                                                new Message(MessageType.DATA, new Date(), new Value("ok")),
                                                new Message(MessageType.DATA, new Date(), new Value("userId")),
                                                new Message(MessageType.DATA, new Date(), new Value(3)),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value("roles")),
                                                new Message(MessageType.DATA, new Date(), new Value(0)),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value(1)),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value("ID")),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value("NAME")),
                                                new Message(MessageType.DATA, new Date(), new Value("USER")),
                                                new Message(MessageType.DATA, new Date(), new Value("permissions")),
                                                new Message(MessageType.DATA, new Date(), new Value(0)),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value(1)),
                                                new Message(MessageType.DATA, new Date(), new Value("PATTERN")),
                                                new Message(MessageType.DATA, new Date(), new Value("profile:*:read")),
                                                new Message(MessageType.DATA, new Date(), new Value("PATTERN")),
                                                new Message(MessageType.DATA, new Date(), new Value("profile:read"))
                                        ),
                                        ActionType.EXECUTE
                                ))),
                null,
                null);
        process.fullLifeCycle(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
    }

    @Test
    public void test5() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "type", new Value(15),
                                "charsetName", new Value(ValueType.STRING, "UTF-8"),
                                "param", new Value(ValueType.STRING, " ")

                        ),
                        null,
                        null
                ),
                new ValueTypeConverter()
        );

        process.start();

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                                                        "<company>\ntest2" +
                                                        "    <Staff id=\"1\">\n" +
                                                        "        <firstname>yong</firstname>\n" +
                                                        "        <lastname>mook kim</lastname>\n" +
                                                        "        <nickname>mkyong</nickname>\n" +
                                                        "        <salary>100000</salary>\n" +
                                                        "    </Staff>\n" +
                                                        "</company>"))
                                        ),
                                        ActionType.EXECUTE
                                ))),
                null,
                null);
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        process.stop();
    }


    @Test
    public void test6() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "type", new Value(16),
                                "charsetName", new Value(ValueType.STRING, "UTF-8"),
                                "param", new Value(ValueType.STRING, " ")

                        ),
                        null,
                        null
                ),
                new ValueTypeConverter()
        );

        process.start();

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(1)),
                                                new Message(MessageType.DATA, new Date(), new Value(1)),
                                                new Message(MessageType.DATA, new Date(), new Value(3)),
                                                new Message(MessageType.DATA, new Date(), new Value("name")),
                                                new Message(MessageType.DATA, new Date(), new Value(4)),
                                                new Message(MessageType.DATA, new Date(), new Value("company")),
                                                new Message(MessageType.DATA, new Date(), new Value("value")),
                                                new Message(MessageType.DATA, new Date(), new Value(4)),
                                                new Message(MessageType.DATA, new Date(), new Value("test2")),
                                                new Message(MessageType.DATA, new Date(), new Value("childNodes")),
                                                new Message(MessageType.DATA, new Date(), new Value(0)),
                                                new Message(MessageType.DATA, new Date(), new Value(1)),
                                                new Message(MessageType.DATA, new Date(), new Value(1)),
                                                new Message(MessageType.DATA, new Date(), new Value(4)),
                                                new Message(MessageType.DATA, new Date(), new Value("name")),
                                                new Message(MessageType.DATA, new Date(), new Value(4)),
                                                new Message(MessageType.DATA, new Date(), new Value("Staff")),
                                                new Message(MessageType.DATA, new Date(), new Value("value")),
                                                new Message(MessageType.DATA, new Date(), new Value(4)),
                                                new Message(MessageType.DATA, new Date(), new Value(" ")),
                                                new Message(MessageType.DATA, new Date(), new Value("id")),
                                                new Message(MessageType.DATA, new Date(), new Value(7)),
                                                new Message(MessageType.DATA, new Date(), new Value(1)),
                                                new Message(MessageType.DATA, new Date(), new Value("childNodes")),
                                                new Message(MessageType.DATA, new Date(), new Value(0)),
                                                new Message(MessageType.DATA, new Date(), new Value(1)),
                                                new Message(MessageType.DATA, new Date(), new Value(4)),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value("name")),
                                                new Message(MessageType.DATA, new Date(), new Value(4)),
                                                new Message(MessageType.DATA, new Date(), new Value("firstname")),
                                                new Message(MessageType.DATA, new Date(), new Value("value")),
                                                new Message(MessageType.DATA, new Date(), new Value(4)),
                                                new Message(MessageType.DATA, new Date(), new Value("yong")),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value("name")),
                                                new Message(MessageType.DATA, new Date(), new Value(4)),
                                                new Message(MessageType.DATA, new Date(), new Value("lastname")),
                                                new Message(MessageType.DATA, new Date(), new Value("value")),
                                                new Message(MessageType.DATA, new Date(), new Value(4)),
                                                new Message(MessageType.DATA, new Date(), new Value("mook kim")),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value("name")),
                                                new Message(MessageType.DATA, new Date(), new Value(4)),
                                                new Message(MessageType.DATA, new Date(), new Value("nickname")),
                                                new Message(MessageType.DATA, new Date(), new Value("value")),
                                                new Message(MessageType.DATA, new Date(), new Value(4)),
                                                new Message(MessageType.DATA, new Date(), new Value("mkyong")),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value("name")),
                                                new Message(MessageType.DATA, new Date(), new Value(4)),
                                                new Message(MessageType.DATA, new Date(), new Value("salary")),
                                                new Message(MessageType.DATA, new Date(), new Value("value")),
                                                new Message(MessageType.DATA, new Date(), new Value(7)),
                                                new Message(MessageType.DATA, new Date(), new Value(100000))
                                        ),
                                        ActionType.EXECUTE
                                ))),
                null,
                null);
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        process.stop();
    }

    @Test
    public void test7() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "type", new Value(ValueType.INTEGER, 3),
                                "charsetName", new Value(ValueType.STRING, "UTF-8"),
                                "param", new Value(ValueType.STRING, " ")

                        ),
                        null,
                        null
                ),
                new ValueTypeConverter()
        );

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value("12kl jh65 j898.98 0.65 -1.56 gfj8799")),
                                                new Message(MessageType.DATA, new Date(), new Value("st54 6868 0.568")),
                                                new Message(MessageType.DATA, new Date(), new Value(" "))
                                        ),
                                        ActionType.EXECUTE
                                ))),
                null,
                null);
        process.fullLifeCycle(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
    }

    @Test
    public void test8() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "type", new Value(ValueType.INTEGER, 17),
                                "charsetName", new Value(ValueType.STRING, "UTF-8"),
                                "param", new Value(ValueType.STRING, "")

                        ),
                        null,
                        null
                ),
                new ValueTypeConverter()
        );

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value("{\"code\": 0, \"data\": {\"date\": 1638901562763, \"ticker\": {\"vol\": \"1883937.15537386\", \"low\": \"0.030440\", \"open\": \"0.030500\", \"high\": \"0.035257\", \"last\": \"0.034216\", \"buy\": \"0.034189\", \"buy_amount\": \"1934.08182333\", \"sell\": \"0.034213\", \"sell_amount\": \"456.49983759\"}}, \"message\": \"OK\"}"))
                                        ),
                                        ActionType.EXECUTE
                                ))),
                null,
                null);
        process.fullLifeCycle(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
    }

    @Test
    public void test9() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "type", new Value(ValueType.INTEGER, 20),
                                "charsetName", new Value(ValueType.STRING, "UTF-8"),
                                "param", new Value(ValueType.STRING, "")

                        ),
                        null,
                        null
                ),
                new ValueTypeConverter()
        );

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value("abc4".getBytes()))
                                        ),
                                        ActionType.EXECUTE
                                ))),
                null,
                null);
        process.fullLifeCycle(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
    }

    @Test
    public void test10() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "type", new Value(ValueType.INTEGER, 19),
                                "charsetName", new Value(ValueType.STRING, "UTF-8"),
                                "param", new Value(ValueType.STRING, "")

                        ),
                        null,
                        null
                ),
                new ValueTypeConverter()
        );

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value("YWJjNA=="))
                                        ),
                                        ActionType.EXECUTE
                                ))),
                null,
                null);
        process.fullLifeCycle(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue() + " " + new String((byte[]) m.getValue())));
    }

    @Test
    public void test11() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "type", new Value(ValueType.INTEGER, 21),
                                "charsetName", new Value(ValueType.STRING, "UTF-8"),
                                "param", new Value(ValueType.STRING, "")

                        ),
                        null,
                        null
                ),
                new ValueTypeConverter()
        );

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(1)),
                                                new Message(MessageType.DATA, new Date(), new Value("123")),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value("456")),
                                                new Message(MessageType.DATA, new Date(), new Value("789")),
                                                new Message(MessageType.DATA, new Date(), new Value(3))
                                        ),
                                        ActionType.EXECUTE
                                ))),
                null,
                null);
        process.fullLifeCycle(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
    }

    @Test
    public void test12() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "type", new Value(ValueType.INTEGER, 21),
                                "charsetName", new Value(ValueType.STRING, "UTF-8"),
                                "param", new Value(ValueType.STRING, "2::name::value")

                        ),
                        null,
                        null
                ),
                new ValueTypeConverter()
        );

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value("test1")),
                                                new Message(MessageType.DATA, new Date(), new Value(1)),
                                                new Message(MessageType.DATA, new Date(), new Value("test2")),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value("test3")),
                                                new Message(MessageType.DATA, new Date(), new Value(3))
                                        ),
                                        ActionType.EXECUTE
                                ))),
                null,
                null);
        process.fullLifeCycle(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
    }

    @Test
    public void test13() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "type", new Value(ValueType.INTEGER, 22),
                                "charsetName", new Value(ValueType.STRING, "UTF-8"),
                                "param", new Value(ValueType.STRING, "2")

                        ),
                        null,
                        null
                ),
                new ValueTypeConverter()
        );

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value("name")),
                                                new Message(MessageType.DATA, new Date(), new Value("test1")),
                                                new Message(MessageType.DATA, new Date(), new Value("value")),
                                                new Message(MessageType.DATA, new Date(), new Value(1)),
                                                new Message(MessageType.DATA, new Date(), new Value("name")),
                                                new Message(MessageType.DATA, new Date(), new Value("test2")),
                                                new Message(MessageType.DATA, new Date(), new Value("value")),
                                                new Message(MessageType.DATA, new Date(), new Value(2))
                                        ),
                                        ActionType.EXECUTE
                                ))),
                null,
                null);
        process.fullLifeCycle(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
    }

    @Test
    public void testOnce() throws IOException {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "type", new Value(ValueType.INTEGER, 17),
                                "charsetName", new Value(ValueType.STRING, "UTF-8"),
                                "param", new Value(ValueType.STRING, "")

                        ),
                        null,
                        null
                ),
                new ValueTypeConverter()
        );

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(StringUtils.join(
                                                        Files.readAllLines(Path.of(new File("C:\\Users\\user\\Documents\\tmp\\test.json").toURI())), "")))
                                        ),
                                        ActionType.EXECUTE
                                ))),
                null,
                null);
        process.fullLifeCycle(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
    }

    @Test
    public void test14() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "type", new Value(ValueType.INTEGER, 13),
                                "charsetName", new Value(ValueType.STRING, "UTF-8"),
                                "param", new Value(ValueType.STRING, "2")

                        ),
                        null,
                        null
                ),
                new ValueTypeConverter()
        );

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(1)),
                                                new Message(MessageType.DATA, new Date(), new Value(1)),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value("dates")),
                                                new Message(MessageType.DATA, new Date(), new Value(0)),
                                                new Message(MessageType.DATA, new Date(), new Value(8)),
                                                new Message(MessageType.DATA, new Date(), new Value(8)),
                                                new Message(MessageType.DATA, new Date(), new Value(123L)),
                                                new Message(MessageType.DATA, new Date(), new Value(123L)),
                                                new Message(MessageType.DATA, new Date(), new Value(123L)),
                                                new Message(MessageType.DATA, new Date(), new Value(123L)),
                                                new Message(MessageType.DATA, new Date(), new Value(123L)),
                                                new Message(MessageType.DATA, new Date(), new Value(123L)),
                                                new Message(MessageType.DATA, new Date(), new Value(123L)),
                                                new Message(MessageType.DATA, new Date(), new Value(123L)),
                                                new Message(MessageType.DATA, new Date(), new Value("params")),
                                                new Message(MessageType.DATA, new Date(), new Value(0)),
                                                new Message(MessageType.DATA, new Date(), new Value(15)),
                                                new Message(MessageType.DATA, new Date(), new Value(3)),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value("date")),
                                                new Message(MessageType.DATA, new Date(), new Value("value")),
                                                new Message(MessageType.DATA, new Date(), new Value(1L)),
                                                new Message(MessageType.DATA, new Date(), new Value("name")),
                                                new Message(MessageType.DATA, new Date(), new Value(1L)),
                                                new Message(MessageType.DATA, new Date(), new Value("name")),
                                                new Message(MessageType.DATA, new Date(), new Value(1L)),
                                                new Message(MessageType.DATA, new Date(), new Value("name"))
                                        ),
                                        ActionType.EXECUTE
                                ))),
                null,
                null);
        process.fullLifeCycle(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
    }

    @Test
    public void test15() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "type", new Value(ValueType.INTEGER, 17),
                                "charsetName", new Value(ValueType.STRING, "UTF-8"),
                                "param", new Value(ValueType.STRING, "10")

                        ),
                        null,
                        null
                ),
                new ValueTypeConverter()
        );
        process.start();

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value("{\"message_type\":\"instrument_snapshot\",\"message\":{\"data\":[{\"id\":2,\"symbol\":\"MSFT\",\"primary_exchange\":\"NASDAQ\",\"exchange\":\"SMART\",\"currency\":\"USD\",\"security_type\":\"STK\",\"amount\":1001.00,\"node_name\":null,\"enabled\":true,\"status\":\"STOP\",\"trading_time_from\":null,\"trading_time_to\":null,\"permanent_triggered\":null,\"tws_symbol\":null,\"node_id\":null},{\"id\":9,\"symbol\":\"NVDA\",\"primary_exchange\":\"NASDAQ\",\"exchange\":\"SMART\",\"currency\":\"USD\",\"security_type\":\"STK\",\"amount\":1001.00,\"node_name\":\"craig_twitch_node_1\",\"enabled\":true,\"status\":\"STOP\",\"trading_time_from\":null,\"trading_time_to\":null,\"permanent_triggered\":null,\"tws_symbol\":null,\"node_id\":null},{\"id\":10,\"symbol\":\"META\",\"primary_exchange\":\"NASDAQ\",\"exchange\":\"SMART\",\"currency\":\"USD\",\"security_type\":\"STK\",\"amount\":1001.00,\"node_name\":null,\"enabled\":true,\"status\":\"STOP\",\"trading_time_from\":null,\"trading_time_to\":null,\"permanent_triggered\":null,\"tws_symbol\":null,\"node_id\":null},{\"id\":3,\"symbol\":\"AAPL\",\"primary_exchange\":\"NASDAQ\",\"exchange\":\"SMART\",\"currency\":\"USD\",\"security_type\":\"STK\",\"amount\":1001.00,\"node_name\":null,\"enabled\":true,\"status\":\"STOP\",\"trading_time_from\":null,\"trading_time_to\":null,\"permanent_triggered\":null,\"tws_symbol\":null,\"node_id\":null},{\"id\":6,\"symbol\":\"TSLA\",\"primary_exchange\":\"NASDAQ\",\"exchange\":\"SMART\",\"currency\":\"USD\",\"security_type\":\"STK\",\"amount\":1001.00,\"node_name\":\"craig_twitch_node_1\",\"enabled\":true,\"status\":\"STOP\",\"trading_time_from\":null,\"trading_time_to\":null,\"permanent_triggered\":null,\"tws_symbol\":null,\"node_id\":null},{\"id\":18,\"symbol\":\"V\",\"primary_exchange\":\"NYSE\",\"exchange\":\"SMART\",\"currency\":\"USD\",\"security_type\":\"STK\",\"amount\":1001.00,\"node_name\":\"craig_twitch_node_1\",\"enabled\":true,\"status\":\"STOP\",\"trading_time_from\":null,\"trading_time_to\":null,\"permanent_triggered\":null,\"tws_symbol\":null,\"node_id\":null},{\"id\":14,\"symbol\":\"JNJ\",\"primary_exchange\":\"NYSE\",\"exchange\":\"SMART\",\"currency\":\"USD\",\"security_type\":\"STK\",\"amount\":1001.00,\"node_name\":null,\"enabled\":true,\"status\":\"STOP\",\"trading_time_from\":null,\"trading_time_to\":null,\"permanent_triggered\":null,\"tws_symbol\":null,\"node_id\":null},{\"id\":15,\"symbol\":\"PG\",\"primary_exchange\":\"NYSE\",\"exchange\":\"SMART\",\"currency\":\"USD\",\"security_type\":\"STK\",\"amount\":1001.00,\"node_name\":\"craig_twitch_node_1\",\"enabled\":true,\"status\":\"STOP\",\"trading_time_from\":null,\"trading_time_to\":null,\"permanent_triggered\":null,\"tws_symbol\":null,\"node_id\":null},{\"id\":23,\"symbol\":\"SPY\",\"primary_exchange\":\"\",\"exchange\":\"SMART\",\"currency\":\"USD\",\"security_type\":\"STK\",\"amount\":1000.00,\"node_name\":null,\"enabled\":true,\"status\":\"STOP\",\"trading_time_from\":[0,0],\"trading_time_to\":[23,59,59],\"permanent_triggered\":null,\"tws_symbol\":null,\"node_id\":null},{\"id\":5,\"symbol\":\"AMZN\",\"primary_exchange\":\"NASDAQ\",\"exchange\":\"SMART\",\"currency\":\"USD\",\"security_type\":\"STK\",\"amount\":1001.00,\"node_name\":null,\"enabled\":true,\"status\":\"STOP\",\"trading_time_from\":null,\"trading_time_to\":null,\"permanent_triggered\":null,\"tws_symbol\":null,\"node_id\":null},{\"id\":16,\"symbol\":\"UNH\",\"primary_exchange\":\"NYSE\",\"exchange\":\"SMART\",\"currency\":\"USD\",\"security_type\":\"STK\",\"amount\":1001.00,\"node_name\":null,\"enabled\":true,\"status\":\"STOP\",\"trading_time_from\":null,\"trading_time_to\":null,\"permanent_triggered\":null,\"tws_symbol\":null,\"node_id\":null}]}}"))
                                        ),
                                        ActionType.EXECUTE
                                ))),
                null,
                null);
        process.execute(executionContextTool);
        for (int i = 0; i < executionContextTool.getOutput().size(); i++) {
            IMessage m = executionContextTool.getOutput().get(i);
            System.out.println(i + " " + m.getMessageType() + " " + m.getValue());
        }
        executionContextTool.getOutput().clear();

        process.execute(executionContextTool);
        for (int i = 0; i < executionContextTool.getOutput().size(); i++) {
            IMessage m = executionContextTool.getOutput().get(i);
            System.out.println(i + " " + m.getMessageType() + " " + m.getValue());
        }

        process.stop();
    }

    @Test
    public void test16() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "type", new Value(29),
                                "charsetName", new Value(ValueType.STRING, "UTF-8"),
                                "param", new Value(ValueType.STRING, " ")

                        ),
                        null,
                        null
                ),
                new ValueTypeConverter()
        );

        process.start();

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                                                        "<company>\ntest2" +
                                                        "    <Staff id=\"1\">\n" +
                                                        "        <firstname>yong</firstname>\n" +
                                                        "        <lastname>mook kim</lastname>\n" +
                                                        "        <nickname>mkyong</nickname>\n" +
                                                        "        <salary>100000</salary>\n" +
                                                        "    </Staff>\n" +
                                                        "</company>"))
                                        ),
                                        ActionType.EXECUTE
                                ))),
                null,
                null);
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        process.stop();
    }

}