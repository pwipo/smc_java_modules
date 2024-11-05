package ru.smcsystem.smcmodules.module;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import ru.smcsystem.api.dto.IValue;
import ru.smcsystem.api.exceptions.ModuleException;
import ru.smcsystem.api.module.Module;
import ru.smcsystem.api.tools.ConfigurationTool;
import ru.smcsystem.api.tools.execution.ExecutionContextTool;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Comparator implements Module {
    private static final Pattern base64 = Pattern.compile("^([A-Za-z0-9+\\/]{4})*([A-Za-z0-9+\\/]{4}|[A-Za-z0-9+\\/]{3}=|[A-Za-z0-9+\\/]{2}==)$");
    private Object moreResult;
    private Object lessResult;
    private Object equalsResult;
    private Object value;

    @Override
    public void start(ConfigurationTool configurationTool) throws ModuleException {
        moreResult = autoConvert((String) configurationTool.getSetting("moreResult").orElseThrow(() -> new ModuleException("moreResult setting not found")).getValue());
        lessResult = autoConvert((String) configurationTool.getSetting("lessResult").orElseThrow(() -> new ModuleException("lessResult setting not found")).getValue());
        equalsResult = autoConvert((String) configurationTool.getSetting("equalsResult").orElseThrow(() -> new ModuleException("equalsResult setting not found")).getValue());
        Boolean useValue = Boolean.valueOf((String) configurationTool.getSetting("useValue").orElseThrow(() -> new ModuleException("useValue setting not found")).getValue());
        value = null;
        if (useValue)
            value = autoConvert((String) configurationTool.getSetting("value").orElseThrow(() -> new ModuleException("value setting not found")).getValue());
    }

    private Object autoConvert(String value) {
        Object result;
        if (NumberUtils.isNumber(value)) {
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
                result = !value.isEmpty() ? value : null;
            }
        }
        return result;
    }

    @Override
    public void update(ConfigurationTool configurationTool) throws ModuleException {
        stop(configurationTool);
        start(configurationTool);
    }

    @Override
    public void process(ConfigurationTool configurationTool, ExecutionContextTool executionContextTool) throws ModuleException {
        List<Object> values = Stream.iterate(0, i -> i + 1)
                .limit(executionContextTool.countSource())
                .flatMap(i -> executionContextTool.getMessages(i).stream())
                .flatMap(action -> action.getMessages().stream())
                // .filter(m -> m.getValue() instanceof Number)
                .map(IValue::getValue)
                .collect(Collectors.toList());

        if (value == null) {
            for (int i = 0; i < values.size() - 1; i = i + 2)
                compare(executionContextTool, values.get(i), values.get(i + 1));
        } else {
            values.forEach(o1 -> compare(executionContextTool, value, o1));
        }
    }

    private void compare(ExecutionContextTool executionContextTool, Object o1, Object o2) {
        int compareResult;
        if (o1 instanceof Number && o2 instanceof Number) {
            compareResult = toBigDecimal((Number) o1).compareTo(toBigDecimal((Number) o2));
        } else if (o1 instanceof byte[] && o2 instanceof byte[]) {
            compareResult = Arrays.compare((byte[]) o1, (byte[]) o2);
        } else {
            compareResult = o1.toString().compareTo(o2.toString());
        }

        if (compareResult > 0 && moreResult != null) {
            executionContextTool.addMessage(List.of(moreResult));
        } else if (compareResult == 0 && equalsResult != null) {
            executionContextTool.addMessage(List.of(equalsResult));
        } else if (compareResult < 0 && lessResult != null) {
            executionContextTool.addMessage(List.of(lessResult));
        }
    }

    private BigDecimal toBigDecimal(Number value) {
        if (value instanceof Byte) {
            return new BigDecimal((Byte) value);
        } else if (value instanceof Short) {
            return new BigDecimal((Short) value);
        } else if (value instanceof Integer) {
            return new BigDecimal((Integer) value);
        } else if (value instanceof Long) {
            return new BigDecimal((Long) value);
        } else if (value instanceof BigInteger) {
            return new BigDecimal((BigInteger) value);
        } else if (value instanceof Float) {
            return new BigDecimal((Float) value);
        } else if (value instanceof Double) {
            return new BigDecimal((Double) value);
        } else {
            return (BigDecimal) value;
        }
    }

    @Override
    public void stop(ConfigurationTool configurationTool) throws ModuleException {
        moreResult = null;
        lessResult = null;
        equalsResult = null;
        value = null;
    }

}
