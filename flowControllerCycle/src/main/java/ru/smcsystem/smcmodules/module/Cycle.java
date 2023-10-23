package ru.smcsystem.smcmodules.module;

import ru.smcsystem.api.dto.IAction;
import ru.smcsystem.api.dto.IMessage;
import ru.smcsystem.api.dto.IValue;
import ru.smcsystem.api.dto.ObjectArray;
import ru.smcsystem.api.enumeration.CommandType;
import ru.smcsystem.api.enumeration.MessageType;
import ru.smcsystem.api.enumeration.ObjectType;
import ru.smcsystem.api.exceptions.ModuleException;
import ru.smcsystem.api.module.Module;
import ru.smcsystem.api.tools.ConfigurationTool;
import ru.smcsystem.api.tools.execution.ExecutionContextTool;
import ru.smcsystem.smc.utils.ModuleUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// import ru.smcsystem.smc.utils.ValueArray;

public class Cycle implements Module {

    private enum Type {
        WHILE,
        DO_WHILE,
        FOR,
        FOR_EACH,
        FOR_EACH_DYNAMIC,
        FOR_EACH_OBJECT,
        ENDLESS_CYCLE,
        FOR_DYNAMIC,
        FOR_EACH_OBJECT_PARALLEL,
        FOR_EACH_ALL
    }

    private Type type;
    private List<Integer> idForBreak;
    private List<Integer> idForContinue;

    private enum CheckType {
        NO_DATA,
        ANY_DATA,
        IS_TRUE,
        IS_FALSE
    }

    private CheckType checkType;

    private Integer forCount;
    private Integer sleepTime;

    private enum SleepTimeType {
        FIXED,
        FLOATING
    }

    private SleepTimeType sleepTimeType;

    private Integer countThreads;
    // private String start;
    // private String end;

    @Override
    public void start(ConfigurationTool configurationTool) throws ModuleException {
        type = Type.valueOf((String) configurationTool.getSetting("type").orElseThrow(() -> new ModuleException("type setting")).getValue());

        String idForBreakString = (String) configurationTool.getSetting("idForBreak").orElseThrow(() -> new ModuleException("idForBreak setting not found")).getValue();
        idForBreak = null;
        if (!idForBreakString.isBlank())
            idForBreak = Arrays.stream(idForBreakString.split(",")).map(s -> Integer.valueOf(s.trim())).collect(Collectors.toList());

        String idForContinueString = (String) configurationTool.getSetting("idForContinue").orElseThrow(() -> new ModuleException("idForContinue setting not found")).getValue();
        idForContinue = null;
        if (!idForContinueString.isBlank())
            idForContinue = Arrays.stream(idForContinueString.split(",")).map(s -> Integer.valueOf(s.trim())).collect(Collectors.toList());
        checkType = CheckType.valueOf((String) configurationTool.getSetting("checkType").orElseThrow(() -> new ModuleException("checkType setting")).getValue());

        forCount = (Integer) configurationTool.getSetting("forCount").orElseThrow(() -> new ModuleException("forCount setting not found")).getValue();

        sleepTime = (Integer) configurationTool.getSetting("sleepTime").orElseThrow(() -> new ModuleException("sleepTime setting not found")).getValue();
        sleepTimeType = SleepTimeType.valueOf((String) configurationTool.getSetting("sleepTimeType").orElseThrow(() -> new ModuleException("sleepTimeType setting not found")).getValue());
        countThreads = (Integer) configurationTool.getSetting("countThreads").orElseThrow(() -> new ModuleException("countThreads setting not found")).getValue();
        // start = (String) configurationTool.getSetting("start").orElseThrow(() -> new ModuleException("start setting")).getValue();
        // end = (String) configurationTool.getSetting("end").orElseThrow(() -> new ModuleException("end setting")).getValue();
    }

    @Override
    public void update(ConfigurationTool ConfigurationTool) throws ModuleException {
        stop(ConfigurationTool);
        start(ConfigurationTool);
    }

