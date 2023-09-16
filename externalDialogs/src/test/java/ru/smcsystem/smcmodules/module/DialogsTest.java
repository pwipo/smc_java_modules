package ru.smcsystem.smcmodules.module;

import org.junit.Test;
import ru.smcsystem.api.enumeration.ActionType;
import ru.smcsystem.api.enumeration.MessageType;
import ru.smcsystem.test.Process;
import ru.smcsystem.test.emulate.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class DialogsTest {

    @Test
    public void process() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "type", new Value("MESSAGE"),
                                "title", new Value("[[titleText]]!"),
                                "message", new Value("[[hello]]! [[text]]?eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeersrztttttttttttttttttttttttttttttttttttttttthgffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffeeeeeeeeeeettttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq"),
                                "lang", new Value("hello::en::Hello::ru::Привет;;" +
                                        "text::en::How are you doing::ru::Как дела;;" +
                                        "titleText::en::Text::ru::Текст"
                                )
                        ),
                        null,
                        null
                ),
                new Dialogs()
        );
        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(null, null, null);
        process.fullLifeCycle(executionContextTool).forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
    }

    @Test
    public void process2() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "type", new Value("MESSAGE"),
                                "title", new Value("[[titleText]]!"),
                                "message", new Value("[[hello]]! [[text]]?"),
                                "lang", new Value("hello::en::Hello::ru::Привет;;" +
                                        "text::en::How are you doing::ru::Как дела;;" +
                                        "titleText::en::Text::ru::Текст"
                                )
                        ),
                        null,
                        null
                ),
                new Dialogs()
        );
        process.start();

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(1)),
                                                new Message(MessageType.DATA, new Date(), new Value("test")),
                                                new Message(MessageType.DATA, new Date(), new Value(3))
                                        ),
                                        ActionType.EXECUTE
                                ))), null, null);
        process.execute(executionContextTool).forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(1)),
                                                new Message(MessageType.DATA, new Date(), new Value("[[hello]]!")),
                                                new Message(MessageType.DATA, new Date(), new Value(3))
                                        ),
                                        ActionType.EXECUTE
                                ))), null, null);
        process.execute(executionContextTool).forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        process.stop();
    }

    @Test
    public void process3() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "type", new Value("SELECT"),
                                "title", new Value("[[titleText]]!"),
                                "message", new Value("[[hello]]! [[text]]?"),
                                "lang", new Value("hello::en::Hello::ru::Привет;;" +
                                        "text::en::How are you doing::ru::Как дела;;" +
                                        "titleText::en::Text::ru::Текст"
                                )
                        ),
                        null,
                        null
                ),
                new Dialogs()
        );
        process.start();

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value("плохо")),
                                                new Message(MessageType.DATA, new Date(), new Value("нормально")),
                                                new Message(MessageType.DATA, new Date(), new Value("хорошо")),
                                                new Message(MessageType.DATA, new Date(), new Value("отлично"))
                                        ),
                                        ActionType.EXECUTE
                                ))), null, null);
        process.execute(executionContextTool).forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        process.stop();
    }

    @Test
    public void process4() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "type", new Value("TWO_INPUT"),
                                "title", new Value("[[titleText]]!"),
                                "message", new Value("[[hello]]! [[text]]?"),
                                "lang", new Value("hello::en::Hello::ru::Привет;;" +
                                        "text::en::How are you doing::ru::Как дела;;" +
                                        "titleText::en::Text::ru::Текст длинный"
                                )
                        ),
                        null,
                        null
                ),
                new Dialogs()
        );
        process.start();

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(" напиши свой вариант"))
                                        ),
                                        ActionType.EXECUTE
                                ))), null, null);
        process.execute(executionContextTool).forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        process.stop();
    }


}
