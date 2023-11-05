package ru.smcsystem.modules.configurationControllerChangeSettingString;

import ru.smcsystem.api.dto.ObjectArray;
import ru.smcsystem.api.dto.ObjectElement;
import ru.smcsystem.api.exceptions.ModuleException;
import ru.smcsystem.api.module.Module;
import ru.smcsystem.api.tools.ConfigurationTool;
import ru.smcsystem.api.tools.execution.ExecutionContextTool;
import ru.smcsystem.smc.utils.ModuleUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

public class ChangeSetting implements Module {

    // private List<String> names;
    private String key;
    private String value;

    @Override
    public void start(ConfigurationTool configurationTool) throws ModuleException {
        /*
        String strNames = (String) configurationTool.getSetting("names").orElseThrow(() -> new ModuleException("key setting not found")).getValue();
        this.names = new ArrayList<>();
        for (String name : strNames.split(","))
            this.names.add(StringUtils.trim(name));
        */
        key = (String) configurationTool.getSetting("key").orElseThrow(() -> new ModuleException("key setting not found")).getValue();
        value = (String) configurationTool.getSetting("value").orElseThrow(() -> new ModuleException("value setting not found")).getValue();
    }

    @Override
    public void process(ConfigurationTool configurationTools, ExecutionContextTool executionContextTool) throws ModuleException {
        if (executionContextTool.getType().equals("default")) {
            if (executionContextTool.countSource() == 0) {
                change(executionContextTool, key, value);
            } else {
                Stream.iterate(0, n -> n + 1)
                        .limit(executionContextTool.countSource())
                        .flatMap(i -> executionContextTool.getMessages(i).stream())
                        .flatMap(a -> a.getMessages().stream())
                        .forEach(m -> {
                            change(executionContextTool, key, m.getValue());
                        });
            }
        } else if (executionContextTool.getType().equals("set")) {
            ModuleUtils.processMessages(configurationTools, executionContextTool, 0, (id, messages) -> {
                if (messages == null)
                    return;
                ObjectArray objectArray = ModuleUtils.deserializeToObject(messages);
                if (ModuleUtils.isArrayContainObjectElements(objectArray)) {
                    ((ObjectElement) objectArray.get(0)).getFields()
                            .forEach(f -> {
                                if (f.getValue() != null)
                                    change(executionContextTool, f.getName(), f.getValue());
                            });
                }
            });
        }
    }

    private void change(ExecutionContextTool executionContextTool, String key, Object value) {
        List<Object> result = new LinkedList<>();
        for (int i = 0; i < executionContextTool.getConfigurationControlTool().countManagedConfigurations(); i++) {
            int j = i;
            executionContextTool.getConfigurationControlTool().getManagedConfiguration(i).ifPresent(c -> {
                c.getSetting(key).ifPresent(v -> {
                    result.add(String.format("%d: %s, key: %s", j, c.getName(), key));
                    Object oldValue = v.getValue();
                    result.add(oldValue);
                    Object newValue = null;
                    switch (v.getType()) {
                        case STRING:
                            newValue = value.toString();
                            c.setSetting(key, (String) newValue);
                            break;
                        case BYTE:
                            newValue = value instanceof String ? Byte.parseByte(value.toString()) : (value instanceof Number ? ((Number) value).byteValue() : (Byte) value);
                            c.setSetting(key, (Byte) newValue);
                            break;
                        case SHORT:
                            newValue = value instanceof String ? Short.parseShort((String) value) : (value instanceof Number ? ((Number) value).byteValue() : (Short) value);
                            c.setSetting(key, (Short) newValue);
                            break;
                        case INTEGER:
                            newValue = value instanceof String ? Integer.parseInt(value.toString()) : (value instanceof Number ? ((Number) value).byteValue() : (Integer) value);
                            c.setSetting(key, (Integer) newValue);
                            break;
                        case LONG:
                            newValue = value instanceof String ? Long.parseLong(value.toString()) : (value instanceof Number ? ((Number) value).byteValue() : (Long) value);
                            c.setSetting(key, (Long) newValue);
                            break;
                        case BIG_INTEGER:
                            newValue = value instanceof String ? new BigInteger(value.toString()) : (value instanceof Number ? BigInteger.valueOf(((Number) value).longValue()) : (BigInteger) value);
                            c.setSetting(key, (BigInteger) newValue);
                            break;
                        case FLOAT:
                            newValue = value instanceof String ? Float.parseFloat(value.toString()) : (value instanceof Number ? ((Number) value).byteValue() : (Float) value);
                            c.setSetting(key, (Float) newValue);
                            break;
                        case DOUBLE:
                            newValue = value instanceof String ? Double.parseDouble(value.toString()) : (value instanceof Number ? ((Number) value).byteValue() : (Double) value);
                            c.setSetting(key, (Double) newValue);
                            break;
                        case BIG_DECIMAL:
                            newValue = value instanceof String ? new BigDecimal(value.toString()) : (value instanceof Number ? BigDecimal.valueOf(((Number) value).doubleValue()) : (BigDecimal) value);
                            c.setSetting(key, (BigDecimal) newValue);
                            break;
                        case BYTES:
                            newValue = value instanceof String ? Base64.getDecoder().decode(value.toString()) : (byte[]) value;
                            c.setSetting(key, (byte[]) newValue);
                            break;
                        case OBJECT_ARRAY:
                            c.setSetting(key, (ObjectArray) newValue);
                            break;
                        case BOOLEAN:
                            newValue = value instanceof String ? Boolean.parseBoolean(value.toString()) : (value instanceof Number ? ((Number) value).longValue() > 0 : (Boolean) value);
                            c.setSetting(key, (Boolean) newValue);
                            break;
                    }
                    result.add(newValue);
                });
            });
        }
        executionContextTool.addMessage(result);
    }

    @Override
    public void update(ConfigurationTool configurationTool) throws ModuleException {
        stop(configurationTool);
        start(configurationTool);
    }

    @Override
    public void stop(ConfigurationTool configurationTool) throws ModuleException {
        // this.names = null;
        key = null;
        value = null;
    }
}
