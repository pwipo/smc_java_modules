package ru.smcsystem.smcmodules.module.message.getter;

import org.apache.commons.collections4.CollectionUtils;
import ru.smcsystem.api.dto.*;
import ru.smcsystem.api.enumeration.ActionType;
import ru.smcsystem.api.enumeration.ObjectType;
import ru.smcsystem.api.exceptions.ModuleException;
import ru.smcsystem.api.module.Module;
import ru.smcsystem.api.tools.ConfigurationTool;
import ru.smcsystem.api.tools.execution.ExecutionContextTool;
import ru.smcsystem.smc.utils.ModuleUtils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class CommandGetter implements Module {

    private enum Type {
        COUNT, GET_ALL, GET_FROM_TO, GET_LAST, FIND_BY_DATE_RANGE_AND_CONTAIN_VALUES_AS_COMMANDS, GET_ALL_AS_COMMANDS, GET_FROM_TO_AS_COMMANDS, GET_LAST_AS_COMMANDS, GET_LAST_AS_COMMANDS_EXECUTE_NOT_EMPTY
    }

    @Override
    public void start(ConfigurationTool configurationTool) throws ModuleException {
    }

    @Override
    public void process(ConfigurationTool configurationTool, ExecutionContextTool executionContextTool) throws ModuleException {
        if (Objects.equals(executionContextTool.getType(), "default")) {
            executionContextTool.getMessages(0).forEach(a -> {
                LinkedList<IMessage> messages = new LinkedList<>(a.getMessages());
                try {
                    while (!messages.isEmpty()) {
                        Type type = Type.values()[ModuleUtils.getNumber(messages.poll()).intValue() - 1];
                        process(executionContextTool, type, messages, 1);
                    }
                } catch (Exception e) {
                    executionContextTool.addError(ModuleUtils.getErrorMessageOrClassName(e));
                    configurationTool.loggerWarn(ModuleUtils.getStackTraceAsString(e));
                }
            });
        } else {
            List<IMessage> iMessages = ModuleUtils.getLastActionWithData(executionContextTool.getMessages(0)).map(IAction::getMessages).orElse(List.of());
            Type type = Type.valueOf(executionContextTool.getType().toUpperCase());
            try {
                LinkedList<IMessage> messages = new LinkedList<>(iMessages);
                int id = type == Type.COUNT || type == Type.GET_ALL || type == Type.GET_ALL_AS_COMMANDS ? 0 : 1;
                process(executionContextTool, type, messages, id);
            } catch (Exception e) {
                executionContextTool.addError(ModuleUtils.getErrorMessageOrClassName(e));
                configurationTool.loggerWarn(ModuleUtils.getStackTraceAsString(e));
            }
        }
    }

    private void process(ExecutionContextTool executionContextTool, Type type, LinkedList<IMessage> messages, int id) {
        switch (type) {
            case COUNT:
                //count
                executionContextTool.addMessage(executionContextTool.countCommands(id));
                break;
            case GET_ALL:
            case GET_ALL_AS_COMMANDS: {
                //get all
                List<ICommand> commands = executionContextTool.getCommands(id);
                if (type == Type.GET_ALL_AS_COMMANDS) {
                    printCommandsAsCommands(executionContextTool, commands);
                } else {
                    printMessages(executionContextTool, commands);
                }
                break;
            }
            case GET_FROM_TO:
            case GET_FROM_TO_AS_COMMANDS: {
                if (messages.size() < 2) {
                    executionContextTool.addError("need 2 params");
                    return;
                }
                int size = executionContextTool.countCommands(id);
                int start = ModuleUtils.getNumber(messages.poll()).intValue();
                int end = ModuleUtils.getNumber(messages.poll()).intValue();
                if (end < start) {
                    executionContextTool.addError("wrong inputs");
                    break;
                }
                if (start > size)
                    break;
                if (size < end)
                    end = size;
                List<ICommand> commands = executionContextTool.getCommands(id, start, end);
                if (type == Type.GET_FROM_TO_AS_COMMANDS) {
                    printCommandsAsCommands(executionContextTool, commands);
                } else {
                    printMessages(executionContextTool, commands);
                }
                break;
            }
            case GET_LAST:
            case GET_LAST_AS_COMMANDS: {
                if (messages.isEmpty()) {
                    executionContextTool.addError("need 1 param");
                    return;
                }
                int count = ModuleUtils.getNumber(messages.poll()).intValue();
                int size = executionContextTool.countCommands(id);
                if (size < count)
                    count = size;
                List<ICommand> commands = executionContextTool.getCommands(id, size - count, size);
                if (type == Type.GET_LAST_AS_COMMANDS) {
                    printCommandsAsCommands(executionContextTool, commands);
                } else {
                    printMessages(executionContextTool, commands);
                }
                break;
            }
            case FIND_BY_DATE_RANGE_AND_CONTAIN_VALUES_AS_COMMANDS: {
                if (messages.size() < 3) {
                    executionContextTool.addError("need minimum 3 params");
                    return;
                }
                long from = ModuleUtils.getNumber(messages.poll()).longValue();
                long to = ModuleUtils.getNumber(messages.poll()).longValue();
                int countValues = ModuleUtils.getNumber(messages.poll()).intValue();
                List<IMessage> values = new ArrayList<>(countValues + 1);
                for (int i = 0; i < countValues; i++)
                    values.add(messages.poll());
                int size = executionContextTool.countCommands(id);
                int pageSize = !messages.isEmpty() && ModuleUtils.isNumber(messages.peek()) ? ModuleUtils.getNumber(messages.poll()).intValue() : 1000;
                int countPages = size / pageSize;
                List<List<IMessage>> resultMessages = new LinkedList<>();
                if (size > 0) {
                    List<List<List<IMessage>>> resultMessagesReverse = new ArrayList<>(countPages + 1);
                    for (int i = countPages; i >= 0; i--) {
                        int fromIndex = Math.min(i * pageSize, size);
                        int toIndex = Math.min(i * pageSize + pageSize, size);
                        if (fromIndex >= toIndex)
                            continue;
                        List<ICommand> commands = executionContextTool.getCommands(id, fromIndex, toIndex);
                        if (CollectionUtils.isEmpty(commands))
                            continue;
                        List<List<IMessage>> dataMessages = commands.stream()
                                .flatMap(c -> c.getActions().stream())
                                .filter(a2 -> a2.getType() == ActionType.EXECUTE)
                                .map(IAction::getMessages)
                                .filter(CollectionUtils::isNotEmpty)
                                .collect(Collectors.toList());
                        if (CollectionUtils.isEmpty(dataMessages))
                            continue;
                        List<List<IMessage>> filteredList = dataMessages.stream()
                                .filter(lst -> {
                                    long time = lst.get(0).getDate().getTime();
                                    return time >= from && time < to;
                                })
                                .filter(lst -> values.isEmpty() || values.stream().allMatch(m -> lst.stream()
                                        .anyMatch(m2 -> m.getType() == m2.getType() && Objects.equals(m.getValue(), m2.getValue()))))
                                .collect(Collectors.toList());
                        if (!filteredList.isEmpty())
                            resultMessagesReverse.add(filteredList);
                        if (dataMessages.get(dataMessages.size() - 1).get(0).getDate().getTime() < from)
                            break;
                    }
                    for (int i = resultMessagesReverse.size() - 1; i >= 0; i--)
                        resultMessages.addAll(resultMessagesReverse.get(i));
                }
                printMessagesAsCommands(executionContextTool, resultMessages);
                break;
            }
            case GET_LAST_AS_COMMANDS_EXECUTE_NOT_EMPTY: {
                if (messages.isEmpty()) {
                    executionContextTool.addError("need 1 param");
                    return;
                }
                int count = ModuleUtils.getNumber(messages.poll()).intValue();
                int size = executionContextTool.countCommands(id);
                List<List<IMessage>> resultMessages = new LinkedList<>();
                if (size > 0) {
                    int pageSize = 10;
                    int countPages = size / pageSize;
                    List<List<List<IMessage>>> resultMessagesReverse = new ArrayList<>(countPages + 1);
                    for (int i = countPages; i >= 0; i--) {
                        int fromIndex = Math.min(i * pageSize, size);
                        int toIndex = Math.min(i * pageSize + pageSize, size);
                        if (fromIndex >= toIndex)
                            continue;
                        List<ICommand> commands = executionContextTool.getCommands(id, fromIndex, toIndex);
                        if (CollectionUtils.isEmpty(commands))
                            continue;
                        List<List<IMessage>> dataMessages = commands.stream()
                                .flatMap(c -> c.getActions().stream())
                                .filter(ModuleUtils::hasData)
                                .map(IAction::getMessages)
                                .collect(Collectors.toList());
                        if (CollectionUtils.isEmpty(dataMessages))
                            continue;
                        if (!dataMessages.isEmpty()) {
                            resultMessagesReverse.add(dataMessages);
                            if (resultMessagesReverse.size() >= count)
                                break;
                        }
                    }
                    for (int i = resultMessagesReverse.size() - 1; i >= 0; i--)
                        resultMessages.addAll(resultMessagesReverse.get(i));

                }
                if (resultMessages.size() > count)
                    resultMessages = resultMessages.subList(resultMessages.size() - count, resultMessages.size());
                printMessagesAsCommands(executionContextTool, resultMessages);
                break;
            }
        }
    }

    private void printMessagesAsCommands(ExecutionContextTool executionContextTool, List<List<IMessage>> commands) {
        ObjectArray objectArray = new ObjectArray(
                commands.stream()
                        .filter(CollectionUtils::isNotEmpty)
                        .map(lst -> {
                            ObjectField objectField = messagesToField(lst);
                            if (objectField != null) {
                                return new ObjectElement(List.of(new ObjectField("date", lst.get(0).getDate().getTime()), objectField));
                            } else {
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()),
                ObjectType.OBJECT_ELEMENT);
        if (objectArray.size() > 0)
            executionContextTool.addMessage(objectArray);
    }

    private ObjectField messagesToField(List<IMessage> lst) {
        LinkedList<IMessage> messages = new LinkedList<>(lst);
        ObjectArray objectArrayValue = ModuleUtils.deserializeToObject(messages);
        ObjectField of;
        if (messages.isEmpty() && objectArrayValue.size() > 0) {
            of = new ObjectField("values", objectArrayValue);
        } else if (!messages.isEmpty()) {
            ObjectArray objectArray = new ObjectArray(lst.stream()
                    .filter(m -> !ModuleUtils.isObjectArray(m))
                    .map(IValue::getValue)
                    .collect(Collectors.toList()),
                    ObjectType.VALUE_ANY);
            ObjectArray objectArray2 = new ObjectArray(lst.stream()
                    .filter(ModuleUtils::isObjectArray)
                    .map(IValue::getValue)
                    .collect(Collectors.toList()),
                    ObjectType.OBJECT_ARRAY);
            if (objectArray2.size() == 1)
                objectArray2 = (ObjectArray) objectArray2.get(0);
            ObjectArray objectArrayResult = null;
            if (objectArray.size() > 0 && objectArray2.size() > 0) {
                objectArrayResult = new ObjectArray(List.of(objectArray, objectArray2), ObjectType.OBJECT_ARRAY);
            } else if (objectArray.size() > 0) {
                objectArrayResult = objectArray;
            } else if (objectArray2.size() > 0) {
                objectArrayResult = objectArray2;
            }
            of = objectArrayResult != null ? new ObjectField("values", objectArrayResult) : null;
        } else {
            of = null;
        }
        return of;
    }

    private void printCommandsAsCommands(ExecutionContextTool executionContextTool, List<ICommand> commands) {
        ObjectArray objectArray = new ObjectArray(
                commands.stream()
                        .flatMap(c -> c.getActions().stream())
                        .filter(a -> CollectionUtils.isNotEmpty(a.getMessages()))
                        .map(a -> {
                            ObjectField objectField = messagesToField(a.getMessages());
                            if (objectField != null) {
                                return new ObjectElement(List.of(new ObjectField("date", a.getMessages().get(0).getDate().getTime()), objectField));
                            } else {
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()),
                ObjectType.OBJECT_ELEMENT);
        if (objectArray.size() > 0)
            executionContextTool.addMessage(objectArray);
    }

    private void printMessages(ExecutionContextTool executionContextTool, List<ICommand> commands) {
        ObjectArray objectArray = new ObjectArray(
                commands.stream()
                        .flatMap(c -> c.getActions().stream())
                        .flatMap(a2 -> a2.getMessages().stream())
                        .map(m -> new ObjectElement(List.of(new ObjectField("date", m.getDate().getTime()), new ObjectField("value", m))))
                        .collect(Collectors.toList()),
                ObjectType.OBJECT_ELEMENT);
        if (objectArray.size() > 0)
            executionContextTool.addMessage(objectArray);
    }

    @Override
    public void update(ConfigurationTool configurationTool) throws ModuleException {
    }

    @Override
    public void stop(ConfigurationTool configurationTool) throws ModuleException {
    }

}
