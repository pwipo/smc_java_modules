package ru.smcsystem.modules.internalValueTypeConverter;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import ru.smcsystem.api.dto.*;
import ru.smcsystem.api.enumeration.ObjectType;
import ru.smcsystem.api.enumeration.ValueType;
import ru.smcsystem.api.exceptions.ModuleException;
import ru.smcsystem.api.module.Module;
import ru.smcsystem.api.tools.ConfigurationTool;
import ru.smcsystem.api.tools.execution.ExecutionContextTool;
import ru.smcsystem.smc.utils.ModuleUtils;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ValueTypeConverter implements Module {
    private static final Pattern base64 = Pattern.compile("/^([A-Za-z0-9+\\/]{4})*([A-Za-z0-9+\\/]{4}|[A-Za-z0-9+\\/]{3}=|[A-Za-z0-9+\\/]{2}==)$/");
    private Type type;
    // private String charsetName;
    private String param;
    private Long lParam;
    private List<String> strings;

    // private String start;
    // private String end;
    private final static String OBJECT_FILED_NAME_NAME = "name";
    private final static String OBJECT_FILED_NAME_VALUE = "value";
    private final static String OBJECT_FILED_VALUE_VALUE_EMPTY = " ";
    private final static String OBJECT_FILED_NAME_CHILD_NODES = "childNodes";

    private enum Type {
        bytesToNumbers,
        numbersToBytes,
        stringToLong,
        stringToDouble,
        numberToString,
        bytesToString,
        stringToBytes,
        toJSONArrayString,
        fromJSONArrayString,
        auto,
        fromCsvToValues,
        fromValuesToCsv,
        toJSONObjectString,
        toLong,
        fromXml,
        toXml,
        fromJSONObjectString,
        toString,
        base64ToBytes,
        bytesToBase64,
        listToObjectArray,
        objectArrayToList,
        stringToNumberOrOrigin,
        objectArrayToMessages,
        messagesToObjectArray,
        objectArrayValuesToList,
        fromJSONArrayStringNew,
        fromJSONObjectStringNew,
        fromXmlSimple,
        toXmlSimple,
        autoNew,
        toBoolean,
        fromCsvToValuesNew
    }

    private Charset charset;
    // private LinkedHashMap<String, ObjectArray> objectArrayCache;

    @Override
    public void start(ConfigurationTool configurationTool) throws ModuleException {
        Integer typeSetting = (Integer) configurationTool.getSetting("type").orElseThrow(() -> new ModuleException("type setting")).getValue();
        type = Type.values()[typeSetting - 1];
        // charsetName = (String) configurationTool.getSetting("charsetName").orElseThrow(() -> new ModuleException("charsetName setting")).getValue();
        param = (String) configurationTool.getSetting("param").orElseThrow(() -> new ModuleException("param setting")).getValue();
        // start = null;
        // end = null;
        lParam = null;
        // objectArrayCache = null;
        updateSettings();
    }

    private void updateSettings() {
        if (Type.fromValuesToCsv.equals(type)) {
            String[] split = param.split("::");
            if (split.length < 2)
                throw new ModuleException("wrong param value format");
            lParam = NumberUtils.toLong(split[0].trim());
            param = split[1];
        } else if (Type.toJSONArrayString.equals(type) || Type.toJSONObjectString.equals(type)) {
            param = null;
        } else if (Type.fromJSONArrayString.equals(type) || Type.fromJSONObjectString.equals(type)) {
            param = null;
        } else if (Type.listToObjectArray.equals(type) || Type.objectArrayToList.equals(type) || Type.objectArrayValuesToList.equals(type)) {
            String[] split = param.split("::");
            lParam = NumberUtils.toLong(split[0].trim());
            strings = split.length - 1 == lParam ? Arrays.asList(Arrays.copyOfRange(split, 1, split.length)) : null;
        } else if (Type.stringToBytes.equals(type) || Type.bytesToString.equals(type)) {
            charset = StringUtils.isNotBlank(param) ? Charset.forName(param) : Charset.defaultCharset();
        }
    }

    @Override
    public void update(ConfigurationTool configurationTool) throws ModuleException {
        stop(configurationTool);
        start(configurationTool);
    }

    @Override
    public void process(ConfigurationTool configurationTool, ExecutionContextTool executionContextTool) throws ModuleException {
        if (Objects.equals(executionContextTool.getType(), "default")) {
            for (int i = 0; i < executionContextTool.countSource(); i++)
                process(executionContextTool, configurationTool, executionContextTool.getMessages(i), type);
        } else {
            Type type = Type.valueOf(executionContextTool.getType());
            if (type != this.type)
                updateSettings();
            List<IAction> actions = executionContextTool.countSource() > 0 ? executionContextTool.getMessages(0) : null;
            process(executionContextTool, configurationTool, actions, type);
        }
    }

    private void process(ExecutionContextTool executionContextTool, ConfigurationTool configurationTool, List<IAction> actions, Type type) {
        switch (type) {
            case bytesToNumbers:
                bytesToNumbers(executionContextTool, actions);
                break;
            case numbersToBytes:
                numbersToBytes(executionContextTool, actions);
                break;
            case stringToLong:
                stringToLong(executionContextTool, actions);
                break;
            case stringToDouble:
                stringToDouble(executionContextTool, actions);
                break;
            case numberToString:
                numberToString(executionContextTool, actions);
                break;
            case bytesToString:
                bytesToString(executionContextTool, actions);
                break;
            case stringToBytes:
                stringToBytes(executionContextTool, actions);
                break;
            case toJSONArrayString:
                toJSONString(executionContextTool, actions, true);
                break;
            case toJSONObjectString:
                toJSONString(executionContextTool, actions, false);
                break;
            case fromJSONArrayString:
                fromJSONString(executionContextTool, configurationTool, actions, false);
                break;
            case auto:
                auto(executionContextTool, actions, false);
                break;
            case fromCsvToValues:
                fromCsvToValues(executionContextTool, actions, false);
                break;
            case fromValuesToCsv:
                fromValuesToCsv(executionContextTool, actions);
                break;
            case toLong:
                toLong(executionContextTool, actions);
                break;
            case fromXml:
                fromXML(executionContextTool, actions, false);
                break;
            case toXml:
                toXML(executionContextTool, actions, false);
                break;
            case fromJSONObjectString:
                fromJSONString(executionContextTool, configurationTool, actions, false);
                break;
            case toString:
                toString(executionContextTool, actions);
                break;
            case base64ToBytes:
                base64ToBytes(executionContextTool, configurationTool, actions);
                break;
            case bytesToBase64:
                bytesToBase64(executionContextTool, configurationTool, actions);
                break;
            case listToObjectArray:
                listToObjectArray(executionContextTool, actions);
                break;
            case objectArrayToList:
                objectArrayToList(executionContextTool, actions, true);
                break;
            case stringToNumberOrOrigin:
                stringToNumberOrOrigin(executionContextTool, actions);
                break;
            case objectArrayToMessages:
                objectArrayToMessages(executionContextTool, actions);
                break;
            case messagesToObjectArray:
                messagesToObjectArray(executionContextTool, actions);
                break;
            case objectArrayValuesToList:
                objectArrayToList(executionContextTool, actions, false);
                break;
            case fromJSONArrayStringNew:
                fromJSONString(executionContextTool, configurationTool, actions, true);
                break;
            case fromJSONObjectStringNew:
                fromJSONString(executionContextTool, configurationTool, actions, true);
                break;
            case fromXmlSimple:
                fromXML(executionContextTool, actions, true);
                break;
            case toXmlSimple:
                toXML(executionContextTool, actions, true);
                break;
            case autoNew:
                auto(executionContextTool, actions, true);
                break;
            case toBoolean:
                toBoolean(executionContextTool, actions);
                break;
            case fromCsvToValuesNew:
                fromCsvToValues(executionContextTool, actions, true);
                break;
        }

    }

    @Override
    public void stop(ConfigurationTool configurationTool) throws ModuleException {
        type = null;
        // charsetName = null;
        // columnCount = null;
        param = null;
        lParam = null;
        strings = null;
        // start = null;
        // end = null;
    }

    private void bytesToNumbers(ExecutionContextTool executionContextTool, List<IAction> actions) {
        actions.forEach(a -> a.getMessages().stream()
                .filter(ModuleUtils::isBytes)
                .forEach(m -> {
                    for (byte var : (byte[]) m.getValue())
                        executionContextTool.addMessage(var);
                }));
    }

    private void numbersToBytes(ExecutionContextTool executionContextTool, List<IAction> actions) {
        actions.forEach(a -> {
            List<Byte> numbers = a.getMessages().stream()
                    .filter(ModuleUtils::isNumber)
                    .flatMap(m -> {
                        byte[] bytes = null;
                        Number value = (Number) m.getValue();
                        switch (m.getType()) {
                            case BYTE:
                                bytes = new byte[Byte.BYTES];
                                ByteBuffer.wrap(bytes, 0, Byte.BYTES).put(value.byteValue());
                                break;
                            case SHORT:
                                bytes = new byte[Short.BYTES];
                                ByteBuffer.wrap(bytes, 0, Short.BYTES).putShort(value.shortValue());
                                break;
                            case INTEGER:
                                bytes = new byte[Integer.BYTES];
                                ByteBuffer.wrap(bytes, 0, Integer.BYTES).putInt(value.intValue());
                                break;
                            case LONG:
                                bytes = new byte[Long.BYTES];
                                ByteBuffer.wrap(bytes, 0, Long.BYTES).putLong(value.longValue());
                                break;
                            case BIG_INTEGER:
                                bytes = ((BigInteger) value).toByteArray();
                                break;
                            case FLOAT:
                                bytes = new byte[Float.BYTES];
                                ByteBuffer.wrap(bytes, 0, Float.BYTES).putFloat(value.floatValue());
                                break;
                            case DOUBLE:
                                bytes = new byte[Double.BYTES];
                                ByteBuffer.wrap(bytes, 0, Double.BYTES).putDouble(value.doubleValue());
                                break;
                            case BIG_DECIMAL:
                                bytes = ((BigDecimal) value).toBigInteger().toByteArray();
                                break;
                        }
                        return Arrays.asList(ArrayUtils.toObject(bytes)).stream();
                    })
                    .collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(numbers))
                executionContextTool.addMessage(ArrayUtils.toPrimitive(numbers.toArray(new Byte[numbers.size()])));
        });
    }

    private void stringToLong(ExecutionContextTool executionContextTool, List<IAction> actions) {
        actions.forEach(a -> a.getMessages().stream()
                .filter(ModuleUtils::isString)
                .map(m -> (String) m.getValue())
                .map(s -> s.replaceAll("[^\\d\\s.-]", ""))
                .flatMap(s -> Arrays.stream(s.split("\\s+")))
                .filter(s -> !s.isBlank())
                .filter(NumberUtils::isCreatable)
                .forEach(v -> executionContextTool.addMessage(!StringUtils.contains(v, ".") ? NumberUtils.toLong(v) : (long) NumberUtils.toDouble(v))));
    }

    private void stringToDouble(ExecutionContextTool executionContextTool, List<IAction> actions) {
        actions.forEach(a -> a.getMessages().stream()
                .filter(ModuleUtils::isString)
                .map(m -> (String) m.getValue())
                .map(s -> s.replaceAll("[^\\d\\s.-]", ""))
                .flatMap(s -> Arrays.stream(s.split("\\s+")))
                .filter(s -> !s.isBlank())
                .filter(NumberUtils::isCreatable)
                .forEach(v -> executionContextTool.addMessage(NumberUtils.toDouble(v))));
    }

    private void numberToString(ExecutionContextTool executionContextTool, List<IAction> actions) {
        actions.forEach(a -> a.getMessages().stream()
                .filter(ModuleUtils::isNumber)
                .map(m -> (Number) m.getValue())
                .forEach(v -> executionContextTool.addMessage(v.toString()))
        );
    }

    private void bytesToString(ExecutionContextTool executionContextTool, List<IAction> actions) {
        actions.forEach(a -> a.getMessages().stream()
                .filter(ModuleUtils::isBytes)
                .map(m -> {
                    try {
                        return new String((byte[]) m.getValue(), charset);
                        // return Base64.getEncoder().encodeToString((byte[]) m.getValue());
                    } catch (Exception e) {
                        throw new ModuleException("error", e);
                    }
                })
                .forEach(v -> executionContextTool.addMessage(v))
        );
    }

    private void stringToBytes(ExecutionContextTool executionContextTool, List<IAction> actions) {
        actions.forEach(a -> a.getMessages().stream()
                .filter(ModuleUtils::isString)
                // .filter(m -> base64.matcher((String) m.getValue()).find())
                .map(m -> {
                    try {
                        return ((String) m.getValue()).getBytes(charset);
                        // return Base64.getDecoder().decode((String) m.getValue());
                    } catch (Exception e) {
                        throw new ModuleException("error", e);
                    }
                })
                .forEach(v -> executionContextTool.addMessage(v))
        );
    }

    private void toJSONString(ExecutionContextTool executionContextTool, List<IAction> actions, boolean forceArray) {
        actions.stream()
                .map(a -> {
                    LinkedList<IMessage> messages = new LinkedList<>(a.getMessages());

                    // Integer columnCount = ((Number) messages.poll().getValue()).intValue();
                    // Integer objectsCount = ((a.getMessages().size() - 1) / 2) / columnCount;
                    return toJson(ModuleUtils.deserializeToObject(messages), forceArray);

                    // toJson(json, messages, forceArray);
                    // return forceArray ? json : (json.length() == 1 ? json.get(0) : json);
                })
                .forEach(jsonObj -> executionContextTool.addMessage(jsonObj.toString()));
    }

    private Object toJson(ObjectArray mainList, boolean forceArray) {
        JSONArray jsonArray = new JSONArray();
        if (mainList == null)
            return jsonArray;
        switch (mainList.getType()) {
            case OBJECT_ARRAY:
                for (int i = 0; i < mainList.size(); i++)
                    jsonArray.put(toJson((ObjectArray) mainList.get(i), forceArray));
                break;
            case OBJECT_ELEMENT:
                for (int i = 0; i < mainList.size(); i++)
                    jsonArray.put(toJson((ObjectElement) mainList.get(i), forceArray));
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
            case BOOLEAN:
                for (int i = 0; i < mainList.size(); i++)
                    jsonArray.put(mainList.get(i));
                break;
            case BYTES:
                for (int i = 0; i < mainList.size(); i++)
                    jsonArray.put(Base64.getEncoder().encodeToString((byte[]) mainList.get(i)));
                break;
        }
        return forceArray ? jsonArray : (jsonArray.length() == 1 ? jsonArray.get(0) : jsonArray);
    }

    private JSONObject toJson(ObjectElement objectElement, boolean forceArray) {
        JSONObject jsonObject = new JSONObject();
        if (objectElement == null)
            return jsonObject;
        objectElement.getFields().forEach(objectField -> {
            String name = objectField.getName();
            switch (objectField.getType()) {
                case OBJECT_ARRAY:
                    jsonObject.put(name, toJson((ObjectArray) objectField.getValue(), forceArray));
                    break;
                case OBJECT_ELEMENT:
                    jsonObject.put(name, toJson((ObjectElement) objectField.getValue(), forceArray));
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
                case BOOLEAN:
                    jsonObject.put(name, objectField.getValue());
                    break;
                case BYTES:
                    jsonObject.put(name, objectField.getValue() != null ? Base64.getEncoder().encodeToString((byte[]) objectField.getValue()) : null);
                    break;
            }
        });
        return jsonObject;
    }



    /*
    private void toJson(JSONArray json, LinkedList<IMessage> messages, boolean forceArray) {
        JSONObject obj = null;
        int j = 0;
        Integer columnCount = null;
        while (!messages.isEmpty()) {
            IMessage message = messages.poll();
            boolean needNewObject = false;
            if (columnCount == null || (j % columnCount) == 0) {
                Number number = getNumber(message);
                if (number != null) {
                    columnCount = number.intValue();
                    message = messages.poll();
                }
                if (columnCount == null)
                    break;
                j = 0;
                needNewObject = true;
            }

            String columnName = toString(message);

            if (needNewObject) {
                if (end != null && StringUtils.equals(columnName, end))
                    return;
                obj = new JSONObject();
                json.put(obj);
            }

            IMessage messageValue = messages.poll();
            if (messageValue == null)
                continue;
            // Object value = messageValue.getValue();
            // String value = !messages.isEmpty() ? toString(messages.poll()) : null;
            if (start != null && StringUtils.equals(messageValue.getValue().toString(), start) && !messages.isEmpty()) {
                // IMessage messageNext = messages.poll();
                // Object nextValue = messages.poll().getValue();
                // if (!ValueType.STRING.equals(messageNext.getType()) && !ValueType.BYTES.equals(messageNext.getType())) {
                // Integer innerColumnCount = ((Number) messageNext.getValue()).intValue();
                JSONArray innerJson = new JSONArray();
                toJson(innerJson, messages, forceArray);
                obj.put(columnName, forceArray ? innerJson : (innerJson.length() == 1 ? innerJson.get(0) : innerJson));
                // }
            } else {
                obj.put(columnName, ValueType.BYTES.equals(messageValue.getType()) ? toString(messageValue) : messageValue.getValue());
            }
            j++;
        }
    }
    */

    /*
    private Number getNumber(IMessage m) {
        if (m == null || !(ValueType.BYTE.equals(m.getType()) || ValueType.SHORT.equals(m.getType()) || ValueType.INTEGER.equals(m.getType()) || ValueType.LONG.equals(m.getType()) || ValueType.FLOAT.equals(m.getType()) || ValueType.DOUBLE.equals(m.getType()) || ValueType.BIG_INTEGER.equals(m.getType()) || ValueType.BIG_DECIMAL.equals(m.getType())))
            return null;
        return (Number) m.getValue();
    }
    */

    // формат: цифра - количество полей в объекте, далее пары - название поля, значение
    private void fromJSONString(ExecutionContextTool executionContextTool, ConfigurationTool configurationTool, List<IAction> actions, boolean withNewFuncs) {
        actions.forEach(a -> a.getMessages().stream()
                .filter(m -> ModuleUtils.isString(m) || ModuleUtils.isBytes(m))
                .map(m -> ModuleUtils.isBytes(m) ? new String((byte[]) m.getValue()) : String.valueOf(m.getValue()))
                .forEach(s -> {
                    try {
                        String json = s.trim();
                        if (json.startsWith("[")) {
                            JSONArray jsonArray = new JSONArray(json);
                            executionContextTool.addMessage(fromJSONString(jsonArray, withNewFuncs));
                        } else {
                            JSONObject jsonObject = new JSONObject(json);
                            ObjectElement objectElement = fromJSONString(jsonObject, withNewFuncs);
                            executionContextTool.addMessage(new ObjectArray(List.of(objectElement), ObjectType.OBJECT_ELEMENT));
                        }
                    } catch (Exception e) {
                        configurationTool.loggerWarn(StringUtils.defaultString(e.getMessage(), e.getClass().getName()));
                    }
                }));
    }

    private ObjectArray fromJSONString(JSONArray jsonArray, boolean withNewFuncs) {
        ObjectArray objectArray = new ObjectArray();
        if (jsonArray.isEmpty())
            return objectArray;
        // executionContextTool.addMessage(jsonArray.getJSONObject(0).length());
        ObjectType objectType = null;
        List<Object> list = new LinkedList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            Object obj = jsonArray.get(i);
            if (obj instanceof JSONObject) {
                if (objectType == null) {
                    objectType = ObjectType.OBJECT_ELEMENT;
                } else if (!ObjectType.OBJECT_ELEMENT.equals(objectType)) {
                    continue;
                }
                ObjectElement objectElement = fromJSONString(jsonArray.getJSONObject(i), withNewFuncs);
                list.add(objectElement);
            } else if (obj instanceof JSONArray) {
                if (objectType == null) {
                    objectType = ObjectType.OBJECT_ARRAY;
                } else if (!ObjectType.OBJECT_ARRAY.equals(objectType)) {
                    continue;
                }
                list.add(fromJSONString((JSONArray) obj, withNewFuncs));
            } else {
                if (objectType == null) {
                    objectType = ObjectType.VALUE_ANY;
                } else if (!ObjectType.VALUE_ANY.equals(objectType)) {
                    continue;
                }
                list.add(obj instanceof Number ? obj : (withNewFuncs && obj instanceof Boolean ? obj : fromString(obj.toString(), base64, false, withNewFuncs)));
            }
        }
        if (objectType != null) {
            if (ObjectType.VALUE_ANY.equals(objectType))
                objectType = getSimpleType(list);
            objectArray = new ObjectArray(list, objectType);
        }
        return objectArray;
    }

    public static ObjectType getSimpleType(List<Object> objects) {
        ObjectType newType = null;
        for (int i = 0; i < objects.size(); i++) {
            Object o = objects.get(i);
            newType = checkType(newType, ObjectType.valueOf(ModuleUtils.getValueTypeObject(o).name()));
            if (newType == null)
                break;
        }
        return newType != null ? newType : ObjectType.VALUE_ANY;
    }

    public static ObjectType checkType(ObjectType newType, ObjectType typeForCheck) {
        return newType != null && !typeForCheck.equals(newType) ? null : typeForCheck;
    }

    private ObjectElement fromJSONString(JSONObject jsonObject, boolean withNewFuncs) {
        ObjectElement objectElement = new ObjectElement();
        jsonObject.keySet()//.stream()
                // .filter(k -> jsonObject.get(k) instanceof String)
                .forEach(k -> {
                    Object o = jsonObject.get(k);
                    // executionContextTool.addMessage(k);
                    ObjectField objectField = new ObjectField(k);
                    if (o instanceof JSONArray) {
                        // executionContextTool.addMessage(start);
                        objectField.setValue(fromJSONString((JSONArray) o, withNewFuncs));
                        // executionContextTool.addMessage(end);
                    } else if (o instanceof JSONObject) {
                        // executionContextTool.addMessage(start);
                        objectField.setValue(fromJSONString((JSONObject) o, withNewFuncs));
                        // executionContextTool.addMessage(end);
                    } else if (o != null) {
                        // executionContextTool.addMessage(o instanceof Number ? o : fromString(o.toString(), base64, false));
                        ValueType valueType = ModuleUtils.getValueTypeObject(o);
                        Object value = o;
                        if (valueType == null || valueType == ValueType.STRING || (!withNewFuncs && valueType == ValueType.BOOLEAN)) {
                            value = fromString(o.toString(), base64, false, withNewFuncs);
                            valueType = ModuleUtils.getValueTypeObject(value);
                        }
                        objectField.setValue(ModuleUtils.convertTo(valueType), value);
                    } else {
                        if (!withNewFuncs)
                            return;
                        objectField.setValue((String) null);
                    }
                    objectElement.getFields().add(objectField);
                });
        return objectElement;
    }

    /**
     * to directions convertation: from values to string and from string to values
     *
     * @param executionContextTool
     * @param actions
     */
    private void auto(ExecutionContextTool executionContextTool, List<IAction> actions, boolean withNewFuncs) {
        actions.forEach(a -> executionContextTool.addMessage(
                a.getMessages().stream()
                        .map(m -> {
                            if (ModuleUtils.isString(m)) {
                                return fromString((String) m.getValue(), base64, true, withNewFuncs);
                            } else if (ModuleUtils.isObjectArray(m)) {
                                return auto(ModuleUtils.getObjectArray(m), withNewFuncs);
                            } else {
                                return toString(m);
                            }
                        })
                        .collect(Collectors.toList())));
    }

    private ObjectArray auto(ObjectArray objectArray, boolean withNewFuncs) {
        if (objectArray == null || objectArray.size() == 0)
            return new ObjectArray();
        ObjectArray result = null;
        switch (objectArray.getType()) {
            case OBJECT_ARRAY:
                result = new ObjectArray(objectArray.size(), ObjectType.OBJECT_ARRAY);
                for (int i = 0; i < objectArray.size(); i++)
                    result.add(auto((ObjectArray) objectArray.get(i), withNewFuncs));
                break;
            case OBJECT_ELEMENT:
                result = new ObjectArray(objectArray.size(), ObjectType.OBJECT_ELEMENT);
                for (int i = 0; i < objectArray.size(); i++)
                    result.add(auto((ObjectElement) objectArray.get(i), withNewFuncs));
                break;
            case VALUE_ANY:
                result = new ObjectArray(objectArray.size(), ObjectType.VALUE_ANY);
                for (int i = 0; i < objectArray.size(); i++) {
                    Object value = objectArray.get(i);
                    if (value instanceof String) {
                        result.addValueAny(fromString((String) value, base64, true, withNewFuncs));
                    } else if (value instanceof byte[]) {
                        result.add(Base64.getEncoder().encodeToString((byte[]) value));
                    } else {
                        result.add(value.toString());
                    }
                }
                break;
            case STRING:
                result = new ObjectArray(objectArray.size(), ObjectType.VALUE_ANY);
                for (int i = 0; i < objectArray.size(); i++)
                    result.addValueAny(fromString((String) objectArray.get(i), base64, true, withNewFuncs));
                break;
            case BYTE:
            case SHORT:
            case INTEGER:
            case LONG:
            case FLOAT:
            case DOUBLE:
            case BIG_INTEGER:
            case BIG_DECIMAL:
                result = new ObjectArray(objectArray.size(), ObjectType.STRING);
                for (int i = 0; i < objectArray.size(); i++)
                    result.add(objectArray.get(i).toString());
                break;
            case BYTES:
                result = new ObjectArray(objectArray.size(), ObjectType.STRING);
                for (int i = 0; i < objectArray.size(); i++)
                    result.add(Base64.getEncoder().encodeToString((byte[]) objectArray.get(i)));
                break;
            case BOOLEAN:
                result = new ObjectArray(objectArray.size(), withNewFuncs ? ObjectType.INTEGER : ObjectType.STRING);
                for (int i = 0; i < objectArray.size(); i++) {
                    if (withNewFuncs) {
                        result.add((Boolean) objectArray.get(i) ? 1 : 0);
                    } else {
                        result.add(objectArray.get(i).toString());
                    }
                }
                break;
        }
        return result;
    }

    private ObjectElement auto(ObjectElement objectElement, boolean withNewFuncs) {
        ObjectElement result = new ObjectElement();
        if (objectElement == null || objectElement.getFields().isEmpty())
            return result;
        objectElement.getFields().forEach(f -> {
            if (f.getValue() == null) {
                result.getFields().add(new ObjectField(f.getName(), f));
                return;
            }
            switch (f.getType()) {
                case OBJECT_ARRAY:
                    result.getFields().add(new ObjectField(f.getName(), auto((ObjectArray) f.getValue(), withNewFuncs)));
                    break;
                case OBJECT_ELEMENT:
                    result.getFields().add(new ObjectField(f.getName(), auto((ObjectElement) f.getValue(), withNewFuncs)));
                    break;
                case VALUE_ANY: {
                    Object value = f.getValue();
                    if (value instanceof String) {
                        value = fromString((String) value, base64, true, withNewFuncs);
                        result.getFields().add(new ObjectField(f.getName(), ModuleUtils.convertTo(ModuleUtils.getValueTypeObject(value)), value));
                    } else if (value instanceof byte[]) {
                        result.getFields().add(new ObjectField(f.getName(), Base64.getEncoder().encodeToString((byte[]) value)));
                    } else {
                        result.getFields().add(new ObjectField(f.getName(), value.toString()));
                    }
                    break;
                }
                case STRING: {
                    Object value = fromString((String) f.getValue(), base64, true, withNewFuncs);
                    result.getFields().add(new ObjectField(f.getName(), ModuleUtils.convertTo(ModuleUtils.getValueTypeObject(value)), value));
                    break;
                }
                case BYTE:
                case SHORT:
                case INTEGER:
                case LONG:
                case FLOAT:
                case DOUBLE:
                case BIG_INTEGER:
                case BIG_DECIMAL:
                    result.getFields().add(new ObjectField(f.getName(), f.getValue().toString()));
                    break;
                case BYTES:
                    result.getFields().add(new ObjectField(f.getName(), Base64.getEncoder().encodeToString((byte[]) f.getValue())));
                    break;
                case BOOLEAN:
                    if (withNewFuncs) {
                        result.getFields().add(new ObjectField(f.getName(), (Boolean) f.getValue() ? 1 : 0));
                    } else {
                        result.getFields().add(new ObjectField(f.getName(), f.getValue().toString()));
                    }
                    break;
            }
        });
        return result;
    }

    private String toString(IMessage m) {
        String result;
        switch (m.getType()) {
            case STRING:
                result = (String) m.getValue();
                break;
            case BYTES:
                result = Base64.getEncoder().encodeToString((byte[]) m.getValue());
                break;
            default:
                result = m.getValue().toString();
                break;
        }
        return result;
    }

    private Object fromString(String value, Pattern base64, boolean toNumbers, boolean withBoolean) {
        Object result;
        if (toNumbers && NumberUtils.isCreatable(value)) {
            result = NumberUtils.createNumber(value);
        } else if (toNumbers && NumberUtils.isCreatable(value.replaceAll(" ", ""))) {
            result = NumberUtils.createNumber(value.replaceAll(" ", ""));
        } else if (withBoolean && ("true".equals(value) || "false".equals(value))) {
            result = "true".equals(value);
        } else {
            if (StringUtils.length(value) >= 2 && !value.isBlank() && base64.matcher(value).find()) {
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

    private void toBoolean(ExecutionContextTool executionContextTool, List<IAction> actions) {
        actions.forEach(a -> {
            if (!ModuleUtils.hasData(a)) {
                executionContextTool.addMessage(false);
            } else {
                a.getMessages().forEach(m -> executionContextTool.addMessage(ModuleUtils.toBoolean(m)));
            }
        });
    }

    private void fromCsvToValues(ExecutionContextTool executionContextTool, List<IAction> actions, boolean withNewFuncs) {
        actions.forEach(a -> executionContextTool.addMessage(a.getMessages().stream()
                .filter(m -> ModuleUtils.isString(m) || ModuleUtils.isBytes(m))
                .map(m -> ModuleUtils.isBytes(m) ? new String((byte[]) m.getValue()) : String.valueOf(m.getValue()))
                .flatMap(s -> Arrays.stream((s).split("\\R")))
                .flatMap(s -> Arrays.stream(s.split(param)))
                .map(value -> fromString(value, base64, true, withNewFuncs))
                .collect(Collectors.toList())));
    }

    private void fromValuesToCsv(ExecutionContextTool executionContextTool, List<IAction> actions) {
        List<String> strings = actions.stream()
                .flatMap(a -> a.getMessages().stream())
                .map(this::toString)
                .collect(Collectors.toList());

        int rowSize = lParam.intValue();
        List<String> resultList = new LinkedList<>();
        for (int i = 0; i + rowSize <= strings.size(); i = i + rowSize)
            resultList.add(String.join(param, strings.subList(i, i + rowSize)));

        executionContextTool.addMessage(String.join("\n", resultList));
    }

    private void toLong(ExecutionContextTool executionContextTool, List<IAction> actions) {
        actions.forEach(a -> a.getMessages()
                .forEach(m -> {
                    switch (m.getType()) {
                        case STRING:
                            executionContextTool.addMessage(NumberUtils.toLong((String) m.getValue()));
                            break;
                        case BYTE:
                        case SHORT:
                        case INTEGER:
                        case LONG:
                        case BIG_INTEGER:
                        case FLOAT:
                        case DOUBLE:
                        case BIG_DECIMAL:
                            executionContextTool.addMessage(((Number) m.getValue()).longValue());
                            break;
                        case BYTES:
                            executionContextTool.addMessage(((byte[]) m.getValue()).length);
                            break;
                        case OBJECT_ARRAY:
                            executionContextTool.addMessage(((ObjectArray) m.getValue()).size());
                            break;
                        case BOOLEAN:
                            executionContextTool.addMessage(((Boolean) m.getValue()) ? 1 : 0);
                            break;
                    }
                }));
    }

    private Element createDOM(String strXML) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        //dbf.setValidating(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        InputSource sourceXML = new InputSource(new StringReader(strXML));
        Document xmlDoc = db.parse(sourceXML);
        Element e = xmlDoc.getDocumentElement();
        e.normalize();
        return e;
    }

    private void fromXML(ExecutionContextTool executionContextTool, List<IAction> actions, boolean useSimple) {
        actions.forEach(a -> a.getMessages().stream()
                .filter(m -> ModuleUtils.isString(m) || ModuleUtils.isBytes(m))
                .map(m -> ModuleUtils.isBytes(m) ? new String((byte[]) m.getValue()) : String.valueOf(m.getValue()))
                .forEach(s -> {
                    Element element = null;
                    try {
                        element = createDOM(s);
                    } catch (Exception e) {
                        executionContextTool.addError(e.getClass().getSimpleName() + ": " + e.getMessage());
                    }
                    ObjectArray objectArray = useSimple && element != null ? new ObjectArray(fromXMLSimple(List.of(element))) : new ObjectArray(fromXML(element));
                    executionContextTool.addMessage(objectArray);
                }));
    }

    private ObjectElement fromXML(Element element) {
        ObjectElement result = null;

        if (element == null)
            return result;

        result = new ObjectElement();

        result.getFields().add(new ObjectField(OBJECT_FILED_NAME_NAME, element.getNodeName()));

        NodeList childNodes = element.getChildNodes();
        String textContent = null;//element.getNodeValue();//element.getTextContent();
        if (childNodes != null && childNodes.getLength() > 0) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < childNodes.getLength(); i++) {
                Node node = childNodes.item(i);
                if (Node.TEXT_NODE == node.getNodeType())
                    sb.append(node.getNodeValue());
            }

            textContent = sb.toString().trim();
        }
        if (textContent == null || textContent.isEmpty())
            textContent = OBJECT_FILED_VALUE_VALUE_EMPTY;
        Object value = fromString(textContent, base64, true, false);
        result.getFields().add(new ObjectField(OBJECT_FILED_NAME_VALUE, ModuleUtils.getObjectType(value), value));

        NamedNodeMap attributes = element.getAttributes();
        if (attributes != null && attributes.getLength() > 0) {
            for (int i = 0; i < attributes.getLength(); i++) {
                Node item = attributes.item(i);
                if (NumberUtils.isCreatable(item.getNodeValue())) {
                    result.getFields().add(new ObjectField(item.getNodeName(), NumberUtils.createNumber(item.getNodeValue())));
                } else {
                    result.getFields().add(new ObjectField(item.getNodeName(), item.getNodeValue()));
                }
            }
        }

        if (childNodes != null && childNodes.getLength() > 0) {
            List<Object> objectElements = new LinkedList<>();
            for (int i = 0; i < childNodes.getLength(); i++) {
                Node node = childNodes.item(i);
                if (Node.ELEMENT_NODE == node.getNodeType())
                    objectElements.add(fromXML((Element) node));
            }
            if (!objectElements.isEmpty())
                result.getFields().add(new ObjectField(OBJECT_FILED_NAME_CHILD_NODES, new ObjectArray(objectElements, ObjectType.OBJECT_ELEMENT)));
        }

        return result;
    }

    private ObjectElement fromXMLSimple(List<Element> elements) {
        ObjectElement result = new ObjectElement();
        if (elements == null)
            return result;
        elements.forEach(element -> {
            ObjectField objectField = new ObjectField(element.getNodeName());
            NodeList childNodes = element.getChildNodes();
            List<Element> childElements = new LinkedList<>();
            StringBuilder sb = new StringBuilder();
            if (childNodes != null && childNodes.getLength() > 0) {
                for (int i = 0; i < childNodes.getLength(); i++) {
                    Node node = childNodes.item(i);
                    if (Node.ELEMENT_NODE == node.getNodeType()) {
                        childElements.add((Element) node);
                    } else if (Node.TEXT_NODE == node.getNodeType()) {
                        sb.append(node.getNodeValue());
                    }
                }
            }
            if (!childElements.isEmpty()) {
                objectField.setValue(fromXMLSimple(childElements));
            } else {
                Object value = fromString(sb.toString(), base64, true, true);
                objectField.setValue(ModuleUtils.getObjectType(value), value);
            }
            result.getFields().add(objectField);
        });
        return result;
    }

    private void toXML(ExecutionContextTool executionContextTool, List<IAction> actions, boolean useSimple) {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder;
        try {
            docBuilder = docFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            // e.printStackTrace();
            executionContextTool.addError(e.getClass().getSimpleName() + ": " + e.getMessage());
            return;
        }

        actions.stream()
                .map(a -> {
                    LinkedList<IMessage> messages = new LinkedList<>(a.getMessages());

                    Document doc = docBuilder.newDocument();
                    ObjectArray objectArray = ModuleUtils.deserializeToObject(messages);
                    ObjectElement objectElement = (ObjectElement) objectArray.get(0);
                    Element rootElement;
                    if (useSimple) {
                        List<Element> elements = toXMLSimple(objectElement, doc);
                        if (elements.size() == 1) {
                            rootElement = elements.get(0);
                        } else {
                            rootElement = doc.createElement("root");
                            elements.forEach(rootElement::appendChild);
                        }
                    } else {
                        rootElement = toXML(objectElement, doc);
                    }
                    doc.appendChild(rootElement);
                    StringWriter out = new StringWriter();
                    try {
                        prettyPrint(doc, out);
                        return out.toString();
                    } catch (Exception e) {
                        // e.printStackTrace();
                        executionContextTool.addError(e.getClass().getSimpleName() + ": " + e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .forEach(jsonObj -> executionContextTool.addMessage(jsonObj.toString()));
    }

    private Element toXML(ObjectElement objectElement, Document doc) {
        Element result = null;

        if (objectElement.getFields().isEmpty())
            return result;

        int size = objectElement.getFields().size();
        if (size < 2)
            return result;

        ObjectField objectField = objectElement.getFields().get(0);
        result = doc.createElement(objectField.getValue().toString());

        objectField = objectElement.getFields().get(1);
        String content = objectField.getValue().toString();
        if (!OBJECT_FILED_VALUE_VALUE_EMPTY.equals(content))
            result.appendChild(doc.createTextNode(content));// result.setTextContent(content);

        ObjectField objectFieldChilds = objectElement.getFields().get(size - 1);
        if (OBJECT_FILED_NAME_CHILD_NODES.equals(objectFieldChilds.getName()) && ObjectType.OBJECT_ARRAY.equals(objectFieldChilds.getType())) {
            size--;
        } else {
            objectFieldChilds = null;
        }

        for (int i = 2; i < size; i++) {
            objectField = objectElement.getFields().get(i);
            Attr attribute = doc.createAttribute(objectField.getName());
            attribute.setValue(objectField.getValue().toString());
            result.setAttributeNode(attribute);
        }

        if (objectFieldChilds != null) {
            ObjectArray objectArray = (ObjectArray) objectFieldChilds.getValue();
            for (int i = 0; i < objectArray.size(); i++) {
                ObjectElement objectElementChild = (ObjectElement) objectArray.get(i);
                result.appendChild(toXML(objectElementChild, doc));
            }
        }

        return result;
    }

    private List<Element> toXMLSimple(ObjectElement objectElement, Document doc) {
        if (objectElement == null || doc == null)
            return new ArrayList<>();
        return objectElement.getFields().stream()
                .map(f -> {
                    Element element = doc.createElement(f.getName());
                    if (ModuleUtils.isObjectArray(f) && ModuleUtils.isArrayContainObjectElements((ObjectArray) f.getValue())) {
                        ObjectArray objectArray = (ObjectArray) f.getValue();
                        List<Element> innerElements = new ArrayList<>();
                        for (int i = 0; i < objectArray.size(); i++)
                            innerElements.addAll(toXMLSimple((ObjectElement) objectArray.get(i), doc));
                        innerElements.forEach(element::appendChild);
                    } else if (ModuleUtils.isObjectElement(f)) {
                        toXMLSimple((ObjectElement) f.getValue(), doc).forEach(element::appendChild);
                    } else if (f.getValue() != null) {
                        element.appendChild(doc.createTextNode(f.getValue().toString()));
                    }
                    return element;
                }).collect(Collectors.toList());
    }

    private void prettyPrint(Node xml, StringWriter out) throws TransformerFactoryConfigurationError, TransformerException {
        Transformer tf = TransformerFactory.newInstance().newTransformer();
        //tf.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        tf.setOutputProperty(OutputKeys.INDENT, "yes");
        tf.transform(new DOMSource(xml), new StreamResult(out));
    }

    private void toString(ExecutionContextTool executionContextTool, List<IAction> actions) {
        actions.forEach(a -> executionContextTool.addMessage(a.getMessages().stream()
                .map(m -> ModuleUtils.isBytes(m) ? new String((byte[]) m.getValue()) : String.valueOf(m.getValue()))
                .collect(Collectors.toList())));
    }

    private void base64ToBytes(ExecutionContextTool executionContextTool, ConfigurationTool configurationTool, List<IAction> actions) {
        actions.forEach(a -> a.getMessages().stream()
                .filter(ModuleUtils::isString)
                .map(m -> (String) m.getValue())
                .forEach(s -> {
                    try {
                        executionContextTool.addMessage(Base64.getDecoder().decode(s));
                    } catch (Exception e) {
                        configurationTool.loggerWarn(StringUtils.defaultString(e.getMessage(), e.getClass().getName()));
                    }
                }));
    }

    private void bytesToBase64(ExecutionContextTool executionContextTool, ConfigurationTool configurationTool, List<IAction> actions) {
        actions.forEach(a -> a.getMessages().stream()
                .filter(ModuleUtils::isBytes)
                .map(m -> (byte[]) m.getValue())
                .forEach(b -> {
                    try {
                        executionContextTool.addMessage(Base64.getEncoder().encodeToString(b));
                    } catch (Exception e) {
                        configurationTool.loggerWarn(StringUtils.defaultString(e.getMessage(), e.getClass().getName()));
                    }
                }));
    }

    private void listToObjectArray(ExecutionContextTool executionContextTool, List<IAction> actions) {
        ObjectArray objectArray = null;
        List<Object> values = actions.stream()
                .flatMap(a -> a.getMessages().stream())
                .map(IValue::getValue)
                .collect(Collectors.toList());
        if (lParam > 0) {
            objectArray = new ObjectArray();
            if (strings != null) {
                for (int i = 0; i + lParam - 1 < values.size(); i += lParam) {
                    ObjectElement objectElement = new ObjectElement();
                    for (int j = 0; j < lParam; j++) {
                        Object value = values.get(i + j);
                        objectElement.getFields().add(new ObjectField(strings.get(j), ModuleUtils.getObjectType(value), value));
                    }
                    objectArray.add(objectElement);
                }
            } else {
                int elementsInObject = (int) (lParam * 2);
                for (int i = 0; i + elementsInObject - 1 < values.size(); i += elementsInObject) {
                    ObjectElement objectElement = new ObjectElement();
                    for (int j = 0; j < lParam; j++) {
                        Object value = values.get(i + j * 2 + 1);
                        objectElement.getFields().add(new ObjectField(values.get(i + j * 2).toString(), ModuleUtils.getObjectType(value), value));
                    }
                    objectArray.add(objectElement);
                }
            }
        } else {
            objectArray = new ObjectArray(values, getSimpleType(values));
        }
        executionContextTool.addMessage(objectArray);
    }

    private void objectArrayToList(ExecutionContextTool executionContextTool, List<IAction> actions, boolean printFieldNames) {
        actions.stream()
                .map(a -> ModuleUtils.deserializeToObject(new LinkedList<>(a.getMessages())))
                .forEach(objectArray -> {
                    if (ModuleUtils.isArrayContainObjectElements(objectArray)) {
                        executionContextTool.addMessage(
                                Stream.iterate(0, n -> n + 1)
                                        .limit(objectArray.size())
                                        .map(id -> (ObjectElement) objectArray.get(id))
                                        .flatMap(e -> e.getFields().stream())
                                        .filter(ObjectField::isSimple)
                                        .flatMap(f -> printFieldNames ? Stream.of(f.getName(), f.getValue()) : Stream.of(f.getValue()))
                                        .collect(Collectors.toList()));

                    } else if (objectArray.size() > 0) {
                        executionContextTool.addMessage(
                                Stream.iterate(0, n -> n + 1)
                                        .limit(objectArray.size())
                                        .map(objectArray::get)
                                        .collect(Collectors.toList()));
                    }
                });
    }

    private void stringToNumberOrOrigin(ExecutionContextTool executionContextTool, List<IAction> actions) {
        executionContextTool.addMessage(
                actions.stream().flatMap(a -> a.getMessages().stream()
                                .map(m -> {
                                    if (ModuleUtils.isString(m)) {
                                        String str = ModuleUtils.getString(m);
                                        if (NumberUtils.isCreatable(str)) {
                                            return NumberUtils.createNumber(str);
                                        } else if (NumberUtils.isCreatable(str.replaceAll(" ", ""))) {
                                            return NumberUtils.createNumber(str.replaceAll(" ", ""));
                                        }
                                        return str;
                                    }
                                    return m.getValue();
                                }))
                        .collect(Collectors.toList()));
    }

    private void objectArrayToMessages(ExecutionContextTool executionContextTool, List<IAction> actions) {
        executionContextTool.addMessage(
                actions.stream()
                        .flatMap(a -> a.getMessages().stream()
                                .filter(ModuleUtils::isObjectArray)
                                .flatMap(m -> ModuleUtils.serializeFromObject(ModuleUtils.getObjectArray(m)).stream()))
                        .collect(Collectors.toList()));
    }

    private void messagesToObjectArray(ExecutionContextTool executionContextTool, List<IAction> actions) {
        executionContextTool.addMessage(actions.stream()
                .map(a -> ModuleUtils.deserializeToObject(new LinkedList<>(a.getMessages())))
                .collect(Collectors.toList()));
    }

}
