package ru.smcsystem.modules.flowControllerSwitch;

import ru.smcsystem.api.dto.IMessage;
import ru.smcsystem.api.dto.IValue;
import ru.smcsystem.api.enumeration.CommandType;
import ru.smcsystem.api.exceptions.ModuleException;
import ru.smcsystem.api.module.Module;
import ru.smcsystem.api.tools.ConfigurationTool;
import ru.smcsystem.api.tools.execution.ExecutionContextTool;
import ru.smcsystem.smc.utils.ModuleUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Switch implements Module {

    private Boolean isNeedReturnDataFromLast;
    private Integer countPatternsInPack;
    private List<List<Pattern>> patterns;

    @Override
    public void start(ConfigurationTool ConfigurationTool) throws ModuleException {
        isNeedReturnDataFromLast = Boolean.valueOf((String) ConfigurationTool.getSetting("isNeedReturnDataFromLast").orElseThrow(() -> new ModuleException("isNeedReturnDataFromLast setting not found")).getValue());
        countPatternsInPack = (Integer) ConfigurationTool.getSetting("countPatternsInPack").orElseThrow(() -> new ModuleException("countPatternsInPack setting not found")).getValue();
        String str = (String) ConfigurationTool.getSetting("patterns").orElseThrow(() -> new ModuleException("patterns setting not found")).getValue();
        patterns = null;
        if (!str.isBlank()) {
            String[] arr = str.split("::");
            patterns = new ArrayList<>(arr.length);
            for (int i = 0; i + countPatternsInPack - 1 < arr.length; i = i + countPatternsInPack) {
                List<Pattern> params = new ArrayList<>(countPatternsInPack);
                for (int j = 0; j < countPatternsInPack; j++) {
                    String s = arr[i + j];
                    if (s.isBlank())
                        break;
                    params.add(Pattern.compile(s.trim()));
                }
                if (params.size() == countPatternsInPack)
                    patterns.add(params);
            }
        }
    }

    @Override
    public void update(ConfigurationTool ConfigurationTool) throws ModuleException {
        stop(ConfigurationTool);
        start(ConfigurationTool);
    }

    @Override
    public void process(ConfigurationTool configurationTool, ExecutionContextTool executionContextTool) throws ModuleException {
        ModuleUtils.processMessagesAll(configurationTool, executionContextTool, (ignore, lst) -> {
            LinkedList<IMessage> messages = lst.stream()
                    .flatMap(Collection::stream)
                    .collect(Collectors.toCollection(LinkedList::new));
            if (patterns != null) {
                while (messages.size() >= countPatternsInPack) {
                    List<IMessage> dataPiece = new ArrayList<>(countPatternsInPack);
                    for (int i = 0; i < countPatternsInPack; i++)
                        dataPiece.add(messages.poll());
                    Integer id = null;
                    for (int j = 0; id == null && j < patterns.size(); j++) {
                        id = j;
                        for (int k = 0; k < countPatternsInPack; k++) {
                            if (!patterns.get(j).get(k).matcher(ModuleUtils.toString(dataPiece.get(k))).find()) {
                                id = null;
                                break;
                            }
                        }
                    }
                    exec(executionContextTool, id);
                }
            } else {
                for (IMessage m : messages) {
                    if (ModuleUtils.isNumber(m))
                        exec(executionContextTool, ModuleUtils.getNumber(m).intValue());
                }
            }
        });
    }

    private void exec(ExecutionContextTool executionContextTool, Integer id) {
        if (id == null || id >= executionContextTool.getFlowControlTool().countManagedExecutionContexts())
            return;
        executionContextTool.getFlowControlTool().executeNow(CommandType.EXECUTE, id, null);
        if (isNeedReturnDataFromLast) {
            executionContextTool.getFlowControlTool().getMessagesFromExecuted(id).forEach(action ->
                    executionContextTool.addMessage(action.getMessages().stream().map(IValue::getValue).collect(Collectors.toList())));
        }
    }

    @Override
    public void stop(ConfigurationTool ConfigurationTool) throws ModuleException {
        isNeedReturnDataFromLast = null;
        patterns = null;
    }

}
