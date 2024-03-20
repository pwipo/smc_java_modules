package ru.smcsystem.smcmodules.module;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import ru.smcsystem.api.dto.IAction;
import ru.smcsystem.api.dto.ICommand;
import ru.smcsystem.api.dto.IMessage;
import ru.smcsystem.api.dto.IValue;
import ru.smcsystem.api.enumeration.MessageType;
import ru.smcsystem.api.enumeration.ValueType;
import ru.smcsystem.api.exceptions.ModuleException;
import ru.smcsystem.api.module.Module;
import ru.smcsystem.api.tools.ConfigurationTool;
import ru.smcsystem.api.tools.execution.ExecutionContextTool;
import ru.smcsystem.smc.utils.ModuleUtils;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Get implements Module {
    enum ProcessingType {
        EACH_ACTION,
        EACH_SOURCE,
        ALL
    }

    private ProcessingType processingType;

    enum Type {
        WORK_DATA,
        DATA,
        ERROR,
        DATA_AND_ERROR,
        ALL,
        RANDOM,
        SOURCE_ID,
        SOURCE_ID_EMPTY_OR_ERRORS
    }

    private Type type;

    enum ValueTypes {
        ALL,
        STRING,
        BYTE,
        SHORT,
        INTEGER,
        LONG,
        BIG_INTEGER,
        FLOAT,
        DOUBLE,
        BIG_DECIMAL,
        BYTES,
        NUMBER,
        BYTES_SIZE
    }

    enum DefaultValueType {
        STATIC,
        DYNAMIC
    }

    private ValueTypes valueType;
    private List<List<Integer>> elementIds;
    private DefaultValueType defaultValueType;
    private List<Object> defaultValues;
    private Pattern base64;
    private Boolean outputErrorAsData;

    @Override
    public void start(ConfigurationTool configurationTool) throws ModuleException {
        processingType = ProcessingType.valueOf((String) configurationTool.getSetting("processingType").orElseThrow(() -> new ModuleException("processingType setting")).getValue());
        type = Type.valueOf((String) configurationTool.getSetting("type").orElseThrow(() -> new ModuleException("type setting")).getValue());
        valueType = ValueTypes.valueOf((String) configurationTool.getSetting("valueType").orElseThrow(() -> new ModuleException("valueType setting")).getValue());
        String elements = (String) configurationTool.getSetting("ids").orElseThrow(() -> new ModuleException("ids setting not found")).getValue();
        defaultValueType = DefaultValueType.valueOf((String) configurationTool.getSetting("defaultValueType").orElseThrow(() -> new ModuleException("defaultValueType setting")).getValue());
        String defaultValuesStr = (String) configurationTool.getSetting("defaultValues").orElseThrow(() -> new ModuleException("defaultValues setting not found")).getValue();
        outputErrorAsData = (Boolean) configurationTool.getSetting("outputErrorAsData").orElseThrow(() -> new ModuleException("outputErrorAsData setting not found")).getValue();
        elementIds = new ArrayList<>();
        if (StringUtils.isNotBlank(elements)) {
            for (String element : elements.split(",")) {
                element = element.trim();
                List<Integer> ids = new ArrayList<>();
                if (NumberUtils.isNumber(element)) {
                    ids.add(NumberUtils.toInt(element));
                } else if (element.contains(":") && element.split(":").length == 2) {
                    Arrays.stream(element.split(":"))
                            .map(String::trim)
                            .filter(NumberUtils::isNumber)
                            .map(NumberUtils::toInt)
                            .forEach(ids::add);
                } else {
                    throw new ModuleException("wrong ids " + element);
                }
                elementIds.add(ids);
            }
        }
        base64 = Pattern.compile("/^([A-Za-z0-9+\\/]{4})*([A-Za-z0-9+\\/]{4}|[A-Za-z0-9+\\/]{3}=|[A-Za-z0-9+\\/]{2}==)$/");
        defaultValues = new ArrayList<>();
        if (StringUtils.isNotBlank(defaultValuesStr)) {
            for (String element : defaultValuesStr.split("::"))
                defaultValues.add(DefaultValueType.STATIC.equals(defaultValueType) ? fromString(element, base64) : NumberUtils.toInt(element));
        }
    }

    @Override
    public void update(ConfigurationTool configurationTool) throws ModuleException {
        stop(configurationTool);
        start(configurationTool);
    }

    @Override
    public void process(ConfigurationTool configurationTool, ExecutionContextTool executionContextTool) throws ModuleException {
        // Function<Integer, List<ru.smcsystem.api.dto.IAction>> getMessagesFuncTmp = null;
        List<List<IAction>> actions = null;
        switch (type) {
            case WORK_DATA:
            case RANDOM:
                // getMessagesFuncTmp = executionContextTool::getMessages;
                actions = Stream.iterate(0, n -> n + 1)
                        .limit(executionContextTool.countSource())
                        .map(executionContextTool::getMessages)
                        .collect(Collectors.toList());
                break;
            case DATA:
            case ERROR:
            case DATA_AND_ERROR:
            case ALL:
                // getMessagesFuncTmp = executionContextTool::getCommands;
                actions = Stream.iterate(0, n -> n + 1)
                        .limit(executionContextTool.countSource())
                        .map(executionContextTool::getCommands)
                        .flatMap(Collection::stream)
                        .map(ICommand::getActions)
                        .collect(Collectors.toList());
                break;
            case SOURCE_ID:
            case SOURCE_ID_EMPTY_OR_ERRORS: {
                List<Boolean> hasData = Stream.iterate(0, n -> n + 1)
                        .limit(executionContextTool.countSource())
                        .map(executionContextTool::getCommands)
                        .map(commands -> !commands.isEmpty() && commands.stream().anyMatch(c -> !ModuleUtils.hasErrors(c) && ModuleUtils.hasData(c)))
                        .collect(Collectors.toList());
                for (int i = 0; i < hasData.size(); i++) {
                    if ((Type.SOURCE_ID.equals(type) && hasData.get(i)) || (Type.SOURCE_ID_EMPTY_OR_ERRORS.equals(type) && !hasData.get(i)))
                        executionContextTool.addMessage(i);
                }
                return;
            }
        }

        List<List<IMessage>> messagesList = null;
        switch (processingType) {
            case EACH_ACTION:
                messagesList = actions.stream()
                        .flatMap(Collection::stream)
                        .map(IAction::getMessages)
                        .collect(Collectors.toList());
                break;
            case EACH_SOURCE:
                messagesList = actions.stream()
                        .map(list -> list.stream().flatMap(a -> a.getMessages().stream()).collect(Collectors.toList()))
                        .collect(Collectors.toList());
                break;
            case ALL:
                messagesList = new LinkedList<>();
                messagesList.add(
                        actions.stream()
                                .flatMap(Collection::stream)
                                .flatMap(a -> a.getMessages().stream())
                                .collect(Collectors.toList()));
                break;
        }
        switch (type) {
            case WORK_DATA:
            case DATA:
            case ERROR:
            case DATA_AND_ERROR:
            case ALL:
                if (messagesList.isEmpty() && !defaultValues.isEmpty()) {
                    process(executionContextTool, new ArrayList<>());
                } else {
                    messagesList.forEach(messages -> process(executionContextTool, messages));
                }
                break;
            case RANDOM:
                Integer count = CollectionUtils.isNotEmpty(elementIds) ? elementIds.get(0).get(0) : 1;
                Random rnd = new Random();
                executionContextTool.addMessage(
                        messagesList.stream()
                                .filter(CollectionUtils::isNotEmpty)
                                .map(messages -> messages.stream().filter(this::filter).collect(Collectors.toList()))
                                .filter(CollectionUtils::isNotEmpty)
                                .flatMap(messages -> {
                                    List<Object> result = new LinkedList<>();
                                    for (int i = 0; i < count; i++)
                                        result.add(getValue(messages.get(rnd.nextInt(messages.size()))));
                                    return result.stream();
                                })
                                .collect(Collectors.toList()));
                break;
        }
    }

    private void process(ExecutionContextTool executionContextTool, List<IMessage> messagesLst) {

        switch (type) {
            case DATA:
                messagesLst = messagesLst.stream()
                        .filter(m -> MessageType.DATA.equals(m.getMessageType()))
                        .collect(Collectors.toList());
                break;
            case ERROR:
                messagesLst = messagesLst.stream()
                        .filter(m -> MessageType.ERROR.equals(m.getMessageType()))
                        .collect(Collectors.toList());
                break;
            case DATA_AND_ERROR:
                messagesLst = messagesLst.stream()
                        .filter(m -> MessageType.DATA.equals(m.getMessageType()) || MessageType.ERROR.equals(m.getMessageType()))
                        .collect(Collectors.toList());
                break;
        }

        final List<IMessage> messagesInput = messagesLst.stream().filter(this::filter).collect(Collectors.toList());
        if (!elementIds.isEmpty()) {
            for (int i = 0; i < elementIds.size(); i++) {
                List<Integer> ids = elementIds.get(i);
                List<IMessage> messages = new ArrayList<>();
                if (ids.size() == 1) {
                    Integer id = ids.get(0);
                    if (0 <= id && id < messagesInput.size()) {
                        messages.add(messagesInput.get(id));
                    } else if (id < 0 && (messagesInput.size() + id) >= 0) {
                        messages.add(messagesInput.get(messagesInput.size() + id));
                    }
                } else if (ids.size() == 2) {
                    Integer idStart = ids.get(0);
                    Integer idStop = ids.get(1);
                    if (0 <= idStart && idStart < messagesInput.size() && 0 <= idStop && idStop < messagesInput.size()) {
                        if (idStart < idStop) {
                            for (int j = idStart; j <= idStop; j++)
                                messages.add(messagesInput.get(j));
                        } else {
                            for (int j = idStart; j >= idStop; j--)
                                messages.add(messagesInput.get(j));
                        }
                    } else if (idStart < 0 && (messagesInput.size() + idStart) >= 0 && idStop < 0 && (messagesInput.size() + idStop) >= 0) {
                        if (idStart < idStop) {
                            for (int j = idStart; j <= idStop; j++)
                                messages.add(messagesInput.get(messagesInput.size() + j));
                        } else {
                            for (int j = idStart; j >= idStop; j--)
                                messages.add(messagesInput.get(messagesInput.size() + j));
                        }
                    } else if (0 <= idStart && idStart < messagesInput.size() && idStop < 0 && (messagesInput.size() + idStop) >= 0) {
                        int idStopReal = messagesInput.size() + idStop;
                        for (int j = idStart; j <= idStopReal; j++)
                            messages.add(messagesInput.get(j));
                    }
                }
                if (messages.isEmpty() && defaultValues.size() > i) {
                    executionContextTool.addMessage(List.of(getFromDefault(i, messagesInput)));
                } else {
                    if (outputErrorAsData) {
                        executionContextTool.addMessage(messages.stream().map(IValue::getValue).collect(Collectors.toList()));
                    } else {
                        messages.forEach(m -> {
                            if (MessageType.ERROR.equals(m.getMessageType()) || MessageType.ACTION_ERROR.equals(m.getMessageType())) {
                                executionContextTool.addError(List.of(m.getValue()));
                            } else {
                                executionContextTool.addMessage(List.of(m.getValue()));
                            }
                        });
                    }
                }
            }
        } else {
            if (messagesInput.isEmpty()) {
                for (int i = 0; i < defaultValues.size(); i++)
                    executionContextTool.addMessage(List.of(getFromDefault(i, messagesInput)));
            } else {
                if (outputErrorAsData) {
                    executionContextTool.addMessage(messagesInput.stream().map(IValue::getValue).collect(Collectors.toList()));
                } else {
                    messagesInput.forEach(m -> {
                        if (MessageType.ERROR.equals(m.getMessageType()) || MessageType.ACTION_ERROR.equals(m.getMessageType())) {
                            executionContextTool.addError(List.of(m.getValue()));
                        } else {
                            executionContextTool.addMessage(List.of(m.getValue()));
                        }
                    });
                }
            }
        }
    }

    private Object getValue(IMessage message) {
        if (Get.ValueTypes.BYTES_SIZE.equals(valueType) && ValueType.BYTES.equals(message.getType()))
            return ((byte[]) message.getValue()).length;
        return message.getValue();
    }

    private Object getFromDefault(int id, List<IMessage> messagesInput) {
        Object obj = defaultValues.get(id);
        Object result = null;
        switch (defaultValueType) {
            case STATIC:
                result = obj;
                break;
            case DYNAMIC: {
                id = (Integer) obj;
                if (0 <= id && id < messagesInput.size()) {
                    result = getValue(messagesInput.get(id));
                } else if (id < 0 && (messagesInput.size() + id) >= 0) {
                    result = getValue(messagesInput.get(messagesInput.size() + id));
                }
                break;
            }
        }
        return result;
    }

    private boolean filter(IMessage m) {
        switch (valueType) {
            case ALL:
                return true;
            case STRING:
                return ValueType.STRING.equals(m.getType());
            case BYTE:
                return ValueType.BYTE.equals(m.getType());
            case SHORT:
                return ValueType.SHORT.equals(m.getType());
            case INTEGER:
                return ValueType.INTEGER.equals(m.getType());
            case LONG:
                return ValueType.LONG.equals(m.getType());
            case BIG_INTEGER:
                return ValueType.BIG_INTEGER.equals(m.getType());
            case FLOAT:
                return ValueType.FLOAT.equals(m.getType());
            case DOUBLE:
                return ValueType.DOUBLE.equals(m.getType());
            case BIG_DECIMAL:
                return ValueType.BIG_DECIMAL.equals(m.getType());
            case BYTES:
            case BYTES_SIZE:
                return ValueType.BYTES.equals(m.getType());
            case NUMBER:
                return ValueType.BYTE.equals(m.getType()) || ValueType.SHORT.equals(m.getType()) || ValueType.INTEGER.equals(m.getType()) || ValueType.LONG.equals(m.getType()) || ValueType.FLOAT.equals(m.getType()) || ValueType.DOUBLE.equals(m.getType()) || ValueType.BIG_INTEGER.equals(m.getType()) || ValueType.BIG_DECIMAL.equals(m.getType());
        }
        return false;
    }

    private Object fromString(String value, Pattern base64) {
        Object result;
        if (NumberUtils.isNumber(value)) {
            if (!StringUtils.contains(value, ".")) {
                result = NumberUtils.toLong(value);
            } else {
                result = NumberUtils.toDouble(value);
            }

        } else {
            if (base64.matcher(value).find()) {
                try {
                    result = Base64.getDecoder().decode(value);
                } catch (Exception e) {
                    // not change
                    result = value;
                }
            } else {
                result = value;
            }
        }
        return result;
    }

    @Override
    public void stop(ConfigurationTool configurationTool) throws ModuleException {
        elementIds = null;
        processingType = null;
        type = null;
        valueType = null;
        defaultValues = null;
        base64 = null;
    }

}
