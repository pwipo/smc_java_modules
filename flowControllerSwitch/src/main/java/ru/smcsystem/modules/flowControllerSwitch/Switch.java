package ru.smcsystem.modules.flowControllerSwitch;

import ru.smcsystem.api.dto.IAction;
import ru.smcsystem.api.dto.IMessage;
import ru.smcsystem.api.dto.IValue;
import ru.smcsystem.api.enumeration.CommandType;
import ru.smcsystem.api.exceptions.ModuleException;
import ru.smcsystem.api.module.Module;
import ru.smcsystem.api.tools.ConfigurationTool;
import ru.smcsystem.api.tools.execution.ExecutionContextTool;
import ru.smcsystem.smc.utils.ModuleUtils;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Switch implements Module {

    private Boolean isNeedReturnDataFromLast;
    private Integer countPatternsInPack;
    private List<List<Pattern>> patterns;
    private Boolean isNeedReturnErrorFromLast;

    @Override
    public void start(ConfigurationTool configurationTool) throws ModuleException {
        isNeedReturnDataFromLast = configurationTool.getSetting("isNeedReturnDataFromLast").map(ModuleUtils::toBoolean)
                .orElseThrow(() -> new ModuleException("isNeedReturnDataFromLast setting not found"));
        countPatternsInPack = configurationTool.getSetting("countPatternsInPack").map(ModuleUtils::getNumber).map(Number::intValue)
                .orElseThrow(() -> new ModuleException("countPatternsInPack setting not found"));
        String str = configurationTool.getSetting("patterns").map(ModuleUtils::getString).orElseThrow(() -> new ModuleException("patterns setting not found"));
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
        isNeedReturnErrorFromLast = configurationTool.getSetting("isNeedReturnErrorFromLast").map(ModuleUtils::toBoolean)
                .orElseThrow(() -> new ModuleException("isNeedReturnErrorFromLast setting"));
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
            List<Object> resultData = null;
            List<Object> resultErrors = null;
            Optional<IAction> lastAction = ModuleUtils.getLastActionExecuteWithMessagesFromCommands(executionContextTool.getFlowControlTool().getCommandsFromExecuted(id));
            resultData = getData(lastAction);
            resultErrors = getError(lastAction);
            if (resultData != null)
                executionContextTool.addMessage(resultData);
            if (resultErrors != null)
                executionContextTool.addError(resultErrors);
        }
    }

    private List<Object> getData(Optional<IAction> lastAction) {
        if (isNeedReturnDataFromLast && lastAction.isPresent() && !ModuleUtils.hasErrors(lastAction.get())) {
            return lastAction
                    .map(ModuleUtils::getData)
                    .map(l -> l.stream().map(IValue::getValue).collect(Collectors.toList()))
                    .orElse(null);
        }
        return null;
    }

    private List<Object> getError(Optional<IAction> lastAction) {
        if (isNeedReturnErrorFromLast && lastAction.isPresent()) {
            return lastAction
                    .map(ModuleUtils::getErrors)
                    .map(l -> l.stream().map(IValue::getValue).collect(Collectors.toList()))
                    .orElse(null);
        }
        return null;
    }

    @Override
    public void stop(ConfigurationTool ConfigurationTool) throws ModuleException {
        isNeedReturnDataFromLast = null;
        patterns = null;
    }

}
