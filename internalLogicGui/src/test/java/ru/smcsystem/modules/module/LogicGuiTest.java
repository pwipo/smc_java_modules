package ru.smcsystem.modules.module;

import org.junit.Test;
import ru.smcsystem.api.dto.ObjectArray;
import ru.smcsystem.api.dto.ObjectElement;
import ru.smcsystem.api.dto.ObjectField;
import ru.smcsystem.test.Process;
import ru.smcsystem.test.emulate.*;

import java.util.List;
import java.util.Map;

public class LogicGuiTest {

    @Test
    public void process() {
        Process process = new Process(
                new ConfigurationToolImpl(
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
        ((Container) process.getConfigurationTool().getContainer()).setShapes(new ObjectArray(
                new ObjectElement(
                        new ObjectField("type", "rhombus"),
                        new ObjectField("name", "if1"),
                        new ObjectField("description", "$src0.-1\neq\n1"),
                        new ObjectField("x", 100),
                        new ObjectField("y", 1),
                        new ObjectField("width", 100),
                        new ObjectField("height", 100),
                        new ObjectField("color", 1),
                        new ObjectField("strokeWidth", 1)
                ),
                new ObjectElement(
                        new ObjectField("type", "rectangle"),
                        new ObjectField("name", "exec1"),
                        new ObjectField("description", "0\n\"test\""),
                        new ObjectField("x", 1),
                        new ObjectField("y", 150),
                        new ObjectField("width", 100),
                        new ObjectField("height", 100),
                        new ObjectField("color", 1),
                        new ObjectField("strokeWidth", 1)
                ),
                new ObjectElement(
                        new ObjectField("type", "circle"),
                        new ObjectField("name", "output1"),
                        new ObjectField("description", "\"success\"\n$ec0"),
                        new ObjectField("x", 10),
                        new ObjectField("y", 300),
                        new ObjectField("width", 50),
                        new ObjectField("height", 50),
                        new ObjectField("color", 1),
                        new ObjectField("strokeWidth", 1)
                ),
                new ObjectElement(
                        new ObjectField("type", "circle"),
                        new ObjectField("name", "output2"),
                        new ObjectField("description", "\"fail\"\n$src0"),
                        new ObjectField("x", 200),
                        new ObjectField("y", 150),
                        new ObjectField("width", 50),
                        new ObjectField("height", 50),
                        new ObjectField("color", 1),
                        new ObjectField("strokeWidth", 1)
                ),
                new ObjectElement(
                        new ObjectField("type", "line"),
                        new ObjectField("name", "line1"),
                        new ObjectField("description", "1"),
                        new ObjectField("x", 150),
                        new ObjectField("y", 100),
                        new ObjectField("width", 100),
                        new ObjectField("height", 55),
                        new ObjectField("point2X", 50),
                        new ObjectField("point2Y", 155),
                        new ObjectField("color", 1),
                        new ObjectField("strokeWidth", 1)
                ),
                new ObjectElement(
                        new ObjectField("type", "line"),
                        new ObjectField("name", "line2"),
                        new ObjectField("description", "1"),
                        new ObjectField("x", 50),
                        new ObjectField("y", 250),
                        new ObjectField("width", 15),
                        new ObjectField("height", 55),
                        new ObjectField("point2X", 35),
                        new ObjectField("point2Y", 305),
                        new ObjectField("color", 1),
                        new ObjectField("strokeWidth", 1)
                ),
                new ObjectElement(
                        new ObjectField("type", "line"),
                        new ObjectField("name", "line3"),
                        new ObjectField("description", "1"),
                        new ObjectField("x", 150),
                        new ObjectField("y", 100),
                        new ObjectField("width", 75),
                        new ObjectField("height", 55),
                        new ObjectField("point2X", 225),
                        new ObjectField("point2Y", 155),
                        new ObjectField("color", 1),
                        new ObjectField("strokeWidth", 1)
                )
        ));
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