    @Override
    public void process(ConfigurationTool ConfigurationTool, ExecutionContextTool executionContextTool) throws ModuleException {
        if (executionContextTool.getFlowControlTool().countManagedExecutionContexts() == 0)
            return;
        if (idForBreak != null && executionContextTool.getFlowControlTool().countManagedExecutionContexts() <= idForBreak.stream().mapToInt(n -> n).max().orElse(0)) {
            executionContextTool.addError("wrong idForBreak");
            return;
        }
        if (idForContinue != null && executionContextTool.getFlowControlTool().countManagedExecutionContexts() <= idForContinue.stream().mapToInt(n -> n).max().orElse(0)) {
            executionContextTool.addError("wrong idForContinue");
            return;
        }

        long counter = 1;
        long startTime = 0;
        switch (type) {
            case WHILE: {
                break_cycle:
                do {
                    startTime = System.currentTimeMillis();
                    for (int i = 0; i < executionContextTool.getFlowControlTool().countManagedExecutionContexts(); i++) {
                        if (executionContextTool.isNeedStop())
                            break break_cycle;

                        executionContextTool.getFlowControlTool().executeNow(CommandType.EXECUTE, i, null);

                        if (isBreak(executionContextTool, i, 0))
                            break break_cycle;

                        if (isContinue(executionContextTool, i))
                            break;
                    }
                    if (!sleep(executionContextTool, startTime)) {
                        executionContextTool.addMessage("force stop cycle");
                        break;
                    }
                    counter++;
                } while (true);
                break;
            }
            case DO_WHILE: {
                int lastId = executionContextTool.getFlowControlTool().countManagedExecutionContexts() - 1;
                break_cycle:
                do {
                    startTime = System.currentTimeMillis();
                    for (int i = 0; i < executionContextTool.getFlowControlTool().countManagedExecutionContexts(); i++) {
                        if (executionContextTool.isNeedStop())
                            break break_cycle;

                        executionContextTool.getFlowControlTool().executeNow(CommandType.EXECUTE, i, null);

                        if (isBreak(executionContextTool, i, lastId))
                            break break_cycle;

                        if (isContinue(executionContextTool, i))
                            break;
                    }
                    if (!sleep(executionContextTool, startTime)) {
                        executionContextTool.addMessage("force stop cycle");
                        break;
                    }
                    counter++;
                } while (true);
                break;
            }
            case FOR:
            case FOR_DYNAMIC: {
                Integer forCountCurrent = forCount;
                if (type == Type.FOR_DYNAMIC) {
                    if (executionContextTool.countSource() == 0)
                        break;
                    forCountCurrent = Stream.iterate(0, i -> i + 1)
                            .limit(executionContextTool.countSource())
                            .flatMap(i -> executionContextTool.getMessages(i).stream())
                            .flatMap(action -> action.getMessages().stream())
                            .filter(ModuleUtils::isNumber)
                            .map(ModuleUtils::getNumber)
                            .map(Number::intValue)
                            .findFirst()
                            .orElse(forCount);
                }

                startTime = System.currentTimeMillis();
                break_cycle:
                for (counter = 1; counter < forCountCurrent + 1; counter++) {
                    for (int i = 0; i < executionContextTool.getFlowControlTool().countManagedExecutionContexts(); i++) {
                        if (executionContextTool.isNeedStop())
                            break break_cycle;

                        executionContextTool.getFlowControlTool().executeNow(CommandType.EXECUTE, i, List.of(counter - 1));

                        if (isBreak(executionContextTool, i, null))
                            break break_cycle;

                        if (isContinue(executionContextTool, i))
                            break;
                    }
                    if (!sleep(executionContextTool, startTime)) {
                        executionContextTool.addMessage("force stop cycle");
                        break;
                    }
                }
                break;

            }
            case FOR_EACH:
            case FOR_EACH_DYNAMIC:
            case FOR_EACH_ALL: {
                if (executionContextTool.countSource() == 0)
                    break;

                List<Object> values = Stream.iterate(0, i -> i + 1)
                        .limit(executionContextTool.countSource())
                        .flatMap(i -> executionContextTool.getMessages(i).stream())
                        .flatMap(action -> action.getMessages().stream())
                        .map(IValue::getValue)
                        .collect(Collectors.toList());

                int start = 0;
                int countElements = -1;
                if (Type.FOR_EACH_DYNAMIC.equals(type)) {
                    start = 1;
                    countElements = !values.isEmpty() ? ((Number) values.get(0)).intValue() : 0;
                }
                List<List<Object>> params = new LinkedList<>();
                for (int j = start; j + forCount - 1 < values.size(); j = j + forCount)
                    params.add(values.subList(j, j + forCount));
                if (Type.FOR_EACH_ALL.equals(type)) {
                    int countLeft = values.size() % forCount;
                    params.add(values.subList(Math.max(values.size() - countLeft - 1, 0), values.size()));
                }

                counter = 1;
                break_cycle:
                for (List<Object> objects : params) {
                    if (countElements > -1 && countElements < counter)
                        break;
                    counter++;
                    startTime = System.currentTimeMillis();
                    for (int i = 0; i < executionContextTool.getFlowControlTool().countManagedExecutionContexts(); i++) {
                        if (executionContextTool.isNeedStop())
                            break break_cycle;

                        executionContextTool.getFlowControlTool().executeNow(CommandType.EXECUTE, i, objects);

                        if (isBreak(executionContextTool, i, null))
                            break break_cycle;

                        if (isContinue(executionContextTool, i))
                            break;
                    }
                    if (!sleep(executionContextTool, startTime)) {
                        executionContextTool.addMessage("force stop cycle");
                        break;
                    }
                }
                break;
            }
            case FOR_EACH_OBJECT:
            case FOR_EACH_OBJECT_PARALLEL: {
                if (executionContextTool.countSource() == 0)
                    break;

                LinkedList<IMessage> messages = Stream.iterate(0, i -> i + 1)
                        .limit(executionContextTool.countSource())
                        .flatMap(i -> executionContextTool.getMessages(i).stream())
                        .flatMap(action -> action.getMessages().stream())
                        // .map(IValue::getValue)
                        .collect(Collectors.toCollection(LinkedList::new));
                ObjectArray objectArray = ModuleUtils.deserializeToObject(messages);
                List<Object> params = new LinkedList<>();
                for (int i = 0; i < objectArray.size(); i++) {
                    Object poll = objectArray.get(i);
                    Object resultObject = null;
                    switch (objectArray.getType()) {
                        case OBJECT_ARRAY:
                            resultObject = (ObjectArray) poll;
                            break;
                        case OBJECT_ELEMENT:
                            resultObject = new ObjectArray(List.of(poll), ObjectType.OBJECT_ELEMENT);
                            break;
                        case VALUE_ANY:
                        case STRING:
                        case BYTE:
                        case SHORT:
                        case INTEGER:
                        case LONG:
                        case FLOAT:
                        case DOUBLE:
                        case BIG_INTEGER:
                        case BIG_DECIMAL:
                        case BYTES:
                            resultObject = List.of(poll);
                            break;
                    }
                    params.add(resultObject);
                }

                counter = 1;
                // int countFields = !values.isEmpty() ? ((Number) values.get(0)).intValue() : 0;
                if (type == Type.FOR_EACH_OBJECT_PARALLEL) {
                    for (int i = 0; i < params.size(); i += countThreads) {
                        counter++;
                        startTime = System.currentTimeMillis();
                        List<Long> threads = new ArrayList<>(countThreads);
                        for (int j = 0; j < countThreads; j++) {
                            if (params.size() <= i + j)
                                break;
                            Object resultObject = params.get(i + j);
                            threads.add(executionContextTool.getFlowControlTool().executeParallel(CommandType.EXECUTE, List.of(0), List.of(resultObject), null, null));
                        }
                        for (Long threadId : threads) {
                            do {
                                try {
                                    Thread.sleep(50);
                                } catch (InterruptedException e) {
                                }
                            } while (!executionContextTool.isNeedStop() && executionContextTool.getFlowControlTool().isThreadActive(threadId));
                            executionContextTool.getFlowControlTool().releaseThread(threadId);
                        }
                        if (!sleep(executionContextTool, startTime)) {
                            executionContextTool.addMessage("force stop cycle");
                            break;
                        }
                    }
                } else {
                    break_cycle:
                    for (Object resultObject : params) {
                        counter++;
                        startTime = System.currentTimeMillis();
                        for (int j = 0; j < executionContextTool.getFlowControlTool().countManagedExecutionContexts(); j++) {
                            if (executionContextTool.isNeedStop())
                                break break_cycle;

                            executionContextTool.getFlowControlTool().executeNow(CommandType.EXECUTE, j, List.of(resultObject));

                            if (isBreak(executionContextTool, j, null))
                                break break_cycle;

                            if (isContinue(executionContextTool, j))
                                break;
                        }
                        if (!sleep(executionContextTool, startTime)) {
                            executionContextTool.addMessage("force stop cycle");
                            break;
                        }
                    }
                }

                break;
            }
            case ENDLESS_CYCLE: {
                break_cycle:
                do {
                    startTime = System.currentTimeMillis();
                    for (int i = 0; i < executionContextTool.getFlowControlTool().countManagedExecutionContexts(); i++) {
                        if (executionContextTool.isNeedStop())
                            break break_cycle;

                        executionContextTool.getFlowControlTool().executeNow(CommandType.EXECUTE, i, null);

                        if (isBreak(executionContextTool, i, null))
                            break break_cycle;

                        if (isContinue(executionContextTool, i))
                            break;
                    }
                    if (!sleep(executionContextTool, startTime)) {
                        executionContextTool.addMessage("force stop cycle");
                        break;
                    }
                    counter++;
                } while (true);
                break;
            }
        }
        executionContextTool.addMessage(counter - 1);
    }

