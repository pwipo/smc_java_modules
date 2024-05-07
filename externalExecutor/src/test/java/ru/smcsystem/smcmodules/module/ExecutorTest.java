package ru.smcsystem.smcmodules.module;

import org.junit.Test;
import ru.smcsystem.api.enumeration.ActionType;
import ru.smcsystem.api.enumeration.MessageType;
import ru.smcsystem.test.Process;
import ru.smcsystem.test.emulate.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class ExecutorTest {

    @Test
    public void process() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "commandPath", new Value("C:\\Users\\user\\Documents\\tmp\\test.bat"),
                                "workDirectory", new Value(" "),
                                "args", new Value("test1::test2::test3"),
                                "encoding", new Value("cp866"),
                                "maxWorkTime", new Value(-1)
                        ),
                        null,
                        null
                ),
                new Executor()
        );
        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(null, null, null);
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
                                "commandPath", new Value("C:\\Windows\\System32\\ping.exe"),
                                "workDirectory", new Value(" "),
                                "args", new Value("8.8.8.8"),
                                "encoding", new Value("cp866"),
                                "maxWorkTime", new Value(10000),
                                "sleepTimeInterval", new Value(50)
                        ),
                        null,
                        null
                ),
                new Executor()
        );
        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(null, null, null);
        process.fullLifeCycle(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
    }

    @Test
    public void processDynamic() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "commandPath", new Value(" "),
                                "workDirectory", new Value(" "),
                                "args", new Value(" "),
                                "encoding", new Value("cp866"),
                                "maxWorkTime", new Value(-1)
                        ),
                        null,
                        null
                ),
                new Executor()
        );
        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value("C:\\Users\\user\\Documents\\tmp\\test.bat")),
                                                new Message(MessageType.DATA, new Date(), new Value("test4")),
                                                new Message(MessageType.DATA, new Date(), new Value("test5")),
                                                new Message(MessageType.DATA, new Date(), new Value("test6"))
                                        ),
                                        ActionType.EXECUTE
                                )))
                , null, null);
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
                                "commandPath", new Value("C:\\Program Files\\MetaTrader 5\\terminal64.exe"),
                                "workDirectory", new Value("C:\\Program Files\\MetaTrader 5"),
                                "args", new Value("/config:C:\\tmp\\auto_mt5.ini"),
                                "encoding", new Value("cp866"),
                                "maxWorkTime", new Value(-1)
                        ),
                        null,
                        null
                ),
                new Executor()
        );
        process.start();
        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(null, null, null);
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        process.stop();
    }

}
