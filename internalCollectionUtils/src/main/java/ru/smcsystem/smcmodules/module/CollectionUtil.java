package ru.smcsystem.smcmodules.module;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import ru.smcsystem.api.dto.*;
import ru.smcsystem.api.enumeration.ActionType;
import ru.smcsystem.api.enumeration.CommandType;
import ru.smcsystem.api.enumeration.ObjectType;
import ru.smcsystem.api.enumeration.ValueType;
import ru.smcsystem.api.exceptions.ModuleException;
import ru.smcsystem.api.module.Module;
import ru.smcsystem.api.tools.ConfigurationTool;
import ru.smcsystem.api.tools.execution.ExecutionContextTool;
import ru.smcsystem.smc.utils.ModuleUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CollectionUtil implements Module {
    private static final Pattern base64 = Pattern.compile("^([A-Za-z0-9+\\/]{4})*([A-Za-z0-9+\\/]{4}|[A-Za-z0-9+\\/]{3}=|[A-Za-z0-9+\\/]{2}==)$");
    private Type type;
    private List<Long> params;
    private List<String> strParams;
    private List<Object> values;
    private int countValuesInObject;
    private List<Pattern> searchs;
    // private String start;
    // private String end;
    private String value;
    private Type typeBase;

    @Override
    public void start(ConfigurationTool configurationTool) throws ModuleException {
        type = Type.valueOf((String) configurationTool.getSetting("type").orElseThrow(() -> new ModuleException("type setting")).getValue());
        typeBase = type;
        value = (String) configurationTool.getSetting("value").orElseThrow(() -> new ModuleException("value setting")).getValue();
        // start = (String) configurationTool.getSetting("start").orElseThrow(() -> new ModuleException("start setting")).getValue();
        // end = (String) configurationTool.getSetting("end").orElseThrow(() -> new ModuleException("end setting")).getValue();
        updateSettings();
    }

    private void updateSettings() {
        params = null;
        strParams = null;
        countValuesInObject = 1;
        searchs = null;
        values = null;
        if (Type.MAP_GET_VALUE.equals(type) || Type.MAP_GET_VALUE_EXT.equals(type) ||
                Type.MAP_GET_VALUE_OBJECT_LIST.equals(type) || Type.MAP_GET_VALUE_OBJECT_LIST_PATH.equals(type) ||
                Type.MAP_GET_VALUE_OBJECT_LIST_SIMPLE.equals(type) || Type.MAP_GET_VALUE_OBJECT_LIST_PATH_SIMPLE.equals(type)) {
            strParams = new ArrayList<>();
            values = new ArrayList<>();
            Arrays.stream(value.split("::"))
                    .map(s -> s.split(";;"))
                    .forEach(arr -> {
                        if (arr.length == 1) {
                            strParams.add(arr[0]);
                            values.add(null);
                        } else if (arr.length > 2) {
                            strParams.add(String.join(";;", arr));
                            values.add(null);
                        } else {
                            strParams.add(arr[0]);
                            values.add(fromString(arr[1]));
                        }
                    });
        } else if (Type.MAP_GET_VALUE_EXT_REGEXP.equals(type)) {
            searchs = new ArrayList<>();
            values = new ArrayList<>();
            Arrays.stream(value.split("::"))
                    .map(s -> s.split(";;"))
                    .forEach(arr -> {
                        if (arr.length == 1) {
                            searchs.add(Pattern.compile(arr[0]));
                            values.add(null);
                        } else if (arr.length > 2) {
                            searchs.add(Pattern.compile(String.join(";;", arr)));
                            values.add(null);
                        } else {
                            searchs.add(Pattern.compile(arr[0]));
                            values.add(fromString(arr[1]));
                        }
                    });
        } else if (Type.SUBTRACT.equals(type) && !value.isBlank()) {
            try {
                countValuesInObject = Integer.valueOf(value.trim());
            } catch (Exception e) {
            }
        } else if (Type.INDEX_OF_VALUE_REGEXP.equals(type) && !value.isBlank()) {
            searchs = List.of(Pattern.compile(value));
        } else if (Type.OBJECT_LIST_SET_FIELD.equals(type) && !value.isBlank()) {
            strParams = List.of(value);
        } else if (Type.TRANSFORM_OBJECT_FIELD.equals(type) && !value.isBlank()) {
            strParams = Arrays.stream(value.split("::"))
                    .collect(Collectors.toList());
        } else {
            if (!value.isBlank()) {
                params = Arrays.stream(value.split("::"))
                        .filter(NumberUtils::isCreatable)
                        .map(s -> NumberUtils.createNumber(s).longValue())
                        .collect(Collectors.toList());
            }
            if (Type.SUB_LIST.equals(type) && (CollectionUtils.isEmpty(params) || params.size() < 2)) {
                throw new ModuleException("wrong value");
            } else if (Type.SUB_LIST_EXT.equals(type) && (CollectionUtils.isEmpty(params) || params.size() < 1)) {
                throw new ModuleException("wrong value");
            } else if (Type.LIST_ARRAY_GET.equals(type) && (CollectionUtils.isEmpty(params) || params.size() < 1)) {
                throw new ModuleException("wrong value");
            } else if (Type.TRANSFORM.equals(type) && (CollectionUtils.isEmpty(params) || params.size() < 1)) {
                throw new ModuleException("wrong value");
            }
        }
    }

    @Override
    public void update(ConfigurationTool configurationTool) throws ModuleException {
        stop(configurationTool);
        start(configurationTool);
    }

    @Override
    public void process(ConfigurationTool configurationTool, ExecutionContextTool executionContextTool) throws ModuleException {
        boolean isDefault = Objects.equals(executionContextTool.getType(), "default");
        ModuleUtils.executor(configurationTool, executionContextTool, -1, null, (ignoredId, ignored) -> {
            Type type = isDefault ? this.typeBase : Type.valueOf(executionContextTool.getType().toUpperCase());
            if (!isDefault && type != this.type) {
                this.type = type;
                updateSettings();
            }
            switch (type) {
                case SIZE:
                    executionContextTool.addMessage(Stream.iterate(0, n -> n + 1)
                            .limit(executionContextTool.countSource())
                            .flatMap(n -> executionContextTool.getMessages(n).stream())
                            .mapToLong(a -> a.getMessages().size())
                            .sum());
                    break;
                case INDEX_OF: {
                    if (executionContextTool.countSource() < 2) {
                        executionContextTool.addError("need 2 source");
                        break;
                    }
                    List<IMessage> collect = executionContextTool.getMessages(1).stream()
                            .flatMap(a -> a.getMessages().stream())
                            .collect(Collectors.toList());
                    executionContextTool.getMessages(0).stream()
                            .flatMap(a -> a.getMessages().stream())
                            .forEach(searchMessage -> {
                                for (int i = 0; i < collect.size(); i++) {
                                    IMessage message = collect.get(i);
                                    if ((Objects.equals(message.getType(), searchMessage.getType()) && Objects.equals(message.getValue(), searchMessage.getValue()))
                                            || (ModuleUtils.isNumber(searchMessage) == ModuleUtils.isNumber(message) && ((Number) searchMessage.getValue()).longValue() == ((Number) message.getValue()).longValue() && ((Number) searchMessage.getValue()).doubleValue() == ((Number) message.getValue()).doubleValue())) {
                                        executionContextTool.addMessage(i);
                                        return;
                                    }
                                }
                                executionContextTool.addMessage(-1);
                            });
                    break;
                }
                case SUB_LIST: {
                    List<IMessage> inputs = Stream.iterate(0, n -> n + 1)
                            .limit(executionContextTool.countSource())
                            .flatMap(n -> executionContextTool.getMessages(n).stream())
                            .flatMap(a -> a.getMessages().stream())
                            // .map(IValue::getValue)
                            .collect(Collectors.toList());
                    if (inputs.isEmpty())
                        break;
                    int start = params.get(0).intValue();
                    int end = params.get(1).intValue();
                    executionContextTool.addMessage(inputs.subList(start, end).stream().map(IValue::getValue).collect(Collectors.toList()));
                    break;
                }
                case SUB_LIST_EXT: {
                    List<IMessage> inputs = Stream.iterate(0, n -> n + 1)
                            .limit(executionContextTool.countSource())
                            .flatMap(n -> executionContextTool.getMessages(n).stream())
                            .flatMap(a -> a.getMessages().stream())
                            // .map(IValue::getValue)
                            .collect(Collectors.toList());
                    if (inputs.isEmpty())
                        break;
                    int numberValueId = params.get(0).intValue();
                    int currentNumberValueId = -1;
                    int i = 0;
                    for (; i < inputs.size(); i++) {
                        if (ModuleUtils.isNumber(inputs.get(i))) {
                            currentNumberValueId++;
                            if (currentNumberValueId == numberValueId)
                                break;
                        }
                    }
                    if (currentNumberValueId == numberValueId) {
                        IMessage message = inputs.get(i);
                        int size = ((Number) message.getValue()).intValue();
                        executionContextTool.addMessage(inputs.subList(i + 1, i + 1 + size).stream().map(IValue::getValue).collect(Collectors.toList()));
                    }
                    break;
                }
                case LIST_ARRAY_GET: {
                    LinkedList<IMessage> inputs = Stream.iterate(0, n -> n + 1)
                            .limit(executionContextTool.countSource())
                            .flatMap(n -> executionContextTool.getMessages(n).stream())
                            .flatMap(a -> a.getMessages().stream())
                            // .map(IValue::getValue)
                            .collect(Collectors.toCollection(LinkedList::new));
                    if (inputs.isEmpty())
                        break;
                    ObjectArray arrays = ModuleUtils.deserializeToObject(inputs);
                    if (!ModuleUtils.isArrayContainArrays(arrays)) {
                        executionContextTool.addError("need array of arrays");
                        break;
                    }
                    int numberValueId = params.get(0).intValue();
                    if (arrays.size() < numberValueId) {
                        executionContextTool.addError("need a large array");
                        break;
                    }
                    ObjectArray array = (ObjectArray) arrays.get(numberValueId);
                    if (params.size() > 1) {
                        int start = params.get(1).intValue();
                        int end = params.size() > 2 ? Math.min(params.get(2).intValue(), params.size()) : params.size();
                        List<Object> values = new LinkedList<>();
                        for (int i = start; i < end; i++)
                            values.add(array.get(i));
                        array = new ObjectArray(values, array.getType());
                    }
                    if (ModuleUtils.isArrayContainArrays(array) || ModuleUtils.isArrayContainObjectElements(array)) {
                        executionContextTool.addMessage(array);
                    } else {
                        executionContextTool.addMessage(ModuleUtils.serializeFromObject(array));
                    }
                    break;
                }
                case SUBTRACT: {
                    if (executionContextTool.countSource() < 2) {
                        executionContextTool.addError("need 2 source");
                        break;
                    }
                    List<Object> inputs1 = executionContextTool.getMessages(0).stream()
                            .flatMap(a -> a.getMessages().stream())
                            .map(IValue::getValue)
                            .collect(Collectors.toList());
                    List<Object> inputs2 = executionContextTool.getMessages(1).stream()
                            .flatMap(a -> a.getMessages().stream())
                            .map(IValue::getValue)
                            .collect(Collectors.toList());
                    if (countValuesInObject > 1) {
                        List<List<Object>> inputs1Obj = new ArrayList<>(inputs1.size() / countValuesInObject + 1);
                        for (int i = 0; i <= inputs1.size() - countValuesInObject; i = i + countValuesInObject) {
                            List<Object> lst = new ArrayList<>(countValuesInObject);
                            for (int j = i; j < i + countValuesInObject; j++)
                                lst.add(inputs1.get(j));
                            inputs1Obj.add(lst);
                        }
                        List<List<Object>> inputs2Obj = new ArrayList<>(inputs2.size() / countValuesInObject + 1);
                        for (int i = 0; i <= inputs2.size() - countValuesInObject; i = i + countValuesInObject) {
                            List<Object> lst = new ArrayList<>(countValuesInObject);
                            for (int j = i; j < i + countValuesInObject; j++)
                                lst.add(inputs2.get(j));
                            inputs2Obj.add(lst);
                        }
                        for (Iterator<List<Object>> it = inputs2Obj.iterator(); it.hasNext(); ) {
                            List<Object> objects2 = it.next();
                            for (int i = 0; i < inputs1Obj.size(); i++) {
                                if (CollectionUtils.isEqualCollection(inputs1Obj.get(i), objects2)) {
                                    inputs1Obj.remove(i);
                                    break;
                                }
                            }
                        }
                        executionContextTool.addMessage(
                                inputs1Obj.stream()
                                        .flatMap(Collection::stream).collect(Collectors.toList()));
                    } else {
                        executionContextTool.addMessage(new ArrayList<>(CollectionUtils.subtract(inputs1, inputs2)));
                    }
                    break;
                }
                case MAP_GET_VALUE: {
                    if (executionContextTool.countSource() < 1) {
                        executionContextTool.addError("need 1 source");
                        break;
                    }
                    List<Object> inputs = Stream.iterate(0, n -> n + 1)
                            .limit(executionContextTool.countSource())
                            .flatMap(n -> executionContextTool.getMessages(n).stream())
                            .flatMap(a -> a.getMessages().stream())
                            .map(IValue::getValue)
                            .collect(Collectors.toList());
                    List<Object> result = new LinkedList<>();
                    for (int j = 0; j < strParams.size(); j++) {
                        String param = strParams.get(j);
                        Object value = values.get(j);
                        for (int i = 0; i < inputs.size() - 1; i++) {
                            Object o = inputs.get(i);
                            if (param.equalsIgnoreCase(o.toString()))
                                value = inputs.get(++i);
                        }
                        if (value != null)
                            result.add(value);
                    }
                    executionContextTool.addMessage(result);
                    break;
                }
                case MAP_GET_VALUE_EXT:
                case MAP_GET_VALUE_EXT_REGEXP: {
                    if (executionContextTool.countSource() < 1) {
                        executionContextTool.addError("need 1 source");
                        break;
                    }
                    List<String[]> inputs = Stream.iterate(0, n -> n + 1)
                            .limit(executionContextTool.countSource())
                            .flatMap(n -> executionContextTool.getMessages(n).stream())
                            .flatMap(a -> a.getMessages().stream())
                            // .map(IValue::getValue)
                            .filter(ModuleUtils::isString)
                            .map(ModuleUtils::getString)
                            .filter(Objects::nonNull)
                            .map(s -> s.split("=", 2))
                            .filter(a -> a.length == 2)
                            .collect(Collectors.toList());
                    List<Object> result = new LinkedList<>();
                    if (Type.MAP_GET_VALUE_EXT.equals(type)) {
                        for (int j = 0; j < strParams.size(); j++) {
                            String param = strParams.get(j);
                            Object value = values.get(j);
                            List<Object> values2 = new ArrayList<>();
                            for (int i = 0; i < inputs.size(); i++) {
                                String[] e = inputs.get(i);
                                if (param.equalsIgnoreCase(e[0]))
                                    values2.add(fromString(e[1]));
                            }
                            if (!values2.isEmpty()) {
                                result.addAll(values2);
                            } else if (value != null) {
                                result.add(value);
                            }
                        }
                    } else if (Type.MAP_GET_VALUE_EXT_REGEXP.equals(type)) {
                        for (int j = 0; j < searchs.size(); j++) {
                            Pattern pattern = searchs.get(j);
                            Object value = values.get(j);
                            List<Object> values2 = new ArrayList<>();
                            for (int i = 0; i < inputs.size(); i++) {
                                String[] e = inputs.get(i);
                                if (pattern.matcher(e[0]).matches()) {
                                    values2.add(fromString(e[0]));
                                    values2.add(fromString(e[1]));
                                }
                            }
                            if (!values2.isEmpty()) {
                                result.addAll(values2);
                            } else if (value != null) {
                                result.add(pattern.toString());
                                result.add(value);
                            }
                        }
                    }
                    executionContextTool.addMessage(result);
                    break;
                }
                case MAP_GET_VALUE_OBJECT_LIST:
                case MAP_GET_VALUE_OBJECT_LIST_PATH:
                case MAP_GET_VALUE_OBJECT_LIST_SIMPLE:
                case MAP_GET_VALUE_OBJECT_LIST_PATH_SIMPLE: {
                    if (executionContextTool.countSource() < 1) {
                        executionContextTool.addError("need 1 source");
                        break;
                    }
                    LinkedList<IMessage> inputs = Stream.iterate(0, n -> n + 1)
                            .limit(executionContextTool.countSource())
                            .flatMap(n -> executionContextTool.getMessages(n).stream())
                            .flatMap(a -> a.getMessages().stream())
                            // .map(IValue::getValue)
                            .collect(Collectors.toCollection(LinkedList::new));
                    if (strParams.isEmpty())
                        break;
                    // int paramId = 0;
                    // String param = strParams.get(paramId);
                    ObjectArray mainList = ModuleUtils.deserializeToObject(inputs);
                    if (mainList.getType() != ObjectType.OBJECT_ELEMENT) {
                        executionContextTool.addMessage(ModuleUtils.serializeFromObject(new ObjectArray(0, mainList.getType())));
                        break;
                    }

                    List<ObjectElement> resultObjects = new LinkedList<>();
                    if (Type.MAP_GET_VALUE_OBJECT_LIST_PATH.equals(type) || Type.MAP_GET_VALUE_OBJECT_LIST_PATH_SIMPLE.equals(type)) {
                        for (int i = 0; i < mainList.size(); i++) {
                            ObjectElement objectElementResult = new ObjectElement();
                            for (int z = 0; z < strParams.size(); z++) {
                                String fullPath = strParams.get(z);
                                String[] path = fullPath.split("\\.");
                                ObjectElement lastElementPrev = null;
                                ObjectElement lastElement = (ObjectElement) mainList.get(i);
                                ObjectField objectField = null;
                                String pathElement = null;
                                for (int j = 0; lastElement != null && j < path.length; j++) {
                                    pathElement = path[j];
                                    lastElementPrev = lastElement;
                                    String pathElementTmp = pathElement;
                                    objectField = lastElementPrev.getFields().stream()
                                            .filter(f -> pathElementTmp.equalsIgnoreCase(f.getName()))
                                            .findAny().orElse(null);
                                    if (j + 1 < path.length) {
                                        lastElement = ModuleUtils.getObjectElement(objectField);
                                    } else {
                                        lastElement = null;
                                    }
                                }
                                if (objectField != null) {
                                    objectElementResult.getFields().add(objectField);
                                } else {
                                    Object value = values.get(z);
                                    if (value != null)
                                        objectElementResult.getFields().add(new ObjectField(pathElement != null ? pathElement : fullPath, ModuleUtils.getObjectType(value), value));
                                }
                            }
                            resultObjects.add(objectElementResult);
                        }
                    } else {
                        for (int i = 0; i < mainList.size(); i++) {
                            ObjectElement objectElement = (ObjectElement) mainList.get(i);
                            ObjectElement objectElementResult = new ObjectElement();
                            for (int j = 0; j < strParams.size(); j++) {
                                String param = strParams.get(j);
                                Object value = values.get(j);
                                objectElement.findField(param).ifPresentOrElse(f -> objectElementResult.getFields().add(f),
                                        () -> {
                                            if (value != null)
                                                objectElementResult.getFields().add(new ObjectField(param, ModuleUtils.getObjectType(value), value));
                                        });
                            }
                            resultObjects.add(objectElementResult);
                        }
                    }
                    ObjectArray result = resultObjects.stream().allMatch(o -> o.getFields().isEmpty()) ?
                            new ObjectArray(0, mainList.getType()) :
                            new ObjectArray((List) resultObjects, mainList.getType());
                    // if (ObjectType.OBJECT_ELEMENT_SIMPLE.equals(result.getType()) && result.size() == 1) {
                    //     ObjectArray resultNew = new ObjectArray(null, ObjectType.VALUE_ANY);
                    //     for (int i = 0; i < result.size(); i++)
                    //         ((ObjectElement) result.get(i)).getFields().forEach(objectField -> resultNew.add(objectField.getValue()));
                    //     result = resultNew.updateType();
                    // } else
                    if (strParams.size() == 1 && ModuleUtils.isArrayContainObjectElements(result)) {
                        ObjectElement objectElement = (ObjectElement) result.get(0);
                        if (objectElement.getFields().size() > 0) {
                            ObjectField objectField = objectElement.getFields().get(0);
                            List<Object> resultObjectsInner = new LinkedList<>();
                            for (int i = 0; i < result.size(); i++) {
                                objectElement = (ObjectElement) result.get(i);
                                if (objectElement.getFields().size() > 0) {
                                    ObjectField objectFieldCurrent = objectElement.getFields().get(0);
                                    if (objectFieldCurrent.getType() == objectField.getType())
                                        resultObjectsInner.add(objectFieldCurrent.getValue());
                                }
                            }
                            result = new ObjectArray(resultObjectsInner, objectField.getType());
                            // if (ObjectType.OBJECT_ARRAY.equals(result.getType()) && result.size() == 1)
                            //     result = (ObjectArray) result.get(0);
                        }
                    }
                    if (type == Type.MAP_GET_VALUE_OBJECT_LIST_SIMPLE || type == Type.MAP_GET_VALUE_OBJECT_LIST_PATH_SIMPLE) {
                        while (result.getType() == ObjectType.OBJECT_ARRAY && result.size() == 1) {
                            result = (ObjectArray) result.get(0);
                        }
                    }
                    if (ModuleUtils.isArrayContainObjectElements(result) || ModuleUtils.isArrayContainArrays(result)) {
                        executionContextTool.addMessage(result);
                    } else {
                        executionContextTool.addMessage(ModuleUtils.serializeFromObject(result));
                    }
                    break;
                }
                case TRANSFORM: {
                    if (executionContextTool.getFlowControlTool().countManagedExecutionContexts() == 0) {
                        executionContextTool.addError("need execution contexts for transform");
                        break;
                    }

                    List<Object> inputs = Stream.iterate(0, n -> n + 1)
                            .limit(executionContextTool.countSource())
                            .flatMap(n -> executionContextTool.getMessages(n).stream())
                            .flatMap(a -> a.getMessages().stream())
                            .map(IValue::getValue)
                            .collect(Collectors.toList());

                    int size = params.get(0).intValue();
                    List<Object> result = new LinkedList<>();
                    for (int i = 0; i + size <= inputs.size(); i = i + size) {
                        List<Object> values = inputs.subList(i, i + size);
                        executionContextTool.getFlowControlTool().executeNow(CommandType.EXECUTE, 0, values);
                        executionContextTool.getFlowControlTool().getMessagesFromExecuted(0).stream()
                                .flatMap(a -> a.getMessages().stream())
                                .forEach(m -> result.add(m.getValue()));
                    }
                    executionContextTool.addMessage(result);
                    break;
                }
                case INDEX_OF_VALUE_REGEXP: {
                    if (executionContextTool.countSource() < 1) {
                        executionContextTool.addError("need 1 source");
                        break;
                    }
                    if (searchs == null)
                        break;
                    List<String> list = Stream.iterate(0, n -> n + 1)
                            .limit(executionContextTool.countSource())
                            .flatMap(n -> executionContextTool.getMessages(n).stream())
                            .flatMap(a -> a.getMessages().stream())
                            .map(IValue::getValue)
                            .map(Object::toString)
                            .collect(Collectors.toList());
                    int index = -1;
                    for (int i = 0; i < list.size(); i++) {
                        if (searchs.get(0).matcher(list.get(i)).find()) {
                            index = i;
                            break;
                        }
                    }
                    if (index > -1)
                        executionContextTool.addMessage(index);
                    break;
                }
                case INDEX_OF_OR_NULL: {
                    if (executionContextTool.countSource() < 2) {
                        executionContextTool.addError("need 2 source");
                        break;
                    }
                    List<IMessage> collect = executionContextTool.getMessages(1).stream()
                            .flatMap(a -> a.getMessages().stream())
                            .collect(Collectors.toList());
                    executionContextTool.getMessages(0).stream()
                            .flatMap(a -> a.getMessages().stream())
                            .forEach(searchMessage -> {
                                for (int i = 0; i < collect.size(); i++) {
                                    IMessage message = collect.get(i);
                                    if ((Objects.equals(message.getType(), searchMessage.getType()) && Objects.equals(message.getValue(), searchMessage.getValue()))
                                            || (ModuleUtils.isNumber(searchMessage) == ModuleUtils.isNumber(message) && ((Number) searchMessage.getValue()).longValue() == ((Number) message.getValue()).longValue() && ((Number) searchMessage.getValue()).doubleValue() == ((Number) message.getValue()).doubleValue())) {
                                        executionContextTool.addMessage(i);
                                        return;
                                    }
                                }
                                // executionContextTool.addMessage(-1);
                            });
                    break;
                }
                case FILTER_OBJECT_LIST: {
                    if (executionContextTool.getFlowControlTool().countManagedExecutionContexts() == 0) {
                        executionContextTool.addError("need execution contexts for transform");
                        break;
                    }
                    if (executionContextTool.countSource() < 1) {
                        executionContextTool.addError("need 1 source");
                        break;
                    }
                    LinkedList<IMessage> inputs = Stream.iterate(0, n -> n + 1)
                            .limit(executionContextTool.countSource())
                            .flatMap(n -> executionContextTool.getMessages(n).stream())
                            .flatMap(a -> a.getMessages().stream())
                            // .map(IValue::getValue)
                            .collect(Collectors.toCollection(LinkedList::new));
                    ObjectArray mainList = ModuleUtils.deserializeToObject(inputs);
                    if (!ModuleUtils.isArrayContainObjectElements(mainList)) {
                        executionContextTool.addError("wrong format. need ObjectElements.");
                        break;
                    }
                    ObjectArray result = new ObjectArray(mainList.size(), ObjectType.OBJECT_ELEMENT);
                    for (int i = 0; i < mainList.size(); i++) {
                        ObjectElement objectElement = (ObjectElement) mainList.get(i);
                        ObjectArray objectArray = new ObjectArray(List.of(objectElement), ObjectType.OBJECT_ELEMENT);
                        executionContextTool.getFlowControlTool().executeNow(CommandType.EXECUTE, 0, List.of(objectArray));
                        executionContextTool.getFlowControlTool().getMessagesFromExecuted(0).stream()
                                .filter(a -> ActionType.EXECUTE.equals(a.getType()))
                                .map(IAction::getMessages)
                                .filter(lst -> !lst.isEmpty())
                                .findAny()
                                .ifPresent(lst -> result.add(objectElement));
                    }
                    if (result.size() > 0)
                        executionContextTool.addMessage(result);
                    break;
                }
                case OBJECT_LIST_SET_FIELD: {
                    if (executionContextTool.countSource() < 1) {
                        executionContextTool.addError("need 1 source");
                        break;
                    }
                    LinkedList<IMessage> inputs = executionContextTool.getMessages(0).stream()
                            .flatMap(a -> a.getMessages().stream())
                            // .map(IValue::getValue)
                            .collect(Collectors.toCollection(LinkedList::new));
                    LinkedList<IMessage> values = executionContextTool.countSource() > 1 ? executionContextTool.getMessages(1).stream()
                            .flatMap(a -> a.getMessages().stream())
                            // .map(IValue::getValue)
                            .collect(Collectors.toCollection(LinkedList::new)) : null;
                    ObjectArray mainList = null;
                    try {
                        mainList = (ObjectArray) ModuleUtils.deserializeToObject(inputs).clone();
                    } catch (Exception e) {
                        executionContextTool.addError(String.format("wrong input %s", e.getMessage()));
                        break;
                    }
                    if (!ModuleUtils.isArrayContainObjectElements(mainList)) {
                        executionContextTool.addError("wrong format. need ObjectElements.");
                        break;
                    }
                    String fieldName = strParams.get(0);
                    Object fieldValue = null;
                    if (values != null) {
                        IMessage iMessage = values.peek();
                        int size = values.size();
                        ObjectArray objectArray = ModuleUtils.deserializeToObject(values);
                        if (values.size() != size) {
                            fieldValue = objectArray.size() == 1 ? objectArray.get(0) : objectArray;
                        } else {
                            fieldValue = iMessage.getValue();
                        }
                    }
                    Object fieldValueTmp = fieldValue;
                    for (int j = 0; j < mainList.size(); j++) {
                        ObjectElement objectElement = (ObjectElement) mainList.get(j);
                        boolean find = false;
                        for (int i = 0; i < objectElement.getFields().size(); i++) {
                            ObjectField objectField = objectElement.getFields().get(i);
                            if (fieldName.equalsIgnoreCase(objectField.getName())) {
                                if (fieldValueTmp != null) {
                                    objectElement.getFields().set(i, new ObjectField(fieldName, ModuleUtils.getObjectType(fieldValueTmp), fieldValueTmp));
                                } else {
                                    objectElement.getFields().remove(i);
                                }
                                find = true;
                                break;
                            }
                        }
                        if (!find && fieldValueTmp != null)
                            objectElement.getFields().add(new ObjectField(fieldName, ModuleUtils.getObjectType(fieldValueTmp), fieldValueTmp));
                    }
                    executionContextTool.addMessage(mainList);
                    break;
                }
                case OBJECT_LIST_JOIN: {
                    if (executionContextTool.countSource() < 1) {
                        executionContextTool.addError("need 1 source");
                        break;
                    }
                    LinkedList<IMessage> inputs = Stream.iterate(0, n -> n + 1)
                            .limit(executionContextTool.countSource())
                            .flatMap(n -> executionContextTool.getMessages(n).stream())
                            .flatMap(a -> a.getMessages().stream())
                            // .map(IValue::getValue)
                            .collect(Collectors.toCollection(LinkedList::new));
                    ObjectArray result = new ObjectArray();
                    int lastCount;
                    do {
                        lastCount = inputs.size();
                        ObjectArray mainList = ModuleUtils.deserializeToObject(inputs);
                        if (!ModuleUtils.isArrayContainObjectElements(mainList)) {
                            executionContextTool.addError("wrong format. need ObjectElements.");
                            continue;
                        }
                        for (int i = 0; i < mainList.size(); i++)
                            result.add((ObjectElement) mainList.get(i));
                    } while (!inputs.isEmpty() && lastCount != inputs.size());
                    executionContextTool.addMessage(result);
                    break;
                }
                case OBJECT_LIST_JOIN_ARRAYS: {
                    if (executionContextTool.countSource() < 1) {
                        executionContextTool.addError("need 1 source");
                        break;
                    }
                    LinkedList<IMessage> inputs = Stream.iterate(0, n -> n + 1)
                            .limit(executionContextTool.countSource())
                            .flatMap(n -> executionContextTool.getMessages(n).stream())
                            .flatMap(a -> a.getMessages().stream())
                            // .map(IValue::getValue)
                            .collect(Collectors.toCollection(LinkedList::new));
                    ObjectArray result = new ObjectArray(0, ObjectType.OBJECT_ARRAY);
                    int lastCount;
                    do {
                        lastCount = inputs.size();
                        ObjectArray mainList = ModuleUtils.deserializeToObject(inputs);
                        if (!ModuleUtils.isArrayContainArrays(mainList)) {
                            executionContextTool.addError("wrong format. need Arrays.");
                            executionContextTool.addError(mainList.getType().name());
                            executionContextTool.addError(mainList.size());
                            executionContextTool.addError(lastCount);
                            continue;
                        }
                        for (int i = 0; i < mainList.size(); i++)
                            result.add((ObjectArray) mainList.get(i));
                    } while (!inputs.isEmpty() && lastCount != inputs.size());
                    executionContextTool.addMessage(result);
                    break;
                }
                case OBJECT_FIELDS_JOIN: {
                    if (executionContextTool.countSource() < 1) {
                        executionContextTool.addError("need 1 source");
                        break;
                    }
                    List<ObjectElement> objectElements = Stream.iterate(0, n -> n + 1)
                            .limit(executionContextTool.countSource())
                            .map(i -> executionContextTool.getMessages(i).stream()
                                    .filter(ModuleUtils::hasData)
                                    .flatMap(a -> a.getMessages().stream())
                                    .collect(Collectors.toCollection(LinkedList::new)))
                            .flatMap(l -> {
                                int size;
                                List<ObjectArray> arrays = new ArrayList<>();
                                do {
                                    size = l.size();
                                    ObjectArray objectArray = ModuleUtils.deserializeToObject(l);
                                    if (ModuleUtils.isArrayContainObjectElements(objectArray))
                                        arrays.add(objectArray);
                                } while (!l.isEmpty() && size != l.size());
                                return arrays.stream();
                            })
                            .map(a -> (ObjectElement) a.get(0))
                            .collect(Collectors.toList());
                    if (objectElements.size() < 2) {
                        executionContextTool.addError("need 2 objects or more");
                        break;
                    }
                    Map<String, ObjectField> fields = new HashMap<>();
                    objectElements.forEach(o -> o.getFields().forEach(f -> fields.computeIfAbsent(f.getName(), n -> f)));
                    executionContextTool.addMessage(new ObjectArray(List.of(new ObjectElement(new ArrayList<>(fields.values()))), ObjectType.OBJECT_ELEMENT));
                    break;
                }
                case TRANSFORM_OBJECT_FIELD: {
                    if (executionContextTool.getFlowControlTool().countManagedExecutionContexts() != strParams.size()) {
                        executionContextTool.addError("need " + strParams.size() + " execution contexts for transform");
                        break;
                    }

                    List<ObjectArray> inputs = Stream.iterate(0, n -> n + 1)
                            .limit(executionContextTool.countSource())
                            .flatMap(n -> executionContextTool.getMessages(n).stream())
                            .map(a -> new LinkedList<>(a.getMessages()))
                            .flatMap(l -> {
                                List<ObjectArray> lst = new LinkedList<>();
                                int size;
                                do {
                                    size = l.size();
                                    ObjectArray objectArray = ModuleUtils.deserializeToObject(l);
                                    lst.add(objectArray);
                                } while (!l.isEmpty() && size != l.size());
                                return lst.stream();
                            })
                            .collect(Collectors.toList());

                    List<Object> result = new LinkedList<>();
                    for (ObjectArray objectArray : inputs) {
                        ObjectArray objectArrayTransformed;
                        try {
                            objectArrayTransformed = (ObjectArray) objectArray.clone();
                            result.add(objectArrayTransformed);
                        } catch (Exception e) {
                            result.add(objectArray);
                            continue;
                        }
                        List<List<ObjectField>> fields = ModuleUtils.findFields(objectArrayTransformed, strParams);
                        for (int i = 0; i < fields.size(); i++) {
                            List<ObjectField> fieldList = fields.get(i);
                            int id = i;
                            fieldList.forEach(f -> {
                                executionContextTool.getFlowControlTool().executeNow(CommandType.EXECUTE, id, List.of(f.getValue()));
                                executionContextTool.getFlowControlTool().getMessagesFromExecuted(id).stream()
                                        .flatMap(a -> a.getMessages().stream())
                                        .forEach(f::setValue);
                            });
                        }
                    }
                    executionContextTool.addMessage(result);
                    break;
                }
                case OBJECT_FIELD_VALUE:
                case OBJECT_FIELD_VALUE_AUTO_CONVERT: {
                    if (executionContextTool.countSource() < 2) {
                        executionContextTool.addError("need 2 sources");
                        break;
                    }
                    ObjectArray objectArray = ModuleUtils.getLastActionWithDataList(executionContextTool.getMessages(0))
                            .map(ModuleUtils::deserializeToObject)
                            .filter(ModuleUtils::isArrayContainObjectElements)
                            .orElse(null);
                    List<Object> values = ModuleUtils.getLastActionWithDataList(executionContextTool.getMessages(1))
                            .stream()
                            .flatMap(l -> {
                                List<ObjectElement> elements;
                                if (ModuleUtils.isObjectArray(l.peek())) {
                                    ObjectArray a = ModuleUtils.getObjectArray(l.peek());
                                    if (!ModuleUtils.isArrayContainObjectElements(a))
                                        return Stream.empty();
                                    elements = new ArrayList<>(a.size() + 1);
                                    for (int i = 0; i < a.size(); i++)
                                        elements.add((ObjectElement) a.get(i));
                                    if (elements.stream().anyMatch(o -> o.findFieldIgnoreCase("position").isPresent()))
                                        elements.sort(Comparator.comparing(o -> o.findFieldIgnoreCase("position").map(ModuleUtils::getNumber).map(Number::intValue).orElse(-1)));
                                } else {
                                    elements = l.stream()
                                            .filter(ModuleUtils::isString)
                                            .map(ModuleUtils::getString)
                                            .filter(s -> !s.isBlank())
                                            .map(s -> {
                                                String path = s;
                                                String defaultValueStr = null;
                                                if (path.contains(";;")) {
                                                    String[] split = path.split(";;", 2);
                                                    path = split[0];
                                                    defaultValueStr = split[1];
                                                }
                                                return new ObjectElement(
                                                        new ObjectField("path", path),
                                                        new ObjectField("default", defaultValueStr)
                                                );
                                            })
                                            .collect(Collectors.toList());
                                }
                                return elements.stream();
                            })
                            .flatMap(e -> {
                                String path = e.findFieldIgnoreCase("path").map(ModuleUtils::toString).orElse(null);
                                if (path == null)
                                    return Stream.empty();
                                ValueType vt = e.findFieldIgnoreCase("type").map(ModuleUtils::toString).map(ValueType::valueOf).orElse(null);
                                List<Object> defaultValue = e.findFieldIgnoreCase("default")
                                        .map(f -> {
                                            if (vt != null)
                                                return convertValueTo(f, vt);
                                            return ModuleUtils.isString(f) ? fromString(ModuleUtils.getString(f)) : f.getValue();
                                        })
                                        .map(List::of)
                                        .orElse(List.of());
                                List<Object> lst = ModuleUtils.findFields(objectArray, List.of(path)).stream()
                                        .flatMap(Collection::stream)
                                        .map(f -> {
                                            if (f.getType() == ObjectType.OBJECT_ELEMENT)
                                                return new ObjectArray((ObjectElement) f.getValue());
                                            if (vt == null && type == Type.OBJECT_FIELD_VALUE_AUTO_CONVERT && ModuleUtils.isString(f))
                                                return fromString(ModuleUtils.getString(f));
                                            if (vt == null) {
                                                return f.getValue();
                                            } else {
                                                return convertValueTo(f, vt);
                                            }
                                        })
                                        .collect(Collectors.toList());
                                return (!lst.isEmpty() ? lst : defaultValue).stream();
                            })
                            .collect(Collectors.toList());
                    if (!values.isEmpty())
                        executionContextTool.addMessage(values);
                    break;
                }
                case LIST_OBJECT_GET: {
                    ObjectArray arrays = ModuleUtils.getFirstActionWithDataList(executionContextTool.getMessages(0))
                            .map(ModuleUtils::deserializeToObject).orElse(null);
                    if (!ModuleUtils.isArrayContainObjectElements(arrays) && !ModuleUtils.isArrayContainArrays(arrays)) {
                        executionContextTool.addError("need array of objects");
                        break;
                    }
                    int numberValueId = ModuleUtils.getFirstActionWithDataList(executionContextTool.getMessages(1))
                            .map(l -> ModuleUtils.toNumber(l.poll()))
                            .map(Number::intValue)
                            .orElse(0);
                    if (arrays.size() < numberValueId) {
                        executionContextTool.addError("need a large array");
                        break;
                    }
                    if (ModuleUtils.isArrayContainObjectElements(arrays)) {
                        executionContextTool.addMessage(new ObjectArray((ObjectElement) arrays.get(numberValueId)));
                    } else {
                        executionContextTool.addMessage((ObjectArray) arrays.get(numberValueId));
                    }
                    break;
                }
            }
        });
    }

    private Object fromString(String value) {
        if (value == null)
            return null;
        Object result;
        if (NumberUtils.isCreatable(value)) {
            result = NumberUtils.createNumber(value);
        } else {
            if (StringUtils.length(value) >= 2 && !value.isBlank() && (value.endsWith("=") || value.length() > 50) && base64.matcher(value).find()) {
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

    private Object convertValueTo(ObjectField f, ValueType vt) {
        ObjectField field = new ObjectField("f", ModuleUtils.convertTo(vt), null);
        if (ModuleUtils.isNumber(field)) {
            Number n = ModuleUtils.toNumber(f);
            switch (vt) {
                case BYTE:
                    return n.byteValue();
                case SHORT:
                    return n.shortValue();
                case INTEGER:
                    return n.intValue();
                case LONG:
                    return n.longValue();
                case BIG_INTEGER:
                    return BigInteger.valueOf(n.longValue());
                case FLOAT:
                    return n.floatValue();
                case DOUBLE:
                    return n.doubleValue();
                case BIG_DECIMAL:
                    return BigDecimal.valueOf(n.doubleValue());
            }
        } else if (ModuleUtils.isBytes(field)) {
            return ModuleUtils.isBytes(f) ? ModuleUtils.getBytes(f) : ModuleUtils.toString(f).getBytes();
        } else if (ModuleUtils.isString(field)) {
            return ModuleUtils.toString(f);
        } else if (ModuleUtils.isBoolean(field)) {
            return ModuleUtils.toBoolean(f);
        } else if (ModuleUtils.isObjectArray(field)) {
            return ModuleUtils.toObjectArray(f);
        }
        return null;
    }

    @Override
    public void stop(ConfigurationTool configurationTool) throws ModuleException {
        type = null;
        params = null;
        strParams = null;
        countValuesInObject = 1;
        searchs = null;
    }

    enum Type {
        SIZE,
        INDEX_OF,
        SUB_LIST,
        SUB_LIST_EXT,
        LIST_ARRAY_GET,
        SUBTRACT,
        MAP_GET_VALUE,
        MAP_GET_VALUE_EXT,
        MAP_GET_VALUE_EXT_REGEXP,
        MAP_GET_VALUE_OBJECT_LIST,
        MAP_GET_VALUE_OBJECT_LIST_PATH,
        MAP_GET_VALUE_OBJECT_LIST_SIMPLE,
        MAP_GET_VALUE_OBJECT_LIST_PATH_SIMPLE,
        TRANSFORM,
        INDEX_OF_VALUE_REGEXP,
        INDEX_OF_OR_NULL,
        FILTER_OBJECT_LIST,
        OBJECT_LIST_SET_FIELD,
        OBJECT_LIST_JOIN,
        OBJECT_LIST_JOIN_ARRAYS,
        OBJECT_FIELDS_JOIN,
        TRANSFORM_OBJECT_FIELD,
        OBJECT_FIELD_VALUE,
        OBJECT_FIELD_VALUE_AUTO_CONVERT,
        LIST_OBJECT_GET,
    }

}