    private boolean isContinue(ExecutionContextTool executionContextTool, int id) {
        return (idForContinue != null && idForContinue.contains(id))
                && checkData(executionContextTool, id);
    }

    private boolean isBreak(ExecutionContextTool executionContextTool, int id, Integer equalsNumber) {
        return ((idForBreak != null && idForBreak.contains(id)) || (/*idForBreak == null && */equalsNumber != null && id == equalsNumber))
                && checkData(executionContextTool, id);
    }

    private boolean checkData(ExecutionContextTool executionContextTool, int id) {
        boolean result = false;
        switch (checkType) {
            case NO_DATA:
                result = executionContextTool.getFlowControlTool().getMessagesFromExecuted(id).stream()
                        .noneMatch(a -> a.getMessages().stream().anyMatch(m -> MessageType.DATA.equals(m.getMessageType())));
                break;
            case ANY_DATA:
                result = executionContextTool.getFlowControlTool().getMessagesFromExecuted(id).stream()
                        .anyMatch(a -> a.getMessages().stream().anyMatch(m -> MessageType.DATA.equals(m.getMessageType())));
                break;
            case IS_TRUE:
                result = executionContextTool.getFlowControlTool().getMessagesFromExecuted(id).stream()
                        .flatMap(a -> a.getMessages().stream())
                        .filter(ModuleUtils::isBoolean)
                        .map(m -> (Boolean) m.getValue())
                        .filter(v -> v)
                        .findFirst().orElse(false);
                break;
            case IS_FALSE: {
                List<IAction> data = executionContextTool.getFlowControlTool().getMessagesFromExecuted(id).stream()
                        .filter(ModuleUtils::hasData)
                        .collect(Collectors.toList());
                result = data.isEmpty() || !data.stream()
                        .flatMap(a -> a.getMessages().stream())
                        .filter(ModuleUtils::isBoolean)
                        .map(m -> (Boolean) m.getValue())
                        .filter(v -> !v)
                        .findFirst().orElse(false);
                break;
            }
        }
        // throw new ModuleException("wrong checkType");
        return result;
    }

    private boolean sleep(ExecutionContextTool executionContextTool, long startTime) {
        if (sleepTime != null && sleepTime > 0) {
            try {
                long currentSleepTime = sleepTime;
                if (SleepTimeType.FLOATING.equals(sleepTimeType)) {
                    currentSleepTime = sleepTime - (System.currentTimeMillis() - startTime);
                    if (currentSleepTime <= 0)
                        return true;
                }
                Thread.sleep(currentSleepTime);
            } catch (InterruptedException e) {
                if (!executionContextTool.isNeedStop()) {
                    e.printStackTrace();
                } else {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void stop(ConfigurationTool ConfigurationTool) throws ModuleException {
        type = null;
        idForBreak = null;
        idForContinue = null;
        forCount = null;
        sleepTime = null;
        sleepTimeType = null;
    }

}
