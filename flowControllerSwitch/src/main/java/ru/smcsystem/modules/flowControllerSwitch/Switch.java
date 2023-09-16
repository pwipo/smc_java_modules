package ru.smcsystem.modules.flowControllerSwitch;

import ru.smcsystem.api.dto.IValue;
import ru.smcsystem.api.enumeration.CommandType;
import ru.smcsystem.api.enumeration.ValueType;
import ru.smcsystem.api.exceptions.ModuleException;
import ru.smcsystem.api.module.Module;
import ru.smcsystem.api.tools.ConfigurationTool;
import ru.smcsystem.api.tools.execution.ExecutionContextTool;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Switch implements Module {

    private Boolean isNeedReturnDataFromLast;
    private List<Pattern> patterns;

    @Override
    public void start(ConfigurationTool ConfigurationTool) throws ModuleException {
        isNeedReturnDataFromLast = Boolean.valueOf((String) ConfigurationTool.getSetting("isNeedReturnDataFromLast").orElseThrow(() -> new ModuleException("isNeedReturnDataFromLast setting not found")).getValue());
        String str = (String) ConfigurationTool.getSetting("patterns").orElseThrow(() -> new ModuleException("patterns setting not found")).getValue();
        patterns = null;
        if (!str.isBlank()) {
            patterns = Arrays.stream(str.split("::"))
                    .filter(s -> !s.isBlank())
                    .map(Pattern::compile)
                    .collect(Collectors.toList());
        }
    }

    @Override
    public void update(ConfigurationTool ConfigurationTool) throws ModuleException {
        stop(ConfigurationTool);
        start(ConfigurationTool);
    }

    @Override
    public void process(ConfigurationTool ConfigurationTool, ExecutionContextTool executionContextTool) throws ModuleException {
        for (int i = 0; i < executionContextTool.countSource(); i++) {
            executionContextTool.getMessages(i).forEach(a -> a.getMessages().forEach(m -> {
                int id = -1;
                if (patterns != null) {
                    String value = m.getValue().toString();
                    for (int j = 0; j < patterns.size(); j++) {
                        if (patterns.get(j).matcher(value).find()) {
                            id = j;
                            break;
                        }
                    }
                } else {
                    if (ValueType.BYTE.equals(m.getType()) || ValueType.SHORT.equals(m.getType()) || ValueType.INTEGER.equals(m.getType()) || ValueType.LONG.equals(m.getType()) || ValueType.FLOAT.equals(m.getType()) || ValueType.DOUBLE.equals(m.getType()) || ValueType.BIG_INTEGER.equals(m.getType()) || ValueType.BIG_DECIMAL.equals(m.getType()))
                        id = ((Number) m.getValue()).intValue();
                }
                if (id >= 0 && id < executionContextTool.getFlowControlTool().countManagedExecutionContexts()) {
                    executionContextTool.getFlowControlTool().executeNow(CommandType.EXECUTE, id, null);
                    if (isNeedReturnDataFromLast) {
                        executionContextTool.getFlowControlTool().getMessagesFromExecuted(id).forEach(action ->
                                executionContextTool.addMessage(action.getMessages().stream().map(IValue::getValue).collect(Collectors.toList())));
                    }
                }
            }));
        }

    }

    @Override
    public void stop(ConfigurationTool ConfigurationTool) throws ModuleException {
        isNeedReturnDataFromLast = null;
        patterns = null;
    }

}
