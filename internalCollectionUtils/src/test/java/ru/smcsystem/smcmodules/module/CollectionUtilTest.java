package ru.smcsystem.smcmodules.module;

import org.junit.Test;
import ru.smcsystem.api.dto.ObjectArray;
import ru.smcsystem.api.enumeration.ActionType;
import ru.smcsystem.api.enumeration.MessageType;
import ru.smcsystem.api.enumeration.ValueType;
import ru.smcsystem.test.Process;
import ru.smcsystem.test.emulate.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class CollectionUtilTest {

    @Test
    public void process() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "type", new Value("SIZE")
                                , "value", new Value(" ")
                                // , "start", new Value("==start==")
                                // , "end", new Value("==end==")
                        ),
                        null,
                        null
                ),
                new CollectionUtil()
        );
        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(1)),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value(3)),
                                                new Message(MessageType.DATA, new Date(), new Value(4)),
                                                new Message(MessageType.DATA, new Date(), new Value(5))
                                        ),
                                        ActionType.EXECUTE
                                ))), null, null);
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
                                "type", new Value("INDEX_OF")
                                , "value", new Value(" ")
                                // , "start", new Value("==start==")
                                // , "end", new Value("==end==")
                        ),
                        null,
                        null
                ),
                new CollectionUtil()
        );
        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(1)),
                                                new Message(MessageType.DATA, new Date(), new Value(3)),
                                                new Message(MessageType.DATA, new Date(), new Value(5))
                                        ),
                                        ActionType.EXECUTE
                                )),
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(1)),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value(3)),
                                                new Message(MessageType.DATA, new Date(), new Value(4)),
                                                new Message(MessageType.DATA, new Date(), new Value(5))
                                        ),
                                        ActionType.EXECUTE
                                ))
                ), null, null);
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
                                "type", new Value("SUB_LIST")
                                , "value", new Value("1::4")
                                // , "start", new Value("==start==")
                                // , "end", new Value("==end==")
                        ),
                        null,
                        null
                ),
                new CollectionUtil()
        );
        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(1)),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value(3)),
                                                new Message(MessageType.DATA, new Date(), new Value(4)),
                                                new Message(MessageType.DATA, new Date(), new Value(5))
                                        ),
                                        ActionType.EXECUTE
                                ))), null, null);
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
                                "type", new Value("SUB_LIST_EXT")
                                , "value", new Value("2")
                                // , "start", new Value("==start==")
                                // , "end", new Value("==end==")
                        ),
                        null,
                        null
                ),
                new CollectionUtil()
        );
        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(1)),
                                                new Message(MessageType.DATA, new Date(), new Value("test")),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value("test1")),
                                                new Message(MessageType.DATA, new Date(), new Value("test2")),
                                                new Message(MessageType.DATA, new Date(), new Value(3)),
                                                new Message(MessageType.DATA, new Date(), new Value("test3")),
                                                new Message(MessageType.DATA, new Date(), new Value("test4")),
                                                new Message(MessageType.DATA, new Date(), new Value("test5")),
                                                new Message(MessageType.DATA, new Date(), new Value("test6")),
                                                new Message(MessageType.DATA, new Date(), new Value("test7"))
                                        ),
                                        ActionType.EXECUTE
                                ))), null, null);
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
                                "type", new Value("SUBTRACT")
                                , "value", new Value(" ")
                                // , "start", new Value("==start==")
                                // , "end", new Value("==end==")
                        ),
                        null,
                        null
                ),
                new CollectionUtil()
        );
        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(1)),
                                                new Message(MessageType.DATA, new Date(), new Value("test")),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value("test2")),
                                                new Message(MessageType.DATA, new Date(), new Value(3)),
                                                new Message(MessageType.DATA, new Date(), new Value("test3"))
                                        ),
                                        ActionType.EXECUTE
                                )),
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(1)),
                                                new Message(MessageType.DATA, new Date(), new Value("test")),
                                                new Message(MessageType.DATA, new Date(), new Value("test2")),
                                                new Message(MessageType.DATA, new Date(), new Value(3))
                                        ),
                                        ActionType.EXECUTE
                                ))
                ), null, null);
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
                                "type", new Value("SUBTRACT")
                                , "value", new Value("3")
                                // , "start", new Value("==start==")
                                // , "end", new Value("==end==")
                        ),
                        null,
                        null
                ),
                new CollectionUtil()
        );
        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(1)),
                                                new Message(MessageType.DATA, new Date(), new Value("test")),
                                                new Message(MessageType.DATA, new Date(), new Value("test")),
                                                new Message(MessageType.DATA, new Date(), new Value(3)),
                                                new Message(MessageType.DATA, new Date(), new Value("test")),
                                                new Message(MessageType.DATA, new Date(), new Value("test")),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value("test2")),
                                                new Message(MessageType.DATA, new Date(), new Value("test2")),
                                                new Message(MessageType.DATA, new Date(), new Value(4)),
                                                new Message(MessageType.DATA, new Date(), new Value("test2")),
                                                new Message(MessageType.DATA, new Date(), new Value("test2")),
                                                new Message(MessageType.DATA, new Date(), new Value(5))
                                        ),
                                        ActionType.EXECUTE
                                )),
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(1)),
                                                new Message(MessageType.DATA, new Date(), new Value("test")),
                                                new Message(MessageType.DATA, new Date(), new Value("test")),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value("test2")),
                                                new Message(MessageType.DATA, new Date(), new Value("test2")),
                                                new Message(MessageType.DATA, new Date(), new Value(4)),
                                                new Message(MessageType.DATA, new Date(), new Value("test2")),
                                                new Message(MessageType.DATA, new Date(), new Value("test2"))
                                        ),
                                        ActionType.EXECUTE
                                ))
                ), null, null);
        process.fullLifeCycle(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
    }

    @Test
    public void process7() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "type", new Value("TRANSFORM")
                                , "value", new Value("3")
                                // , "start", new Value("==start==")
                                // , "end", new Value("==end==")
                        ),
                        null,
                        null
                ),
                new CollectionUtil()
        );
        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(1)),
                                                new Message(MessageType.DATA, new Date(), new Value("test")),
                                                new Message(MessageType.DATA, new Date(), new Value("test")),
                                                new Message(MessageType.DATA, new Date(), new Value(3)),
                                                new Message(MessageType.DATA, new Date(), new Value("test")),
                                                new Message(MessageType.DATA, new Date(), new Value("test")),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value("test2")),
                                                new Message(MessageType.DATA, new Date(), new Value("test2")),
                                                new Message(MessageType.DATA, new Date(), new Value(4)),
                                                new Message(MessageType.DATA, new Date(), new Value("test2")),
                                                new Message(MessageType.DATA, new Date(), new Value("test2")),
                                                new Message(MessageType.DATA, new Date(), new Value(5))
                                        ),
                                        ActionType.EXECUTE
                                ))
                ),
                null,
                null,
                List.of(
                        args -> {
                            int i = ((Number) (args.get(0))).intValue();
                            return new Action(
                                    List.of(
                                            new Message(MessageType.DATA, new Date(), new Value(i)),
                                            new Message(MessageType.DATA, new Date(), new Value("test" + i))
                                    ),
                                    ActionType.EXECUTE
                            );
                        }));
        process.fullLifeCycle(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
    }

    @Test
    public void process8() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "type", new Value("MAP_GET_VALUE_EXT")
                                , "value", new Value("test1::test3::test4;;3::from")
                                // , "start", new Value("==start==")
                                // , "end", new Value("==end==")
                        ),
                        null,
                        null
                ),
                new CollectionUtil()
        );
        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(1)),
                                                new Message(MessageType.DATA, new Date(), new Value("test1=value1")),
                                                new Message(MessageType.DATA, new Date(), new Value("test2=value2")),
                                                new Message(MessageType.DATA, new Date(), new Value("test")),
                                                new Message(MessageType.DATA, new Date(), new Value(3)),
                                                new Message(MessageType.DATA, new Date(), new Value("test3=value3")),
                                                new Message(MessageType.DATA, new Date(), new Value("from=1672995741000")),
                                                new Message(MessageType.DATA, new Date(), new Value(5))
                                        ),
                                        ActionType.EXECUTE
                                ))
                ), null, null);
        process.fullLifeCycle(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getType() + " " + m.getValue()));
    }

    @Test
    public void process9() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "type", new Value("INDEX_OF_VALUE_REGEXP")
                                , "value", new Value("^test\\d.*$")
                                // , "start", new Value("==start==")
                                // , "end", new Value("==end==")
                        ),
                        null,
                        null
                ),
                new CollectionUtil()
        );
        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(1)),
                                                new Message(MessageType.DATA, new Date(), new Value("test1=value1")),
                                                new Message(MessageType.DATA, new Date(), new Value("test2=value2")),
                                                new Message(MessageType.DATA, new Date(), new Value("test")),
                                                new Message(MessageType.DATA, new Date(), new Value(3)),
                                                new Message(MessageType.DATA, new Date(), new Value("test3=value3")),
                                                new Message(MessageType.DATA, new Date(), new Value(5))
                                        ),
                                        ActionType.EXECUTE
                                ))
                ), null, null);
        process.fullLifeCycle(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
    }


    @Test
    public void process10() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "type", new Value("MAP_GET_VALUE_OBJECT_LIST")
                                , "value", new Value("field1::field3")
                                // , "start", new Value("==start==")
                                // , "end", new Value("==end==")
                        ),
                        null,
                        null
                ),
                new CollectionUtil()
        );
        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(1)),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value("field1")),
                                                new Message(MessageType.DATA, new Date(), new Value(3)),
                                                new Message(MessageType.DATA, new Date(), new Value("value1")),
                                                new Message(MessageType.DATA, new Date(), new Value("field2")),
                                                new Message(MessageType.DATA, new Date(), new Value(0)),
                                                new Message(MessageType.DATA, new Date(), new Value(1)),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value("f1")),
                                                new Message(MessageType.DATA, new Date(), new Value(3)),
                                                new Message(MessageType.DATA, new Date(), new Value("v1")),
                                                new Message(MessageType.DATA, new Date(), new Value("f2")),
                                                new Message(MessageType.DATA, new Date(), new Value(3)),
                                                new Message(MessageType.DATA, new Date(), new Value("v2")),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value("f1")),
                                                new Message(MessageType.DATA, new Date(), new Value(3)),
                                                new Message(MessageType.DATA, new Date(), new Value("v3")),
                                                new Message(MessageType.DATA, new Date(), new Value("f2")),
                                                new Message(MessageType.DATA, new Date(), new Value(3)),
                                                new Message(MessageType.DATA, new Date(), new Value("v4")),
                                                new Message(MessageType.DATA, new Date(), new Value(1)),
                                                new Message(MessageType.DATA, new Date(), new Value("field3")),
                                                new Message(MessageType.DATA, new Date(), new Value(3)),
                                                new Message(MessageType.DATA, new Date(), new Value(5))
                                        ),
                                        ActionType.EXECUTE
                                ))
                ), null, null);
        process.fullLifeCycle(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
    }

    @Test
    public void process11() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "type", new Value("MAP_GET_VALUE_OBJECT_LIST")
                                , "value", new Value("rows::id;;1")
                                // , "start", new Value("==start==")
                                // , "end", new Value("==end==")
                        ),
                        null,
                        null
                ),
                new CollectionUtil()
        );
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
                                                new Message(MessageType.DATA, new Date(), new Value("Sheet1")),
                                                new Message(MessageType.DATA, new Date(), new Value("count")),
                                                new Message(MessageType.DATA, new Date(), new Value(7)),
                                                new Message(MessageType.DATA, new Date(), new Value(3)),
                                                new Message(MessageType.DATA, new Date(), new Value("rows")),
                                                new Message(MessageType.DATA, new Date(), new Value(0)),
                                                new Message(MessageType.DATA, new Date(), new Value(0)),
                                                new Message(MessageType.DATA, new Date(), new Value(3)),
                                                new Message(MessageType.DATA, new Date(), new Value(3)),
                                                new Message(MessageType.DATA, new Date(), new Value(6)),
                                                new Message(MessageType.DATA, new Date(), new Value("param1")),
                                                new Message(MessageType.DATA, new Date(), new Value(1.d)),
                                                new Message(MessageType.DATA, new Date(), new Value("param2")),
                                                new Message(MessageType.DATA, new Date(), new Value(2.d)),
                                                new Message(MessageType.DATA, new Date(), new Value("param3")),
                                                new Message(MessageType.DATA, new Date(), new Value(3.d)),
                                                new Message(MessageType.DATA, new Date(), new Value(3)),
                                                new Message(MessageType.DATA, new Date(), new Value(6)),
                                                new Message(MessageType.DATA, new Date(), new Value("param1")),
                                                new Message(MessageType.DATA, new Date(), new Value(4.d)),
                                                new Message(MessageType.DATA, new Date(), new Value("param2")),
                                                new Message(MessageType.DATA, new Date(), new Value(5.d)),
                                                new Message(MessageType.DATA, new Date(), new Value("param3")),
                                                new Message(MessageType.DATA, new Date(), new Value(6.d)),
                                                new Message(MessageType.DATA, new Date(), new Value(3)),
                                                new Message(MessageType.DATA, new Date(), new Value(6)),
                                                new Message(MessageType.DATA, new Date(), new Value("param1")),
                                                new Message(MessageType.DATA, new Date(), new Value(7.d)),
                                                new Message(MessageType.DATA, new Date(), new Value("param2")),
                                                new Message(MessageType.DATA, new Date(), new Value(8.d)),
                                                new Message(MessageType.DATA, new Date(), new Value("param3")),
                                                new Message(MessageType.DATA, new Date(), new Value(9.d))
                                        ),
                                        ActionType.EXECUTE
                                ))
                ), null, null);
        process.fullLifeCycle(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
    }

    @Test
    public void process12() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "type", new Value("OBJECT_LIST_SET_FIELD")
                                , "value", new Value("field2")
                                // , "start", new Value("==start==")
                                // , "end", new Value("==end==")
                        ),
                        null,
                        null
                ),
                new CollectionUtil()
        );
        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(1)),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value("field1")),
                                                new Message(MessageType.DATA, new Date(), new Value(3)),
                                                new Message(MessageType.DATA, new Date(), new Value("value1")),
                                                new Message(MessageType.DATA, new Date(), new Value("field2")),
                                                new Message(MessageType.DATA, new Date(), new Value(0)),
                                                new Message(MessageType.DATA, new Date(), new Value(1)),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value("f1")),
                                                new Message(MessageType.DATA, new Date(), new Value(3)),
                                                new Message(MessageType.DATA, new Date(), new Value("v1")),
                                                new Message(MessageType.DATA, new Date(), new Value("f2")),
                                                new Message(MessageType.DATA, new Date(), new Value(3)),
                                                new Message(MessageType.DATA, new Date(), new Value("v2")),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value("f1")),
                                                new Message(MessageType.DATA, new Date(), new Value(3)),
                                                new Message(MessageType.DATA, new Date(), new Value("v3")),
                                                new Message(MessageType.DATA, new Date(), new Value("f2")),
                                                new Message(MessageType.DATA, new Date(), new Value(3)),
                                                new Message(MessageType.DATA, new Date(), new Value("v4")),
                                                new Message(MessageType.DATA, new Date(), new Value(1)),
                                                new Message(MessageType.DATA, new Date(), new Value("field3")),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value(5))
                                        ),
                                        ActionType.EXECUTE
                                )
                        ),
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(1)),
                                                new Message(MessageType.DATA, new Date(), new Value(1)),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value("f11")),
                                                new Message(MessageType.DATA, new Date(), new Value(3)),
                                                new Message(MessageType.DATA, new Date(), new Value("v1")),
                                                new Message(MessageType.DATA, new Date(), new Value("f21")),
                                                new Message(MessageType.DATA, new Date(), new Value(3)),
                                                new Message(MessageType.DATA, new Date(), new Value("v2"))
                                        ),
                                        ActionType.EXECUTE
                                )
                        )
                ), null, null);
        process.fullLifeCycle(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
    }

    @Test
    public void process13() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "type", new Value("FILTER_OBJECT_LIST")
                                , "value", new Value("")
                                // , "start", new Value("==start==")
                                // , "end", new Value("==end==")
                        ),
                        null,
                        null
                ),
                new CollectionUtil()
        );
        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(1)),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value("field1")),
                                                new Message(MessageType.DATA, new Date(), new Value(3)),
                                                new Message(MessageType.DATA, new Date(), new Value("value1")),
                                                new Message(MessageType.DATA, new Date(), new Value("field2")),
                                                new Message(MessageType.DATA, new Date(), new Value(0)),
                                                new Message(MessageType.DATA, new Date(), new Value(1)),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value("f1")),
                                                new Message(MessageType.DATA, new Date(), new Value(3)),
                                                new Message(MessageType.DATA, new Date(), new Value("v1")),
                                                new Message(MessageType.DATA, new Date(), new Value("f2")),
                                                new Message(MessageType.DATA, new Date(), new Value(3)),
                                                new Message(MessageType.DATA, new Date(), new Value("v2")),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value("f1")),
                                                new Message(MessageType.DATA, new Date(), new Value(3)),
                                                new Message(MessageType.DATA, new Date(), new Value("v3")),
                                                new Message(MessageType.DATA, new Date(), new Value("f2")),
                                                new Message(MessageType.DATA, new Date(), new Value(3)),
                                                new Message(MessageType.DATA, new Date(), new Value("v4")),
                                                new Message(MessageType.DATA, new Date(), new Value(1)),
                                                new Message(MessageType.DATA, new Date(), new Value("field3")),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value(5))
                                        ),
                                        ActionType.EXECUTE
                                )
                        )
                ),
                null,
                List.of(
                        new Action(
                                List.of(
                                        new Message(MessageType.DATA, new Date(), new Value(1))
                                ),
                                ActionType.EXECUTE
                        )
                ),
                List.of(
                        params -> {
                            if (params.size() > 7 || params.size()==1 && params.get(0) instanceof ObjectArray) {
                                return new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(1))
                                        ),
                                        ActionType.EXECUTE
                                );
                            }
                            return new Action(
                                    new ArrayList<>(),
                                    ActionType.EXECUTE
                            );
                        }
                )
        );
        process.fullLifeCycle(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
    }

    @Test
    public void process14() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "type", new Value("INDEX_OF_OR_NULL")
                                , "value", new Value("")
                                // , "start", new Value("==start==")
                                // , "end", new Value("==end==")
                        ),
                        null,
                        null
                ),
                new CollectionUtil()
        );

        process.start();

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(1))
                                        ),
                                        ActionType.EXECUTE)
                        ),
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(1.f)),
                                                new Message(MessageType.DATA, new Date(), new Value("free"))
                                        ),
                                        ActionType.EXECUTE)
                        )
                ), null, null);
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        process.stop();
    }

    @Test
    public void process15() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "type", new Value("LIST_ARRAY_GET")
                                , "value", new Value("1::1::3")
                                // , "start", new Value("==start==")
                                // , "end", new Value("==end==")
                        ),
                        null,
                        null
                ),
                new CollectionUtil()
        );

        process.start();

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(0)),
                                                new Message(MessageType.DATA, new Date(), new Value(4)),
                                                new Message(MessageType.DATA, new Date(), new Value(4)),
                                                new Message(MessageType.DATA, new Date(), new Value(3)),
                                                new Message(MessageType.DATA, new Date(), new Value("value1")),
                                                new Message(MessageType.DATA, new Date(), new Value("value2")),
                                                new Message(MessageType.DATA, new Date(), new Value("value3")),
                                                new Message(MessageType.DATA, new Date(), new Value(3)),
                                                new Message(MessageType.DATA, new Date(), new Value(4)),
                                                new Message(MessageType.DATA, new Date(), new Value("value4")),
                                                new Message(MessageType.DATA, new Date(), new Value(5)),
                                                new Message(MessageType.DATA, new Date(), new Value("value6")),
                                                new Message(MessageType.DATA, new Date(), new Value("value7")),
                                                new Message(MessageType.DATA, new Date(), new Value(3)),
                                                new Message(MessageType.DATA, new Date(), new Value(4)),
                                                new Message(MessageType.DATA, new Date(), new Value(8)),
                                                new Message(MessageType.DATA, new Date(), new Value(9)),
                                                new Message(MessageType.DATA, new Date(), new Value(10)),
                                                new Message(MessageType.DATA, new Date(), new Value("value11"))
                                        ),
                                        ActionType.EXECUTE)
                        )
                ), null, null);
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        process.stop();
    }

    @Test
    public void process16() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "type", new Value("MAP_GET_VALUE_OBJECT_LIST_PATH")
                                , "value", new Value("data.ticker.low::data.ticker.buy::data.ticker")
                                // , "start", new Value("==start==")
                                // , "end", new Value("==end==")
                        ),
                        null,
                        null
                ),
                new CollectionUtil()
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
                                                new Message(MessageType.DATA, new Date(), new Value("code")),
                                                new Message(MessageType.DATA, new Date(), new Value(7)),
                                                new Message(MessageType.DATA, new Date(), new Value(0)),
                                                new Message(MessageType.DATA, new Date(), new Value("data")),
                                                new Message(MessageType.DATA, new Date(), new Value(1)),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value("date")),
                                                new Message(MessageType.DATA, new Date(), new Value(8)),
                                                new Message(MessageType.DATA, new Date(), new Value(1638901562763L)),
                                                new Message(MessageType.DATA, new Date(), new Value("ticker")),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value(9)),
                                                new Message(MessageType.DATA, new Date(), new Value("vol")),
                                                new Message(MessageType.DATA, new Date(), new Value(1883937.15537386)),
                                                new Message(MessageType.DATA, new Date(), new Value("high")),
                                                new Message(MessageType.DATA, new Date(), new Value(0.035257)),
                                                new Message(MessageType.DATA, new Date(), new Value("last")),
                                                new Message(MessageType.DATA, new Date(), new Value(0.034216)),
                                                new Message(MessageType.DATA, new Date(), new Value("low")),
                                                new Message(MessageType.DATA, new Date(), new Value(0.030440)),
                                                new Message(MessageType.DATA, new Date(), new Value("buy")),
                                                new Message(MessageType.DATA, new Date(), new Value(0.034189)),
                                                new Message(MessageType.DATA, new Date(), new Value("sell")),
                                                new Message(MessageType.DATA, new Date(), new Value(0.034213)),
                                                new Message(MessageType.DATA, new Date(), new Value("sell_amount")),
                                                new Message(MessageType.DATA, new Date(), new Value(456.49983759)),
                                                new Message(MessageType.DATA, new Date(), new Value("buy_amount")),
                                                new Message(MessageType.DATA, new Date(), new Value(1934.08182333)),
                                                new Message(MessageType.DATA, new Date(), new Value("open")),
                                                new Message(MessageType.DATA, new Date(), new Value(0.030500)),
                                                new Message(MessageType.DATA, new Date(), new Value("message")),
                                                new Message(MessageType.DATA, new Date(), new Value(4)),
                                                new Message(MessageType.DATA, new Date(), new Value("OK"))
                                        ),
                                        ActionType.EXECUTE)
                        )
                ), null, null);
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        process.stop();
    }

    @Test
    public void process18() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "type", new Value("MAP_GET_VALUE_EXT_REGEXP")
                                , "value", new Value("test1::test[34]_.*")
                                // , "start", new Value("==start==")
                                // , "end", new Value("==end==")
                        ),
                        null,
                        null
                ),
                new CollectionUtil()
        );
        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(1)),
                                                new Message(MessageType.DATA, new Date(), new Value("test1=value1")),
                                                new Message(MessageType.DATA, new Date(), new Value("test1_v1=value11")),
                                                new Message(MessageType.DATA, new Date(), new Value("test2=value2")),
                                                new Message(MessageType.DATA, new Date(), new Value("test")),
                                                new Message(MessageType.DATA, new Date(), new Value(3)),
                                                new Message(MessageType.DATA, new Date(), new Value("test3_v1=value31")),
                                                new Message(MessageType.DATA, new Date(), new Value("test3_v2=value32")),
                                                new Message(MessageType.DATA, new Date(), new Value("test4_v1=value41")),
                                                new Message(MessageType.DATA, new Date(), new Value(5))
                                        ),
                                        ActionType.EXECUTE
                                ))
                ), null, null);
        process.fullLifeCycle(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
    }

}
