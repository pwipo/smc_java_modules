package ru.smcsystem.modules.flowControllerIfInteger;

import ru.smcsystem.api.dto.*;
import ru.smcsystem.api.enumeration.CommandType;
import ru.smcsystem.api.exceptions.ModuleException;
import ru.smcsystem.api.module.Module;
import ru.smcsystem.api.tools.ConfigurationTool;
import ru.smcsystem.api.tools.execution.ExecutionContextTool;
import ru.smcsystem.smc.utils.ModuleUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IfInteger implements Module {

    private Double number;
    private Type type;
    private Boolean hasElse;
    private Boolean isNeedReturnDataFromLast;
    private Boolean isNeedInsertSourceDataToExecutionContexts;
    private List<String> paths;

    private enum Type {
        EXIST,
        NOT_EXIST,
        EXIST_ANY,
        NOT_EXIST_ANY,
        NUMBER_EQUAL,
        NUMBER_MORE_THEN,
        NUMBER_MORE_THEN_OR_EQUAL,
        NUMBER_LESS_THEN,
        NUMBER_LESS_THEN_OR_EQUAL,
        EQUAL,
        NOT_EQUAL,
        HAS_ERROR,
        NO_ERROR,
        HAS_STRING,
        HAS_NUMBER,
        HAS_BYTES,
        HAS_OBJECT_ARRAY,
        IS_TRUE,
        IS_FALSE,
        HAS_BOOLEANS,
    }

    @Override
    public void start(ConfigurationTool ConfigurationTool) throws ModuleException {
        number = (Double) ConfigurationTool.getSetting("number").orElseThrow(() -> new ModuleException("number setting not found")).getValue();
        type = Type.valueOf((String) ConfigurationTool.getSetting("type").orElseThrow(() -> new ModuleException("type setting not found")).getValue());
        hasElse = Boolean.valueOf((String) ConfigurationTool.getSetting("hasElse").orElseThrow(() -> new ModuleException("hasElse setting not found")).getValue());
        isNeedReturnDataFromLast = Boolean.valueOf((String) ConfigurationTool.getSetting("isNeedReturnDataFromLast").orElseThrow(() -> new ModuleException("isNeedReturnDataFromLast setting not found")).getValue());
        isNeedInsertSourceDataToExecutionContexts = Boolean.valueOf((String) ConfigurationTool.getSetting("isNeedInsertSourceDataToExecutionContexts").orElseThrow(() -> new ModuleException("isNeedInsertSourceDataToExecutionContexts setting not found")).getValue());
        String strPaths = (String) ConfigurationTool.getSetting("paths").orElseThrow(() -> new ModuleException("paths setting not found")).getValue();
        paths = Arrays.stream(strPaths.split("::"))
                .filter(s -> !s.isBlank())
                .map(String::trim)
                .collect(Collectors.toList());
    }

    @Override
    public void update(ConfigurationTool ConfigurationTool) throws ModuleException {
        stop(ConfigurationTool);
        start(ConfigurationTool);
    }

    @Override
    public void process(ConfigurationTool ConfigurationTool, ExecutionContextTool executionContextTool) throws ModuleException {
        if (hasElse && executionContextTool.getFlowControlTool().countManagedExecutionContexts() != 2)
            throw new ModuleException("need 2 managed execution context for implement if-else logic");
        boolean execute[] = new boolean[]{false};

        boolean checkNumbers = Type.NUMBER_EQUAL.equals(type) || Type.NUMBER_MORE_THEN.equals(type) || Type.NUMBER_MORE_THEN_OR_EQUAL.equals(type) || Type.NUMBER_LESS_THEN.equals(type) || Type.NUMBER_LESS_THEN_OR_EQUAL.equals(type);
        boolean checkType = Type.HAS_STRING.equals(type) || Type.HAS_NUMBER.equals(type) || Type.HAS_BYTES.equals(type) || Type.HAS_OBJECT_ARRAY.equals(type) || Type.HAS_BOOLEANS.equals(type);
        boolean checkEq = Type.EQUAL.equals(type) || Type.NOT_EQUAL.equals(type) || Type.IS_TRUE.equals(type) || Type.IS_FALSE.equals(type);
        boolean checkError = Type.HAS_ERROR.equals(type) || Type.NO_ERROR.equals(type);

        List<Object> sourceValues = isNeedInsertSourceDataToExecutionContexts ? new LinkedList<>() : null;
        List<List<IAction>> sources = new LinkedList<>();
        boolean newDataExistAll = false;
        int countExists = 0;
        int countErrors = 0;
        for (int i = 0; i < executionContextTool.countSource(); i++) {
            if (checkError) {
                List<ICommand> commands = executionContextTool.getCommands(i);
                if (commands.stream().anyMatch(ModuleUtils::hasErrors))
                    countErrors++;
                if (isNeedInsertSourceDataToExecutionContexts)
                    sources.add(executionContextTool.getMessages(i));
            } else {
                List<IAction> messages = executionContextTool.getMessages(i);
                if (checkNumbers || checkType || checkEq || isNeedInsertSourceDataToExecutionContexts)
                    sources.add(messages);
                if (!messages.isEmpty())
                    countExists++;
            }
        }

        sources.forEach(messages -> {
            if (checkNumbers) {
                messages.forEach(a ->
                        a.getMessages().stream()
                                .flatMap(m -> {
                                    if (ModuleUtils.isNumber(m)) {
                                        return Stream.of(ModuleUtils.getNumber(m));
                                    } else {
                                        return getFromPaths(m).stream()
                                                .filter(ModuleUtils::isNumber)
                                                .map(ModuleUtils::getNumber);
                                    }
                                })
                                .forEach(n -> {
                                    double value = n.doubleValue();
                                    // boolean execute = false;
                                    switch (type) {
                                        case NUMBER_EQUAL:
                                            if (value == number)
                                                execute[0] = true;
                                            break;
                                        case NUMBER_MORE_THEN:
                                            if (value > number)
                                                execute[0] = true;
                                            break;
                                        case NUMBER_MORE_THEN_OR_EQUAL:
                                            if (value >= number)
                                                execute[0] = true;
                                            break;
                                        case NUMBER_LESS_THEN:
                                            if (value < number)
                                                execute[0] = true;
                                            break;
                                        case NUMBER_LESS_THEN_OR_EQUAL:
                                            if (value <= number)
                                                execute[0] = true;
                                            break;
                                    }
                                }));
            } else if (checkType) {
                List<IAction> actions = messages.stream().filter(ModuleUtils::hasData).collect(Collectors.toList());
                for (int i = 0; i < actions.size(); i++) {
                    IAction iAction = actions.get(i);
                    boolean checkResult = false;
                    switch (type) {
                        case HAS_STRING:
                            checkResult = iAction.getMessages().stream().anyMatch(ModuleUtils::isString);
                            break;
                        case HAS_NUMBER:
                            checkResult = iAction.getMessages().stream().anyMatch(ModuleUtils::isNumber);
                            break;
                        case HAS_BYTES:
                            checkResult = iAction.getMessages().stream().anyMatch(ModuleUtils::isBytes);
                            break;
                        case HAS_OBJECT_ARRAY:
                            checkResult = iAction.getMessages().stream().anyMatch(ModuleUtils::isObjectArray);
                            break;
                        case HAS_BOOLEANS:
                            checkResult = iAction.getMessages().stream().anyMatch(ModuleUtils::isBoolean);
                            break;
                    }
                    if (checkResult) {
                        execute[0] = true;
                        break;
                    }
                }
            }
            if (isNeedInsertSourceDataToExecutionContexts)
                messages.forEach(a -> a.getMessages().forEach(m -> sourceValues.add(m.getValue())));
        });
        switch (type) {
            case EXIST:
                execute[0] = countExists > 0 && executionContextTool.countSource() == countExists;
                break;
            case NOT_EXIST:
                execute[0] = countExists == 0;
                break;
            case EXIST_ANY:
                execute[0] = countExists > 0;
                break;
            case NOT_EXIST_ANY:
                execute[0] = countExists < executionContextTool.countSource();
                break;
            case NUMBER_EQUAL:
                break;
            case NUMBER_MORE_THEN:
                break;
            case NUMBER_MORE_THEN_OR_EQUAL:
                break;
            case NUMBER_LESS_THEN:
                break;
            case NUMBER_LESS_THEN_OR_EQUAL:
                break;
            case EQUAL:
            case NOT_EQUAL:
                execute[0] = false;
                if (sources.size() >= 2) {
                    List<Object> source1 = sources.get(0).stream()
                            .flatMap(a -> a.getMessages().stream())
                            .flatMap(m -> {
                                if (ModuleUtils.isObjectArray(m) && !paths.isEmpty()) {
                                    return getFromPaths(m).stream()
                                            .map(ObjectField::getValue);
                                } else {
                                    return Stream.of(m.getValue());
                                }
                            })
                            .collect(Collectors.toList());
                    List<Object> source2 = sources.get(1).stream()
                            .flatMap(a -> a.getMessages().stream())
                            .flatMap(m -> {
                                if (ModuleUtils.isObjectArray(m) && !paths.isEmpty()) {
                                    return getFromPaths(m).stream()
                                            .map(ObjectField::getValue);
                                } else {
                                    return Stream.of(m.getValue());
                                }
                            })
                            .collect(Collectors.toList());
                    if (source1.size() != source2.size()) {
                        if (type == Type.NOT_EQUAL)
                            execute[0] = true;
                        break;
                    }
                    int size = source1.size();
                    execute[0] = true;
                    for (int i = 0; i < size; i++) {
                        if (!Objects.equals(source1.get(i), source2.get(i))) {
                            execute[0] = false;
                            break;
                        }
                    }
                    if (type == Type.NOT_EQUAL)
                        execute[0] = !execute[0];
                } else if (sources.size() == 1 && type == Type.NOT_EQUAL) {
                    execute[0] = true;
                }
                break;
            case HAS_ERROR:
                execute[0] = countErrors > 0 && executionContextTool.countSource() == countErrors;
                break;
            case NO_ERROR:
                execute[0] = countErrors == 0;
                break;
            case IS_TRUE:
                execute[0] = sources.stream()
                        .flatMap(Collection::stream)
                        .flatMap(a -> a.getMessages().stream())
                        .filter(ModuleUtils::isBoolean)
                        .map(m -> (Boolean) m.getValue())
                        .filter(v -> v)
                        .findFirst().orElse(false);
                break;
            case IS_FALSE: {
                List<IAction> data = sources.stream()
                        .flatMap(Collection::stream)
                        .filter(ModuleUtils::hasData)
                        .collect(Collectors.toList());
                execute[0] = data.isEmpty() || !data.stream()
                        .flatMap(a -> a.getMessages().stream())
                        .filter(ModuleUtils::isBoolean)
                        .map(m -> (Boolean) m.getValue())
                        .filter(v -> !v)
                        .findFirst().orElse(false);
                break;
            }
        }

        Optional<IAction> dataFromExecuted = Optional.empty();
        if (!hasElse) {
            if (execute[0] && executionContextTool.getFlowControlTool().countManagedExecutionContexts() > 0) {
                for (int j = 0; j < executionContextTool.getFlowControlTool().countManagedExecutionContexts(); j++)
                    executionContextTool.getFlowControlTool().executeNow(CommandType.EXECUTE, j, sourceValues);
                if (isNeedReturnDataFromLast)
                    dataFromExecuted = executionContextTool.getFlowControlTool().getMessagesFromExecuted(executionContextTool.getFlowControlTool().countManagedExecutionContexts() - 1).stream().findAny();
            }
        } else {
            if (execute[0]) {
                executionContextTool.getFlowControlTool().executeNow(CommandType.EXECUTE, 0, sourceValues);
                if (isNeedReturnDataFromLast)
                    dataFromExecuted = executionContextTool.getFlowControlTool().getMessagesFromExecuted(0).stream().findAny();
            } else {
                executionContextTool.getFlowControlTool().executeNow(CommandType.EXECUTE, 1, sourceValues);
                if (isNeedReturnDataFromLast)
                    dataFromExecuted = executionContextTool.getFlowControlTool().getMessagesFromExecuted(1).stream().findAny();
            }
        }
        dataFromExecuted.ifPresent(action ->
                executionContextTool.addMessage(action.getMessages().stream().map(IValue::getValue).collect(Collectors.toList())));
    }

    private List<ObjectField> getFromPaths(IMessage m) {
        if (paths.isEmpty() || !ModuleUtils.isObjectArray(m))
            return List.of();
        ObjectArray objectArray = ModuleUtils.getObjectArray(m);
        if (!ModuleUtils.isArrayContainObjectElements(objectArray))
            return List.of();
        List<List<ObjectField>> fields = ModuleUtils.findFields(objectArray, paths);
        List<ObjectField> resultFields = new ArrayList<>(objectArray.size() * paths.size() + 1);
        for (int i = 0; i < objectArray.size(); i++) {
            for (int j = 0; j < paths.size(); j++) {
                if (fields.size() > j) {
                    List<ObjectField> fields1 = fields.get(j);
                    if (fields1.size() > i) {
                        resultFields.add(fields1.get(i));
                    }
                }
            }
        }
        return resultFields;
    }

    @Override
    public void stop(ConfigurationTool ConfigurationTool) throws ModuleException {
        number = null;
        type = null;
        hasElse = null;
        isNeedReturnDataFromLast = false;
        isNeedInsertSourceDataToExecutionContexts = false;
    }

}
