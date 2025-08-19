package ru.smcsystem.smcmodules.module;

import org.junit.Test;
import ru.smcsystem.api.dto.ObjectArray;
import ru.smcsystem.api.dto.ObjectElement;
import ru.smcsystem.api.dto.ObjectField;
import ru.smcsystem.api.enumeration.ActionType;
import ru.smcsystem.api.enumeration.MessageType;
import ru.smcsystem.test.Process;
import ru.smcsystem.test.emulate.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class DialogsTest {

    @Test
    public void testMessage() {
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
    public void testMessage2() {
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
    public void testSelect() {
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
    public void testTwoInput() {
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

    @Test
    public void testChat() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "type", new Value("CHAT"),
                                "title", new Value("[[titleText]]!"),
                                "message", new Value("[[text]]"),
                                "lang", new Value("titleText::en::Window chat::ru::Окно чата;;" +
                                        "text::en::Chat messages::ru::Сообщения чата;;"
                                )
                        ),
                        null,
                        null
                ),
                new Dialogs()
        );
        process.start();

        ExecutionContextToolImpl executionContextTool;

        executionContextTool = new ExecutionContextToolImpl(null, null, null);
        process.execute(executionContextTool).forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value("user"))
                                        ),
                                        ActionType.EXECUTE
                                ))), null, null, List.of(
                (lst) -> {
                    return new Action(List.of(
                            new Message(new Value("1 " + (!lst.isEmpty() ? lst.get(0).toString().length() : ""))),
                            new Message(new Value("response"))
                    ));
                }
        ));
        process.execute(executionContextTool).forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        executionContextTool = new ExecutionContextToolImpl(null, null, null, List.of(
                (lst) -> {
                    return new Action(List.of(new Message(new Value(new ObjectArray(
                            new ObjectElement(new ObjectField("id", 1), new ObjectField("name", "f1"), new ObjectField("value", 1)),
                            new ObjectElement(new ObjectField("id", 2), new ObjectField("name", "f2"), new ObjectField("value", 3)))))));
                }
        ));
        process.execute(executionContextTool).forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        process.stop();
    }

}
