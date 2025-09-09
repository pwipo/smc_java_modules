package ru.smcsystem.smcmodules.module;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import ru.smcsystem.api.enumeration.ActionType;
import ru.smcsystem.api.enumeration.MessageType;
import ru.smcsystem.api.enumeration.ValueType;
import ru.smcsystem.test.Process;
import ru.smcsystem.test.emulate.*;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class FileTest {

    @Test
    public void process() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of("rootFolder", new Value(ValueType.STRING, " "),
                                "type", new Value(ValueType.STRING, "dir"),
                                "hashAlgType", new Value(ValueType.STRING, "SHA256"),
                                "arguments", new Value(ValueType.STRING, ""),
                                "printAbsolutePath", new Value(ValueType.STRING, "false"),
                                "useOnlyWorkDirectory", new Value(ValueType.STRING, "false")
                        ),
                        null,
                        "C:\\tmp"
                ),
                new File()
        );

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                /*List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, ""))
                                        ),
                                        ActionType.EXECUTE
                                ))),*/
                null,
                null,
                null);
        process.fullLifeCycle(executionContextTool);//.forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
    }

    @Test
    public void process2() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of("rootFolder", new Value(ValueType.STRING, "C:\\tmp\\DB-db-360\\install.sql"),
                                "type", new Value(ValueType.STRING, "readTextPart"),
                                "hashAlgType", new Value(ValueType.STRING, "SHA256"),
                                "arguments", new Value(ValueType.STRING, "100,30"),
                                "printAbsolutePath", new Value(ValueType.STRING, "false"),
                                "useOnlyWorkDirectory", new Value(ValueType.STRING, "false")
                        ),
                        null,
                        "C:\\tmp"
                ),
                new File()
        );

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(null, null, null);
        process.fullLifeCycle(executionContextTool);//.forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
    }

    @Test
    public void writePart() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of("rootFolder", new Value(ValueType.STRING, "C:\\tmp\\DB-db-34\\install2.sql"),
                                "type", new Value(ValueType.STRING, "writePart"),
                                "hashAlgType", new Value(ValueType.STRING, "SHA256"),
                                "arguments", new Value(ValueType.STRING, "20,test,test,egfd,тест"),
                                "printAbsolutePath", new Value(ValueType.STRING, "false"),
                                "useOnlyWorkDirectory", new Value(ValueType.STRING, "false")
                        ),
                        null,
                        "C:\\tmp"
                ),
                new File()
        );
        process.start();

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(null, null, null);
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        executionContextTool = new ExecutionContextToolImpl(List.of(List.of(
                new Action(List.of(
                        new Message(new Value("1-test")),
                        new Message(new Value(-1))
                ))
        )), null, null);
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
                        Map.of("rootFolder", new Value(ValueType.STRING, "C:\\tmp\\DB-db-34\\install2.sql"),
                                "type", new Value(ValueType.STRING, "write"),
                                "hashAlgType", new Value(ValueType.STRING, "SHA256"),
                                "arguments", new Value(ValueType.STRING, "100,test,test,egfd,тест"),
                                "printAbsolutePath", new Value(ValueType.STRING, "false"),
                                "useOnlyWorkDirectory", new Value(ValueType.STRING, "false")
                        ),
                        null,
                        "C:\\tmp"
                ),
                new File()
        );

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(null, null, null);
        process.fullLifeCycle(executionContextTool);//.forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
    }

    @Test
    public void readText() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of("rootFolder", new Value(ValueType.STRING, " "),
                                "type", new Value(ValueType.STRING, "readText"),
                                "hashAlgType", new Value(ValueType.STRING, "SHA256"),
                                "arguments", new Value(ValueType.STRING, "\\"),
                                "printAbsolutePath", new Value(ValueType.STRING, "false"),
                                "useOnlyWorkDirectory", new Value(ValueType.STRING, "false")
                        ),
                        null,
                        "C:\\tmp"
                ),
                new File()
        );

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(List.of(
                List.of(
                        new Action(
                                List.of(
                                        new Message(new Value("C:\\Users\\user\\Documents\\tmp\\main.txt")),
                                        new Message(new Value("C:\\Users\\user\\Documents\\tmp\\main2.txt"))
                                ),
                                ActionType.EXECUTE
                        ))),
                null,
                null);
        process.fullLifeCycle(executionContextTool);//.forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
    }

    @Test
    public void process6() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of("rootFolder", new Value(ValueType.STRING, "1.txt"),
                                "type", new Value(ValueType.STRING, "write"),
                                "hashAlgType", new Value(ValueType.STRING, "SHA256"),
                                "arguments", new Value(ValueType.STRING, "\\"),
                                "printAbsolutePath", new Value(ValueType.STRING, "false"),
                                "useOnlyWorkDirectory", new Value(ValueType.STRING, "false")
                        ),
                        null,
                        "C:\\tmp"
                ),
                new File()
        );

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(List.of(
                List.of(
                        new Action(
                                List.of(
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "test"))
                                ),
                                ActionType.EXECUTE
                        ))),
                null,
                null);
        process.fullLifeCycle(executionContextTool);//.forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
    }

    @Test
    public void process7() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of("rootFolder", new Value(""),
                                "type", new Value(ValueType.STRING, "useArgument"),
                                "hashAlgType", new Value(ValueType.STRING, "SHA256"),
                                "arguments", new Value(ValueType.STRING, "\\"),
                                "printAbsolutePath", new Value(ValueType.STRING, "false"),
                                "useOnlyWorkDirectory", new Value(ValueType.STRING, "false")
                        ),
                        null,
                        "C:\\tmp"
                ),
                new File()
        );
        process.start();

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(List.of(
                List.of(
                        new Action(
                                List.of(
                                        new Message(MessageType.DATA, new Date(), new Value("writePart")),
                                        new Message(MessageType.DATA, new Date(), new Value("C:\\tmp\\1.txt")),
                                        new Message(MessageType.DATA, new Date(), new Value("hello world"))
                                ),
                                ActionType.EXECUTE
                        ))),
                null,
                null);
        process.execute(executionContextTool);//.forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        process.stop();
    }

    @Test
    public void process8() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of("rootFolder", new Value(ValueType.STRING, "C:\\tmp\\DB-db-360\\install.sql"),
                                "type", new Value(ValueType.STRING, "readTextLastNLines"),
                                "hashAlgType", new Value(ValueType.STRING, "SHA256"),
                                "arguments", new Value(ValueType.STRING, "5"),
                                "printAbsolutePath", new Value(ValueType.STRING, "false"),
                                "useOnlyWorkDirectory", new Value(ValueType.STRING, "false")
                        ),
                        null,
                        "C:\\tmp"
                ),
                new File()
        );

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(null, null, null);
        process.fullLifeCycle(executionContextTool);//.forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
    }

    @Test
    public void process9() throws IOException {
        String filePath = "C:\\tmp\\tmp.txt";
        java.io.File file = new java.io.File(filePath);
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of("rootFolder", new Value(ValueType.STRING, filePath),
                                "type", new Value(ValueType.STRING, "readTextNew"),
                                "hashAlgType", new Value(ValueType.STRING, "SHA256"),
                                "arguments", new Value(ValueType.STRING, ""),
                                "printAbsolutePath", new Value(ValueType.STRING, "false"),
                                "useOnlyWorkDirectory", new Value(ValueType.STRING, "false")
                        ),
                        null,
                        "C:\\tmp"
                ),
                new File()
        );

        FileUtils.writeStringToFile(file, "start\n", Charset.defaultCharset(), false);
        process.start();

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(null, null, null);

        process.execute(executionContextTool);//.forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().clear();

        for (int i = 0; i < 5; i++) {
            FileUtils.writeStringToFile(file, String.format("%d line\n", i), Charset.defaultCharset(), true);

            process.execute(executionContextTool);//.forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
            executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
            executionContextTool.getOutput().clear();
        }

        process.stop();
    }

    @Test
    public void process10() throws IOException {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of("rootFolder", new Value(ValueType.STRING, ""),
                                "type", new Value(ValueType.STRING, "waitFile"),
                                "hashAlgType", new Value(ValueType.STRING, "SHA256"),
                                "arguments", new Value(ValueType.STRING, "10000"),
                                "printAbsolutePath", new Value(ValueType.STRING, "false"),
                                "useOnlyWorkDirectory", new Value(ValueType.STRING, "false")
                        ),
                        null,
                        "C:\\tmp"
                ),
                new File()
        );

        process.start();

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(List.of(
                List.of(
                        new Action(
                                List.of(
                                        new Message(MessageType.DATA, new Date(), new Value("C:\\mt5\\agents\\2\\auto_report.htm"))
                                ),
                                ActionType.EXECUTE
                        ))), null, null);

        process.execute(executionContextTool);//.forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().clear();

        process.stop();
    }

    @Test
    public void process11() throws IOException {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of("rootFolder", new Value(ValueType.STRING, ""),
                                "type", new Value(ValueType.STRING, "copyForce"),
                                "hashAlgType", new Value(ValueType.STRING, "SHA256"),
                                "arguments", new Value(ValueType.STRING, "10000"),
                                "printAbsolutePath", new Value(ValueType.STRING, "false"),
                                "useOnlyWorkDirectory", new Value(ValueType.STRING, "false")
                        ),
                        null,
                        "C:\\tmp"
                ),
                new File()
        );

        process.start();

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(List.of(
                List.of(
                        new Action(
                                List.of(
                                        new Message(MessageType.DATA, new Date(), new Value("C:\\mt5\\master\\")),
                                        new Message(MessageType.DATA, new Date(), new Value("C:\\mt5\\agents\\10\\"))
                                ),
                                ActionType.EXECUTE
                        ))), null, null);

        process.execute(executionContextTool);//.forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().clear();

        process.stop();
    }

    @Test
    public void process12() throws IOException {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of("rootFolder", new Value(ValueType.STRING, "C:\\tmp\\DB-db-34\\install2.sql"),
                                "type", new Value(ValueType.STRING, "writeText"),
                                "hashAlgType", new Value(ValueType.STRING, "SHA256"),
                                "arguments", new Value(ValueType.STRING, "100,test,test,egfd,тест"),
                                "printAbsolutePath", new Value(ValueType.STRING, "false"),
                                "useOnlyWorkDirectory", new Value(ValueType.STRING, "false")
                        ),
                        null,
                        "C:\\tmp"
                ),
                new File()
        );

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(List.of(
                List.of(
                        new Action(
                                List.of(
                                        new Message(MessageType.DATA, new Date(), new Value("100,test,test,egfd,тест")),
                                        new Message(MessageType.DATA, new Date(), new Value("UTF-8"))
                                ),
                                ActionType.EXECUTE
                        ))), null, null);
        process.fullLifeCycle(executionContextTool);//.forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
    }

    @Test
    public void readTextFromZip() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of("rootFolder", new Value(ValueType.STRING, "C:\\tmp\\"),
                                "type", new Value(ValueType.STRING, "readText"),
                                "hashAlgType", new Value(ValueType.STRING, "SHA256"),
                                "arguments", new Value(ValueType.STRING, ""),
                                "printAbsolutePath", new Value(ValueType.STRING, "false"),
                                "useOnlyWorkDirectory", new Value(ValueType.STRING, "false")
                        ),
                        null,
                        "C:\\tmp"
                ),
                new File()
        );

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(List.of(
                List.of(
                        new Action(
                                List.of(
                                        new Message(new Value("C:\\Users\\pwipo\\Documents\\dev\\soft\\smc\\smcm\\HtmlGuiPage.smcm")),
                                        new Message(new Value("properties.xml")),
                                        new Message(new Value("UTF-8"))
                                ),
                                ActionType.EXECUTE
                        ))),
                null,
                null, null, "ec", "readTextFromZip");
        process.fullLifeCycle(executionContextTool);//.forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
    }

    @Test
    public void dirInfoObj() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of("rootFolder", new Value(ValueType.STRING, ""),
                                "type", new Value(ValueType.STRING, "readText"),
                                "hashAlgType", new Value(ValueType.STRING, "SHA256"),
                                "arguments", new Value(ValueType.STRING, ""),
                                "printAbsolutePath", new Value(ValueType.STRING, "false"),
                                "useOnlyWorkDirectory", new Value(ValueType.STRING, "true")
                        ),
                        null,
                        "C:\\tmp"
                ),
                new File()
        );

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(List.of(
                List.of(
                        new Action(
                                List.of(
                                        new Message(new Value(""))
                                ),
                                ActionType.EXECUTE
                        ))),
                null,
                null, null, "ec", "dirInfoObj");
        process.fullLifeCycle(executionContextTool);//.forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
    }

}