package ru.smcsystem.modules.module;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import ru.smcsystem.api.exceptions.ModuleException;
import ru.smcsystem.api.module.Module;
import ru.smcsystem.api.tools.ConfigurationTool;
import ru.smcsystem.api.tools.execution.ExecutionContextTool;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ValueTransformer implements Module {
    private static final Pattern base64 = Pattern.compile("^([A-Za-z0-9+\\/]{4})*([A-Za-z0-9+\\/]{4}|[A-Za-z0-9+\\/]{3}=|[A-Za-z0-9+\\/]{2}==)$");
    private List<Entity> entities;

    @Override
    public void start(ConfigurationTool configurationTool) throws ModuleException {
        String configMap = (String) configurationTool.getSetting("config").orElseThrow(() -> new ModuleException("config setting")).getValue();

        entities = Arrays.stream(configMap.stripTrailing().split("\\R"))
                .map(s -> s.split("::"))
                .filter(arr -> arr.length >= 2)
                .map(arr -> {
                    Pattern pattern = null;
                    byte[] patternBytes = null;
                    Number patternNumber = null;
                    List<Object> resultLst = new ArrayList<>();
                    if (!arr[0].isEmpty()) {
                        Object obj = autoConvert(arr[0]);
                        if (obj instanceof byte[]) {
                            patternBytes = (byte[]) obj;
                        } else if (obj instanceof Number) {
                            patternNumber = (Number) obj;
                        } else {
                            pattern = Pattern.compile(obj.toString());
                        }
                    }
                    for (int i = 1; i < arr.length; i++)
                        resultLst.add(autoConvert(arr[i]));
                    return new Entity(patternNumber, pattern, patternBytes, resultLst);
                }).collect(Collectors.toList());
    }

    @Override
    public void update(ConfigurationTool configurationTool) throws ModuleException {
        stop(configurationTool);
        start(configurationTool);
    }

    @Override
    public void process(ConfigurationTool configurationTool, ExecutionContextTool executionContextTool) throws ModuleException {
        executionContextTool.addMessage(
                Stream.iterate(0, n -> n + 1)
                        .limit(executionContextTool.countSource())
                        .flatMap(i -> executionContextTool.getMessages(i).stream())
                        .flatMap(a -> a.getMessages().stream())
                        .map(m -> entities.stream()
                                .filter(e -> e.match(m))
                                .findFirst()
                                .or(() -> Optional.of(new Entity(null, null, null, List.of(m.getValue()))))
                                .get())
                        .flatMap(e -> e.getResultLst().stream())
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()));
    }

    @Override
    public void stop(ConfigurationTool configurationTool) throws ModuleException {
        entities = null;
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
                result = value;
            }
        }
        return result;
    }

}
