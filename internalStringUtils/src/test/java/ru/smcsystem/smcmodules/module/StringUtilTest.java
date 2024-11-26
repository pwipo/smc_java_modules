package ru.smcsystem.smcmodules.module;

import org.junit.Test;
import ru.smcsystem.api.dto.IAction;
import ru.smcsystem.api.enumeration.ActionType;
import ru.smcsystem.api.enumeration.MessageType;
import ru.smcsystem.api.enumeration.ValueType;
import ru.smcsystem.test.Process;
import ru.smcsystem.test.emulate.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class StringUtilTest {

    @Test
    public void process() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "type", new Value(ValueType.STRING, "JOIN"),
                                "value", new Value(ValueType.STRING, " , ")
                        ),
                        null,
                        null
                ),
                new StringUtil()
        );

        process.start();

        ExecutionContextToolImpl executionContextTool;

        execute(
                process,
                "JOIN",
                " , ",
                List.of(
                        List.of(
                                new Action(
                                        List.of(new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "one"))),
                                        ActionType.EXECUTE)
                        ),
                        List.of(
                                new Action(
                                        List.of(new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "two"))),
                                        ActionType.EXECUTE)
                        ),
                        List.of(
                                new Action(
                                        List.of(new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "free"))),
                                        ActionType.EXECUTE)
                        )
                ));

        execute(
                process,
                "SPLIT",
                " ",
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "one two free"))
                                        ),
                                        ActionType.EXECUTE)
                        )
                ));

        execute(
                process,
                "APPEND",
                " hi",
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "house")),
                                                new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "dog"))
                                        ),
                                        ActionType.EXECUTE)
                        )
                ));

        executionContextTool = execute(
                process,
                "APPEND_FIRST",
                "ih ",
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "/files/1.txt")),
                                                new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "house")),
                                                new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "dog"))
                                        ),
                                        ActionType.EXECUTE)
                        )
                ));

        process.getConfigurationTool().getAllSettings().put("type", new Value(ValueType.STRING, "REPLACE"));
        process.getConfigurationTool().getAllSettings().put("value", new Value(ValueType.STRING, "/files/::"));

        process.update();
        executionContextTool.getOutput().clear();

        process.execute(executionContextTool);//.forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().clear();

        process.getConfigurationTool().getAllSettings().put("type", new Value(ValueType.STRING, "REPLACE_REGEXP"));
        process.getConfigurationTool().getAllSettings().put("value", new Value(ValueType.STRING, "/files::"));

        process.update();
        executionContextTool.getOutput().clear();

        process.execute(executionContextTool);//.forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().clear();

        execute(
                process,
                "REPLACE_REGEXP_GROUP_ONE",
                "(?m)^ipaddr:(.*)$::0.0.0.0",
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "start\n" +
                                                        "abc\n" +
                                                        "123\n" +
                                                        "ipaddr: 1.0.0.1\n" +
                                                        "def\n" +
                                                        "ipaddr: 1.0.0.2\n" +
                                                        "end"))
                                        ),
                                        ActionType.EXECUTE)
                        )
                ));

        execute(
                process,
                "REPLACE_REGEXP_GROUP_ONE",
                "(?m)^input int timeFrameOffset=(-?\\d+);.*$::-2",
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "input string BaseName=NULL;               // base name \"APPLE.\";\n" +
                                                        "input int timeZoneOffset=-300;            // time zone offset in minutes\n" +
                                                        "input int timeFrameOffset=-1;             // time frame offset in minutes\n" +
                                                        "input string symbolsWithTimeFrameOffset=\"\";   // symbols for which a separate symbol is created with a timeframe offset\n" +
                                                        "\n" +
                                                        "string symbolsWithTimeFrameOffsetArr[];\n" +
                                                        "\n"))
                                        ),
                                        ActionType.EXECUTE)
                        )
                ));

        process.getConfigurationTool().getAllSettings().put("type", new Value(ValueType.STRING, "CONTAIN"));
        process.getConfigurationTool().getAllSettings().put("value", new Value(ValueType.STRING, "hi::house::dog"));

        process.update();
        executionContextTool.getOutput().clear();

        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().clear();

        execute(
                process,
                "MATCH",
                "/main.html::/.*",
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "/")),
                                                new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "/main")),
                                                new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "/main.html"))
                                        ),
                                        ActionType.EXECUTE)
                        )
                ));

        execute(
                process,
                "PLACEHOLDERS",
                "one {0} two {1} three {2}",
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 1)),
                                                new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "2")),
                                                new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 3))
                                        ),
                                        ActionType.EXECUTE)
                        )
                ));

        execute(
                process,
                "PLACEHOLDERS_OR_EMPTY",
                "one {0} two {1} three {2}",
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 1)),
                                                new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "2"))
                                        ),
                                        ActionType.EXECUTE)
                        )
                ));

        execute(
                process,
                "FILTER_SIZE",
                "5",
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "1234567890")),
                                                new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "123")),
                                                new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "12345"))
                                        ),
                                        ActionType.EXECUTE)
                        )
                ));

        execute(
                process,
                "REGEXP_GROUP_ONE",
                "12 (\\S+) 345",
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "12 test1 345 12 test2 345")),
                                                new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "1234567890")),
                                                new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "123")),
                                                new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "34512 test3 345"))
                                        ),
                                        ActionType.EXECUTE)
                        )
                ));

        process.stop();
    }

    @Test
    public void process2() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "type", new Value(ValueType.STRING, "SUBSTRING"),
                                "value", new Value(ValueType.STRING, "1::3")
                        ),
                        null,
                        null
                ),
                new StringUtil()
        );

        process.start();

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value("one"))
                                                , new Message(MessageType.DATA, new Date(), new Value("two"))
                                                , new Message(MessageType.DATA, new Date(), new Value("three"))
                                        ),
                                        ActionType.EXECUTE)
                        )
                ), null, null);
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        process.stop();
    }

    private ExecutionContextToolImpl execute(Process process, String type, String value, List<List<IAction>> input) {
        process.getConfigurationTool().getAllSettings().put("type", new Value(ValueType.STRING, type));
        process.getConfigurationTool().getAllSettings().put("value", new Value(ValueType.STRING, value));

        process.update();

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(input, null, null);

        process.execute(executionContextTool);

        System.out.println(type + " " + value);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().clear();

        return executionContextTool;
    }

    @Test
    public void testPlaceHoldersStream() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "type", new Value(ValueType.STRING, "JOIN"),
                                "value", new Value(ValueType.STRING, " , ")
                        ),
                        null,
                        null
                ),
                new StringUtil()
        );

        process.start();

        execute(
                process,
                "PLACEHOLDERS_STREAM",
                "one {0} two {1}",
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(new Value(1)),
                                                new Message(new Value("2")),
                                                new Message(new Value(3)),
                                                new Message(new Value(4)),
                                                new Message(new Value("5")),
                                                new Message(new Value(6))
                                        ),
                                        ActionType.EXECUTE)
                        )
                ));

        process.stop();
    }
}