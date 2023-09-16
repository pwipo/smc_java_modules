package ru.smcsystem.modules.flowControllerOrderingExecutor;

import org.junit.Test;
import ru.smcsystem.api.dto.IMessage;
import ru.smcsystem.api.enumeration.ActionType;
import ru.smcsystem.api.enumeration.ValueType;
import ru.smcsystem.test.Process;
import ru.smcsystem.test.emulate.Action;
import ru.smcsystem.test.emulate.ConfigurationToolImpl;
import ru.smcsystem.test.emulate.ExecutionContextToolImpl;
import ru.smcsystem.test.emulate.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OrderingExecutorTest {

    @Test
    public void process() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "type", new Value(ValueType.STRING, "execute")
                                , "executionType", new Value(ValueType.STRING, "now")
                                , "isNeedReturnDataFromLast", new Value(ValueType.STRING, "false")
                                , "breakWhenError", new Value(ValueType.STRING, "false")
                                , "breakWhenNoDataFromPrev", new Value(ValueType.STRING, "false")
                        ),
                        null,
                        null
                ),
                new OrderingExecutor()
        );
        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(null, null, List.of(new Action(new ArrayList<>(), ActionType.EXECUTE)));
        List<IMessage> messages = process.fullLifeCycle(executionContextTool);
        messages.forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
    }
}