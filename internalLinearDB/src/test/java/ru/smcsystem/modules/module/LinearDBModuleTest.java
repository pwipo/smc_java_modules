package ru.smcsystem.modules.module;

import org.junit.Test;
import ru.smcsystem.api.dto.ObjectArray;
import ru.smcsystem.api.dto.ObjectElement;
import ru.smcsystem.api.dto.ObjectField;
import ru.smcsystem.api.enumeration.ActionType;
import ru.smcsystem.smc.utils.ModuleUtils;
import ru.smcsystem.test.Process;
import ru.smcsystem.test.emulate.*;

import java.util.List;
import java.util.Map;

public class LinearDBModuleTest {

    @Test
    public void process() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "fieldNameId", new Value("_id"),
                                "fieldNameDate", new Value("_date"),
                                "indexes", new Value("value1=LONG, value2=DOUBLE, value3=STRING")
                        ),
                        null,
                        "C:\\tmp\\13"
                ),
                new LinearDBModule()
        );
        process.start();

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(new Value(new ObjectArray(
                                                        new ObjectElement(new ObjectField("value1", 1), new ObjectField("value2", 2.), new ObjectField("value3", "v1")),
                                                        new ObjectElement(new ObjectField("value1", 2), new ObjectField("value2", 3.), new ObjectField("value3", "v2")),
                                                        new ObjectElement(new ObjectField("value1", 3), new ObjectField("value2", 4.), new ObjectField("value3", "v3")),
                                                        new ObjectElement(new ObjectField("value1", 4), new ObjectField("value2", 5.), new ObjectField("value3", "v4"))
                                                )))
                                        ),
                                        ActionType.EXECUTE
                                ))),
                null, null, null, "insert", "insert");
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        ObjectArray objectArray = ModuleUtils.getObjectArray(executionContextTool.getOutput().get(0));

        if (objectArray.isSimple() && objectArray.size() > 0) {
            executionContextTool = new ExecutionContextToolImpl(
                    List.of(
                            List.of(
                                    new Action(
                                            List.of(
                                                    new Message(new Value(new ObjectArray(new ObjectElement(
                                                            new ObjectField("filter", new ObjectElement(
                                                                    new ObjectField("t", "="),
                                                                    new ObjectField("f", "_id"),
                                                                    new ObjectField("v", ((Number) objectArray.get(0)).longValue())))
                                                    ))))
                                            ),
                                            ActionType.EXECUTE
                                    ))),
                    null, null, null, "find", "find");
            process.execute(executionContextTool);
            executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
            objectArray = ModuleUtils.getObjectArray(executionContextTool.getOutput().get(0));
            if (ModuleUtils.isArrayContainObjectElements(objectArray)) {
                ObjectElement objectElement = (ObjectElement) objectArray.get(0);
                objectElement.findField("value2").ifPresent(f -> f.setValue(100.));
                executionContextTool = new ExecutionContextToolImpl(
                        List.of(
                                List.of(
                                        new Action(
                                                List.of(
                                                        new Message(new Value(new ObjectArray(objectElement)))
                                                ),
                                                ActionType.EXECUTE
                                        ))),
                        null, null, null, "update", "update");
                process.execute(executionContextTool);
                executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
            }
        }

        executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(new Value(new ObjectArray(new ObjectElement(
                                                        new ObjectField("filter", new ObjectElement(
                                                                new ObjectField("t", "<="),
                                                                new ObjectField("f", "value1"),
                                                                new ObjectField("v", 2))),
                                                        new ObjectField("sort", new ObjectElement(
                                                                new ObjectField("t", "desk"),
                                                                new ObjectField("f", "value1"))),
                                                        new ObjectField("skip", 0),
                                                        new ObjectField("limit", 3)
                                                ))))
                                        ),
                                        ActionType.EXECUTE
                                ))),
                null, null, null, "find", "find");
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(new Message(new Value(new ObjectArray(new ObjectElement())))),
                                        ActionType.EXECUTE
                                ))),
                null, null, null, "count", "count");
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(new Value(new ObjectArray(new ObjectElement(new ObjectField("filter", new ObjectElement(
                                                        new ObjectField("t", "<="), new ObjectField("f", "value1"), new ObjectField("v", 3)))))))
                                        ),
                                        ActionType.EXECUTE
                                ))),
                null, null, null, "delete_where", "delete_where");
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(new Value(4L))
                                        ),
                                        ActionType.EXECUTE
                                ))),
                null, null, null, "delete", "delete");
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(new Value(10))
                                        ),
                                        ActionType.EXECUTE
                                ))),
                null, null, null, "apply_log", "apply_log");
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(new Value(new ObjectArray(new ObjectElement(new ObjectField("filter", new ObjectElement(
                                                        new ObjectField("t", "<="), new ObjectField("f", "value1"), new ObjectField("v", 100)))))))
                                        ),
                                        ActionType.EXECUTE
                                ))),
                null, null, null, "count", "count");
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        process.stop();

    }
}
