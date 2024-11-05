package ru.smcsystem.smcmodules.module;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import ru.smcsystem.api.dto.IValue;
import ru.smcsystem.api.dto.ObjectArray;
import ru.smcsystem.api.exceptions.ModuleException;
import ru.smcsystem.api.module.Module;
import ru.smcsystem.api.tools.ConfigurationTool;
import ru.smcsystem.api.tools.execution.ExecutionContextTool;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PrintValue implements Module {
    private static final Pattern base64 = Pattern.compile("^([A-Za-z0-9+\\/]{4})*([A-Za-z0-9+\\/]{4}|[A-Za-z0-9+\\/]{3}=|[A-Za-z0-9+\\/]{2}==)$");
    private Type type;
    private AppendType appendType;
    private List<Object> values;
    private ObjectArray arrayValue;

    @Override
    public void start(ConfigurationTool configurationTool) throws ModuleException {
        type = Type.valueOf((String) configurationTool.getSetting("type").orElseThrow(() -> new ModuleException("text setting")).getValue());
        appendType = AppendType.valueOf((String) configurationTool.getSetting("appendType").orElseThrow(() -> new ModuleException("appendType setting")).getValue());
        String textValue = (String) configurationTool.getSetting("value").orElseThrow(() -> new ModuleException("text setting")).getValue();
        String splitterValues = (String) configurationTool.getSetting("splitterValues").orElseThrow(() -> new ModuleException("splitterValues setting")).getValue();
        arrayValue = (ObjectArray) configurationTool.getSetting("arrayValue").orElseThrow(() -> new ModuleException("arrayValue setting")).getValue();

        values = !textValue.isEmpty() ?
                Arrays.stream(textValue.split(splitterValues))
                        .map(text -> {
                            Object value;
                            switch (type) {
                                case STRING:
                                    value = text;
                                    break;
                                case BYTE:
                                    value = Byte.valueOf(text);
                                    break;
                                case SHORT:
                                    value = Short.valueOf(text);
                                    break;
                                case INTEGER:
                                    value = Integer.valueOf(text);
                                    break;
                                case LONG:
                                    value = Long.valueOf(text);
                                    break;
                                case BIG_INTEGER:
                                    value = new BigInteger(text);
                                    break;
                                case FLOAT:
                                    value = Float.valueOf(text);
                                    break;
                                case DOUBLE:
                                    value = Double.valueOf(text);
                                    break;
                                case BIG_DECIMAL:
                                    value = new BigDecimal(text);
                                    break;
                                case AUTO:
                                    value = autoConvert(text);
                                    break;
                                default:
                                    value = null;
                            }
                            return value;
                        })
                        .collect(Collectors.toList()) :
                List.of();

        if (AppendType.PLACEHOLDER.equals(appendType) && !values.isEmpty()) {
            Pattern forPlaceholder = Pattern.compile("\\{(\\d*)\\}");
            for (int i = 0; i < values.size(); i++) {
                Object o = values.get(i);
                if (o instanceof String) {
                    Matcher matcher = forPlaceholder.matcher(o.toString());
                    if (matcher.matches())
                        values.set(i, new ExecutionContextHolder(Integer.parseInt(matcher.group(1))));
                }
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
        List<Object> inputValues = null;
        if (executionContextTool.countSource() > 0 && !AppendType.PLACEHOLDER.equals(appendType)) {
            inputValues = Stream.iterate(0, n -> n + 1)
                    .limit(executionContextTool.countSource())
                    .flatMap(n -> executionContextTool.getMessages(n).stream())
                    .flatMap(a -> a.getMessages().stream())
                    .map(IValue::getValue)
                    .collect(Collectors.toList());
        }

        if (inputValues != null && AppendType.LAST.equals(appendType))
            executionContextTool.addMessage(inputValues);

        List<Object> result = new LinkedList<>();
        values.forEach(value -> {
            if (AppendType.PLACEHOLDER.equals(appendType) && value instanceof ExecutionContextHolder) {
                ((ExecutionContextHolder) value).process(executionContextTool, result);
            } else {
                result.add(value);
            }
        });
        if (arrayValue.size() > 0)
            result.add(arrayValue);
        executionContextTool.addMessage(result);

        if (inputValues != null && AppendType.FIRST.equals(appendType))
            executionContextTool.addMessage(inputValues);
    }

    @Override
    public void stop(ConfigurationTool configurationTool) throws ModuleException {
        type = null;
        appendType = null;
        values = null;
    }

    private Object autoConvert(String value) {
        Object result;
        if (NumberUtils.isCreatable(value)) {
            if (!StringUtils.contains(value, ".")) {
                result = NumberUtils.toLong(value);
            } else {
                result = NumberUtils.toDouble(value);
            }

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

    private enum Type {
        STRING,
        BYTE,
        SHORT,
        INTEGER,
        LONG,
        BIG_INTEGER,
        FLOAT,
        DOUBLE,
        BIG_DECIMAL,
        AUTO,
    }

    private enum AppendType {
        FIRST,
        LAST,
        PLACEHOLDER
    }

    private class ExecutionContextHolder {
        private int id;

        public ExecutionContextHolder(int id) {
            this.id = id;
        }

        public void process(ExecutionContextTool executionContextTool, List<Object> list) {
            list.addAll(
                    executionContextTool.getMessages(id).stream()
                            .flatMap(a -> a.getMessages().stream())
                            .map(IValue::getValue)
                            .collect(Collectors.toList()));
        }
    }

}
