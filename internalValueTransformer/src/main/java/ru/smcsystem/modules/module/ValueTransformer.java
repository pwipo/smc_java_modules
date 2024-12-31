package ru.smcsystem.modules.module;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import ru.smcsystem.api.dto.ObjectElement;
import ru.smcsystem.api.exceptions.ModuleException;
import ru.smcsystem.api.module.Module;
import ru.smcsystem.api.tools.ConfigurationTool;
import ru.smcsystem.api.tools.execution.ExecutionContextTool;
import ru.smcsystem.smc.utils.ModuleUtils;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ValueTransformer implements Module {
    private static final Pattern base64 = Pattern.compile("^([A-Za-z0-9+\\/]{4})*([A-Za-z0-9+\\/]{4}|[A-Za-z0-9+\\/]{3}=|[A-Za-z0-9+\\/]{2}==)$");
    private List<Entity> entities;

    private enum Type {
        DEFAULT, GET_PATTERNS, GET_VALUES_BY_ID
    }

    @Override
    public void start(ConfigurationTool configurationTool) throws ModuleException {
        String configMap = (String) configurationTool.getSetting("config").orElseThrow(() -> new ModuleException("config setting")).getValue();

        String[] strings = new String[1];
        List<Entity> entityList = configurationTool.getSetting("configObj")
                .map(ModuleUtils::getObjectArray)
                .filter(ModuleUtils::isArrayContainObjectElements)
                .stream()
                .flatMap(a -> {
                    List<Entity> lst = new ArrayList<>(a.size() + 1);
                    for (int i = 0; i < a.size(); i++) {
                        ObjectElement objectElement = (ObjectElement) a.get(i);
                        String pattern = objectElement.findFieldIgnoreCase("pattern").map(ModuleUtils::toString).orElse("");
                        String values = objectElement.findFieldIgnoreCase("values").map(ModuleUtils::toString).orElse("");
                        List<String> lstS = new ArrayList<>();
                        lstS.add(pattern);
                        lstS.addAll(Arrays.asList(values.split("::")));
                        lst.add(map(lstS.toArray(strings)));
                    }
                    return lst.stream();
                })
                .collect(Collectors.toList());

        entities = Stream.concat(
                        Arrays.stream(configMap.stripTrailing().split("\\R"))
                                .map(s -> s.split("::"))
                                .filter(arr -> arr.length >= 2)
                                .map(this::map),
                        entityList.stream())
                .collect(Collectors.toList());

        configurationTool.loggerDebug("count entities=" + entities.size());
        entities.forEach(e -> configurationTool.loggerDebug(String.format("pattern=%s, type=%s, values=%d", e.getOriginPattern(), e.getType(), e.getResultLst().size())));
    }

    private Entity map(String[] arr) {
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
        return new Entity(arr[0], patternNumber, pattern, patternBytes, resultLst);
    }

    @Override
    public void update(ConfigurationTool configurationTool) throws ModuleException {
        stop(configurationTool);
        start(configurationTool);
    }

    @Override
    public void process(ConfigurationTool configurationTool, ExecutionContextTool executionContextTool) throws ModuleException {
        Type type = Type.valueOf(executionContextTool.getType().toUpperCase().trim());
        ModuleUtils.processMessagesAll(configurationTool, executionContextTool, (id, messageList) -> {
            switch (type) {
                case DEFAULT:
                    executionContextTool.addMessage(
                            messageList.stream()
                                    .flatMap(Collection::stream)
                                    .map(m -> entities.stream()
                                            .filter(e -> e.match(m))
                                            .findFirst()
                                            .or(() -> Optional.of(new Entity("", null, null, null, List.of(m.getValue()))))
                                            .get())
                                    .flatMap(e -> e.getResultLst().stream())
                                    .filter(Objects::nonNull)
                                    .collect(Collectors.toList()));
                    break;
                case GET_PATTERNS:
                    entities.forEach(e -> executionContextTool.addMessage(e.getOriginPattern()));
                    break;
                case GET_VALUES_BY_ID:
                    executionContextTool.addMessage(entities.get(ModuleUtils.toNumber(messageList.get(0).poll()).intValue()).getResultLst());
                    break;
            }
        });
    }

    @Override
    public void stop(ConfigurationTool configurationTool) throws ModuleException {
        entities = null;
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

}
