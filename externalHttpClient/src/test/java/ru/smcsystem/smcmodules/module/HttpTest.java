package ru.smcsystem.smcmodules.module;

import org.junit.Test;
import ru.smcsystem.api.enumeration.ActionType;
import ru.smcsystem.api.enumeration.MessageType;
import ru.smcsystem.test.Process;
import ru.smcsystem.test.emulate.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class HttpTest {

    @Test
    public void process() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "cookies", new Value(" "),
                                "userAuth", new Value("false"),
                                "username", new Value(" "),
                                "password", new Value(" "),
                                "charset", new Value(""),
                                "urlPrefix", new Value("")
                        ),
                        null,
                        null
                ),
                new Http()
        );

        process.start();

        ExecutionContextToolImpl executionContextTool;
        executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(0)),
                                                new Message(MessageType.DATA, new Date(), new Value("http://www.google.com/search?q=httpClient"))
                                        ),
                                        ActionType.EXECUTE
                                ))),
                null, null);
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        /*
        executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(0)),
                                                new Message(MessageType.DATA, new Date(), new Value("https://en0ecfpncww2zk.x.pipedream.net"))
                                                ),
                                        ActionType.EXECUTE
                                ))),
                null, null);
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(1)),
                                                new Message(MessageType.DATA, new Date(), new Value("https://en0ecfpncww2zk.x.pipedream.net")),
                                                new Message(MessageType.DATA, new Date(), new Value(1)),
                                                new Message(MessageType.DATA, new Date(), new Value("test=test")),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value("testparam")),
                                                new Message(MessageType.DATA, new Date(), new Value("test2"))
                                        ),
                                        ActionType.EXECUTE
                                ))),
                null, null);
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        */

        executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(0)),
                                                new Message(MessageType.DATA, new Date(), new Value("https://postb.in/1575027215056-8979481202550"))
                                        ),
                                        ActionType.EXECUTE
                                ))),
                null, null);
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(1)),
                                                new Message(MessageType.DATA, new Date(), new Value("https://postb.in/1575027215056-8979481202550")),
                                                new Message(MessageType.DATA, new Date(), new Value(1)),
                                                new Message(MessageType.DATA, new Date(), new Value("testHeader=test1")),
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value("testparam")),
                                                new Message(MessageType.DATA, new Date(), new Value("test2"))
                                        ),
                                        ActionType.EXECUTE
                                ))),
                null, null);
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        /*
        executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(0)),
                                                new Message(MessageType.DATA, new Date(), new Value("https://localhost:8080/test"))
                                        ),
                                        ActionType.EXECUTE
                                ))),
                null, null);
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        */

        process.stop();
    }

    @Test
    public void process2() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "cookies", new Value(" "),
                                "userAuth", new Value("false"),
                                "username", new Value(" "),
                                "password", new Value(" "),
                                "charset", new Value(""),
                                "urlPrefix", new Value("")
                        ),
                        null,
                        null
                ),
                new Http()
        );

        process.start();

        ExecutionContextToolImpl executionContextTool;
        executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(1)),
                                                new Message(MessageType.DATA, new Date(), new Value("https://localhost:8080/chat/send")),
                                                new Message(MessageType.DATA, new Date(), new Value("[{nickname: \"chatBot\", message: \"understand\"}]"))
                                        ),
                                        ActionType.EXECUTE
                                ))),
                null, null);
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        process.stop();
    }

    @Test
    public void process3() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "cookies", new Value(" "),
                                "userAuth", new Value("false"),
                                "username", new Value(" "),
                                "password", new Value(" "),
                                "charset", new Value(""),
                                "urlPrefix", new Value("")
                        ),
                        null,
                        null
                ),
                new Http()
        );

        process.start();

        ExecutionContextToolImpl executionContextTool;
        executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(0)),
                                                new Message(MessageType.DATA, new Date(), new Value("https://www.google.com/"))
                                        ),
                                        ActionType.EXECUTE
                                ))),
                null, null);
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        process.stop();
    }

    @Test
    public void process4() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "cookies", new Value(" "),
                                "userAuth", new Value("false"),
                                "username", new Value(" "),
                                "password", new Value(" "),
                                "charset", new Value(""),
                                "urlPrefix", new Value("")
                        ),
                        null,
                        null
                ),
                new Http()
        );

        process.start();

        ExecutionContextToolImpl executionContextTool;
        executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(0)),
                                                new Message(MessageType.DATA, new Date(), new Value("http://kakoysegodnyaprazdnik.ru/"))
                                        ),
                                        ActionType.EXECUTE
                                ))),
                null, null);
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        process.stop();
    }

    @Test
    public void process5() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "cookies", new Value(" "),
                                "userAuth", new Value("false"),
                                "username", new Value(" "),
                                "password", new Value(" "),
                                "charset", new Value(""),
                                "urlPrefix", new Value("")
                        ),
                        null,
                        null
                ),
                new Http()
        );

        process.start();

        ExecutionContextToolImpl executionContextTool;
        executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(1))
                                                , new Message(MessageType.DATA, new Date(), new Value("https://en53vqstah7326jsrf.m.pipedream.net"))
                                                // , new Message(MessageType.DATA, new Date(), new Value("[{nickname: \"chatBot\", message: \"understand\"}]"))
                                                , new Message(MessageType.DATA, new Date(), new Value(0))
                                                // , new Message(MessageType.DATA, new Date(), new Value("testHeader=test1"))
                                                , new Message(MessageType.DATA, new Date(), new Value(2))
                                                , new Message(MessageType.DATA, new Date(), new Value("testparam"))
                                                , new Message(MessageType.DATA, new Date(), new Value("test2"))
                                        ),
                                        ActionType.EXECUTE
                                ))),
                null, null);
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        process.stop();
    }

    @Test
    public void process6() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "cookies", new Value(" "),
                                "userAuth", new Value("false"),
                                "username", new Value(" "),
                                "password", new Value(" "),
                                "charset", new Value(""),
                                "urlPrefix", new Value("http://www.google.com")
                        ),
                        null,
                        null
                ),
                new Http()
        );

        process.start();

        ExecutionContextToolImpl executionContextTool;
        executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(0)),
                                                new Message(MessageType.DATA, new Date(), new Value("/search?q=httpClient"))
                                        ),
                                        ActionType.EXECUTE
                                ))),
                null, null);
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        process.stop();
    }

}
