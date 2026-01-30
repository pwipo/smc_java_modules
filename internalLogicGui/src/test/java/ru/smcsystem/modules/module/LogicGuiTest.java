package ru.smcsystem.modules.module;

import org.junit.Test;
import ru.smcsystem.test.Process;
import ru.smcsystem.test.emulate.Action;
import ru.smcsystem.test.emulate.ExecutionContextToolImpl;
import ru.smcsystem.test.emulate.Message;
import ru.smcsystem.test.emulate.Value;

import java.util.List;
import java.util.Map;

public class LogicGuiTest {

    @Test
    public void process() {
        Process process = new Process(
                new MyConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "shapeId", new Value("if1")
                        ),
                        null,
                        null
                ),
                new LogicGui()
        );
        process.start();

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                List.of(List.of(new Action(List.of(
                        new Message(new Value(1))
                )))),
                null, null,
                List.of(
                        l -> {
                            System.out.println(l);
                            return new Action(List.of(
                                    new Message(new Value(true)),
                                    new Message(new Value(10))
                            ));
                        }
                ), "test", "execute");
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        executionContextTool = new ExecutionContextToolImpl(
                List.of(List.of(new Action(List.of(
                        new Message(new Value(2))
                )))),
                null, null,
                List.of(
                        l -> {
                            System.out.println(l);
                            return new Action(List.of(
                                    new Message(new Value(true)),
                                    new Message(new Value(2))
                            ));
                        }
                ), "test", "execute");
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        process.stop();
    }
}
