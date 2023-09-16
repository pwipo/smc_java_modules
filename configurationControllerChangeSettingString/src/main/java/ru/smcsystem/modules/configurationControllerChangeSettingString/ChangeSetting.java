package ru.smcsystem.modules.configurationControllerChangeSettingString;

import ru.smcsystem.api.dto.ObjectArray;
import ru.smcsystem.api.exceptions.ModuleException;
import ru.smcsystem.api.module.Module;
import ru.smcsystem.api.tools.ConfigurationTool;
import ru.smcsystem.api.tools.execution.ExecutionContextTool;

import java.math.BigDecimal;
import java.math.BigInteger;
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
        if (executionContextTool.countSource() == 0) {
            change(executionContextTool, value);
        } else {
            Stream.iterate(0, n -> n + 1)
                    .limit(executionContextTool.countSource())
                    .flatMap(i -> executionContextTool.getMessages(i).stream())
                    .flatMap(a -> a.getMessages().stream())
                    .forEach(m -> {
                        change(executionContextTool, m.getValue().toString());
                    });
        }
    }

    private void change(ExecutionContextTool executionContextTool, String value) {
        List<Object> result = new LinkedList<>();
        for (int i = 0; i < executionContextTool.getConfigurationControlTool().countManagedConfigurations(); i++) {
            int j = i;
            executionContextTool.getConfigurationControlTool().getManagedConfiguration(i).ifPresent(c -> {
                c.getSetting(key).ifPresent(v -> {
                    result.add(String.format("%d: %s, key: %s", j, c.getName(), key));
                    Object oldValue = v.getValue();
                    result.add(oldValue);
                    Object newValue = getValue(oldValue, value);
                    if (newValue instanceof Byte) {
                        c.setSetting(key, (Byte) newValue);
                    } else if (newValue instanceof Short) {
                        c.setSetting(key, (Short) newValue);
                    } else if (newValue instanceof Integer) {
                        c.setSetting(key, (Integer) newValue);
                    } else if (newValue instanceof Long) {
                        c.setSetting(key, (Long) newValue);
                    } else if (newValue instanceof BigInteger) {
                        c.setSetting(key, (BigInteger) newValue);
                    } else if (newValue instanceof Float) {
                        c.setSetting(key, (Float) newValue);
                    } else if (newValue instanceof Double) {
                        c.setSetting(key, (Double) newValue);
                    } else if (newValue instanceof BigDecimal) {
                        c.setSetting(key, (BigDecimal) newValue);
                    } else if (newValue instanceof String) {
                        c.setSetting(key, (String) newValue);
                    } else if (newValue instanceof byte[]) {
                        c.setSetting(key, (byte[]) newValue);
                    } else if (newValue instanceof ObjectArray) {
                        c.setSetting(key, (ObjectArray) newValue);
                    }
                    result.add(newValue);
                });
            });
        }
        executionContextTool.addMessage(result);
    }

    private Object getValue(Object oldValue, String newValue) {
        Object result = newValue;
        if (oldValue instanceof Number) {
            if (oldValue instanceof Byte) {
                result = Byte.valueOf(newValue);
            } else if (oldValue instanceof Short) {
                result = Short.valueOf(newValue);
            } else if (oldValue instanceof Integer) {
                result = Integer.valueOf(newValue);
            } else if (oldValue instanceof Long) {
                result = Long.valueOf(newValue);
            } else if (oldValue instanceof BigInteger) {
                result = new BigInteger(newValue);
            } else if (oldValue instanceof Float) {
                result = Float.valueOf(newValue);
            } else if (oldValue instanceof Double) {
                result = Double.valueOf(newValue);
            } else if (oldValue instanceof BigDecimal) {
                result = new BigDecimal(newValue);
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
    public void stop(ConfigurationTool configurationTool) throws ModuleException {
        // this.names = null;
        key = null;
        value = null;
    }
}
