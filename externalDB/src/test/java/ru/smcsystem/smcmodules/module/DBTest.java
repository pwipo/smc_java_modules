package ru.smcsystem.smcmodules.module;

import org.junit.Test;
import ru.smcsystem.api.dto.ObjectArray;
import ru.smcsystem.api.dto.ObjectElement;
import ru.smcsystem.api.dto.ObjectField;
import ru.smcsystem.api.enumeration.ActionType;
import ru.smcsystem.api.enumeration.MessageType;
import ru.smcsystem.api.enumeration.ValueType;
import ru.smcsystem.test.Process;
import ru.smcsystem.test.emulate.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class DBTest {

    @Test
    public void process() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of("type", new Value(ValueType.STRING, "derbyInMemory"),
                                "connection_params", new Value(ValueType.STRING, "ChatModuleDB2"),
                                "login", new Value(ValueType.STRING, " "),
                                "password", new Value(ValueType.STRING, " "),
                                "useAutoConvert", new Value("false"),
                                "resultFormat", new Value("OBJECT_SERIALIZATION")
                        ),
                        null,
                        "C:\\Users\\user\\Documents\\tmp\\5"
                ),
                new DB()
        );

        process.start();
        // executionContextTool.getOutput().clear();

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(List.of(
                List.of(
                        new Action(
                                List.of(
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 1)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "select * from textes"))
                                ),
                                ActionType.EXECUTE
                        ))),
                null,
                null
        );
        // process.fullLifeCycle(executionContextTool).forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        // executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().clear();

        executionContextTool = new ExecutionContextToolImpl(List.of(
                List.of(
                        new Action(
                                List.of(
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 1)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "insert into textes VALUES ('hi','hello', 4)"))
                                ),
                                ActionType.EXECUTE
                        ))),
                null,
                null
        );

        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().clear();

        executionContextTool = new ExecutionContextToolImpl(List.of(
                List.of(
                        new Action(
                                List.of(
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 2))
                                ),
                                ActionType.EXECUTE
                        ))),
                null,
                null);

        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        long connectionId = ((Number) executionContextTool.getOutput().get(0).getValue()).longValue();
        executionContextTool.getOutput().clear();

        executionContextTool = new ExecutionContextToolImpl(List.of(
                List.of(
                        new Action(
                                List.of(
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 5)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.LONG, connectionId)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "insert into textes VALUES ('hi2','hello2', 5)")),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "insert into textes VALUES ('hi3','hello3', 6)"))
                                ),
                                ActionType.EXECUTE
                        ))),
                null,
                null);

        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().clear();

        executionContextTool = new ExecutionContextToolImpl(List.of(
                List.of(
                        new Action(
                                List.of(
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 3)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.LONG, connectionId))
                                ),
                                ActionType.EXECUTE
                        ))),
                null,
                null);

        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().clear();

        executionContextTool = new ExecutionContextToolImpl(List.of(
                List.of(
                        new Action(
                                List.of(
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 1)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "select * from textes"))
                                ),
                                ActionType.EXECUTE
                        ))),
                null,
                null);

        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().clear();

        executionContextTool = new ExecutionContextToolImpl(List.of(
                List.of(
                        new Action(
                                List.of(
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 7))
                                        , new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "select * from textes where UUID=?"))
                                        , new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "hi3"))
                                ),
                                ActionType.EXECUTE
                        ))),
                null,
                null);

        executionContextTool = new ExecutionContextToolImpl(List.of(
                List.of(
                        new Action(
                                List.of(
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 9))
                                        , new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "select * from textes where UUID=?"))
                                        , new Message(MessageType.DATA, new Date(), new Value(new ObjectArray(new ObjectElement(new ObjectField("1", "hi2")))))
                                ),
                                ActionType.EXECUTE
                        ))),
                null,
                null);

        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().clear();

        process.stop();
    }

    @Test
    public void process2() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of("type", new Value(ValueType.STRING, "derby"),
                                "connection_params", new Value(ValueType.STRING, "db1"),
                                "login", new Value(ValueType.STRING, " "),
                                "password", new Value(ValueType.STRING, " "),
                                "useAutoConvert", new Value("false"),
                                "resultFormat", new Value("OBJECT_SERIALIZATION")
                        ),
                        null,
                        "C:\\tmp\\2"
                ),
                new DB()
        );

        process.start();
        // executionContextTool.getOutput().clear();

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(List.of(
                List.of(
                        new Action(
                                List.of(
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 1)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "CREATE TABLE Test (\n" +
                                                "\tid INTEGER PRIMARY KEY,\n" +
                                                "\tname VARCHAR(1000),\n" +
                                                "\tdate TIMESTAMP\n" +
                                                ")")),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "insert into Test VALUES (1, 'test1', '1960-01-01 23:03:20')")),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "insert into Test VALUES (2, 'test2', '1960-01-01 23:03:20')"))
                                ),
                                ActionType.EXECUTE
                        ))),
                null,
                null
        );
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().clear();

        executionContextTool = new ExecutionContextToolImpl(List.of(
                List.of(
                        new Action(
                                List.of(
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 1)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "select * from Test"))
                                ),
                                ActionType.EXECUTE
                        ))),
                null,
                null);

        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().clear();

        process.stop();
    }

    @Test
    public void process3() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of("type", new Value(ValueType.STRING, "derbyInMemory"),
                                "connection_params", new Value(ValueType.STRING, "ChatModuleDB2"),
                                "login", new Value(ValueType.STRING, " "),
                                "password", new Value(ValueType.STRING, " "),
                                "useAutoConvert", new Value("false"),
                                "resultFormat", new Value("OBJECT_SERIALIZATION")
                        ),
                        null,
                        "C:\\Users\\user\\Documents\\tmp\\6"
                ),
                new DB()
        );

        process.start();

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(List.of(
                List.of(
                        new Action(
                                List.of(
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 1)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "select * from modules"))
                                ),
                                ActionType.EXECUTE
                        ))),
                null,
                null
        );

        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().clear();

        executionContextTool = new ExecutionContextToolImpl(List.of(
                List.of(
                        new Action(
                                List.of(
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 1)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "select * from application"))
                                ),
                                ActionType.EXECUTE
                        ))),
                null,
                null
        );

        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().clear();

        executionContextTool = new ExecutionContextToolImpl(List.of(
                List.of(
                        new Action(
                                List.of(
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 1)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "select * from application_description where application_id=(select id from application where uuid='WarOfRobots')"))
                                ),
                                ActionType.EXECUTE
                        ))),
                null,
                null
        );

        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().clear();

        process.stop();
    }

    @Test
    public void process4() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of("type", new Value(ValueType.STRING, "derbyInMemory"),
                                "connection_params", new Value(ValueType.STRING, "TestDB"),
                                "login", new Value(ValueType.STRING, ""),
                                "password", new Value(ValueType.STRING, ""),
                                "useAutoConvert", new Value("true"),
                                "resultFormat", new Value("OBJECT"),
                                "resultSetColumnNameToUpperCase", new Value("false"),
                                "queryTimeout", new Value(0)
                        ),
                        null,
                        "C:\\Users\\user\\Documents\\tmp\\12"
                ),
                new DB()
        );

        process.start();

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(List.of(
                List.of(
                        new Action(
                                List.of(
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 1)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "select * from users"))
                                ),
                                ActionType.EXECUTE
                        ))),
                null,
                null
        );

        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().clear();

        process.stop();
    }

    @Test
    public void processInsertMultiline() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of("type", new Value(ValueType.STRING, "derbyInMemory"),
                                "connection_params", new Value(ValueType.STRING, "TestDB"),
                                "login", new Value(ValueType.STRING, ""),
                                "password", new Value(ValueType.STRING, ""),
                                "useAutoConvert", new Value("true"),
                                "resultFormat", new Value("OBJECT_WITH_NULL_AND_BOOLEAN"),
                                "resultSetColumnNameToUpperCase", new Value("false"),
                                "queryTimeout", new Value(0)
                        ),
                        null,
                        "C:\\Users\\user\\Documents\\tmp\\12"
                ),
                new DB()
        );

        process.start();

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(List.of(
                List.of(
                        new Action(
                                List.of(
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.INTEGER, 1)),
                                        new Message(MessageType.DATA, new Date(), new Value(ValueType.STRING, "select * from users"))
                                ),
                                ActionType.EXECUTE
                        ))),
                null,
                null
        );

        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().clear();

        executionContextTool = new ExecutionContextToolImpl(List.of(
                List.of(
                        new Action(
                                List.of(
                                        new Message(MessageType.DATA, new Date(), new Value(19)),
                                        new Message(MessageType.DATA, new Date(), new Value("INSERT INTO users (login, password, salt, email, created, email_confirmed, version) VALUES (?, ?, ? ,?, CURRENT_TIMESTAMP,  CURRENT_TIMESTAMP,  CURRENT_TIMESTAMP)")),
                                        new Message(MessageType.DATA, new Date(), new Value(new ObjectArray(
                                                new ObjectElement(new ObjectField("login", "user1"), new ObjectField("password", "password1"), new ObjectField("salt", "salt1"), new ObjectField("email", "email1"))
                                                , new ObjectElement(new ObjectField("login", "user2"), new ObjectField("password", "password2"), new ObjectField("salt", "salt2"), new ObjectField("email", "email2"))
                                                , new ObjectElement(new ObjectField("login", "user3"), new ObjectField("password", "password3"), new ObjectField("salt", "salt3"), new ObjectField("email", "email3"))
                                                , new ObjectElement(new ObjectField("login", "user4"), new ObjectField("password", "password4"), new ObjectField("salt", "salt4"), new ObjectField("email", "email4"))
                                        ))),
                                        new Message(MessageType.DATA, new Date(), new Value("login, password, salt, email"))
                                ),
                                ActionType.EXECUTE
                        ))),
                null,
                null
        );

        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().clear();

        executionContextTool = new ExecutionContextToolImpl(List.of(
                List.of(
                        new Action(
                                List.of(
                                        new Message(MessageType.DATA, new Date(), new Value("INSERT INTO users (login, password, salt, email, created, email_confirmed, version) VALUES (?, ?, ? ,?, CURRENT_TIMESTAMP,  CURRENT_TIMESTAMP,  CURRENT_TIMESTAMP)")),
                                        new Message(MessageType.DATA, new Date(), new Value(new ObjectArray(
                                                new ObjectElement(new ObjectField("login", "user5"), new ObjectField("password", "password5"), new ObjectField("salt", "salt5"), new ObjectField("email", "email5"))
                                                , new ObjectElement(new ObjectField("login", "user6"), new ObjectField("password", "password6"), new ObjectField("salt", "salt6"), new ObjectField("email", "email6"))
                                        ))),
                                        new Message(MessageType.DATA, new Date(), new Value("login, password, salt, email"))
                                ),
                                ActionType.EXECUTE
                        ))),
                null,
                null,
                null,
                "execute",
                "EXECUTE_MULTILINE_INSERT_IN_ONE_TRANSACTION_RETURN_GENERATED_KEY"
        );

        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().clear();

        executionContextTool = new ExecutionContextToolImpl(List.of(
                List.of(
                        new Action(
                                List.of(
                                        new Message(MessageType.DATA, new Date(), new Value(1)),
                                        new Message(MessageType.DATA, new Date(), new Value("select * from users"))
                                ),
                                ActionType.EXECUTE
                        ))),
                null,
                null
        );

        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().clear();

        process.stop();
    }


}