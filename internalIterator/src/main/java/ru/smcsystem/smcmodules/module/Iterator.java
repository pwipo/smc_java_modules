package ru.smcsystem.smcmodules.module;

import ru.smcsystem.api.dto.IValue;
import ru.smcsystem.api.exceptions.ModuleException;
import ru.smcsystem.api.module.Module;
import ru.smcsystem.api.tools.ConfigurationTool;
import ru.smcsystem.api.tools.execution.ExecutionContextTool;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Iterator implements Module {

    enum UpdateCacheStrategy {
        FORCE,
        ONY_EXIST,
        ADD,
        ADD_CLEAN_ON_GET
    }

    private UpdateCacheStrategy updateCacheStrategy;
    private int maxCacheSize;
    private int incrementValue;
    private int outputSize;

    private List<Object> cache;
    private int currentStartPosition;

    @Override
    public void start(ConfigurationTool configurationTool) throws ModuleException {
        updateCacheStrategy = UpdateCacheStrategy.valueOf((String) configurationTool.getSetting("updateCacheStrategy").orElseThrow(() -> new ModuleException("updateCacheStrategy setting")).getValue());
        maxCacheSize = (Integer) configurationTool.getSetting("maxCacheSize").orElseThrow(() -> new ModuleException("maxCacheSize setting")).getValue();
        incrementValue = (Integer) configurationTool.getSetting("incrementValue").orElseThrow(() -> new ModuleException("incrementValue setting")).getValue();
        outputSize = (Integer) configurationTool.getSetting("outputSize").orElseThrow(() -> new ModuleException("outputSize setting")).getValue();

        cache = Collections.synchronizedList(new LinkedList<>());
        currentStartPosition = 0;
    }

    @Override
    public void update(ConfigurationTool configurationTool) throws ModuleException {
        stop(configurationTool);
        start(configurationTool);
    }

    @Override
    public void process(ConfigurationTool configurationTool, ExecutionContextTool executionContextTool) throws ModuleException {
        if (executionContextTool.countSource() > 0) {
            List<Object> newValues = Stream.iterate(0, n -> n + 1)
                    .limit(executionContextTool.countSource())
                    .flatMap(i -> executionContextTool.getMessages(i).stream())
                    .flatMap(action -> action.getMessages().stream())
                    .map(IValue::getValue)
                    .collect(Collectors.toList());
            if (newValues.stream().anyMatch(v -> v == null || (v instanceof byte[] && ((byte[]) v).length == 0))) {
                executionContextTool.addError("inputs has empty data");
                return;
            }

            switch (updateCacheStrategy) {
                case FORCE:
                    cache = new LinkedList<>(newValues);
                    break;
                case ONY_EXIST:
                    if (!newValues.isEmpty())
                        cache = new LinkedList<>(newValues);
                    break;
                case ADD:
                    cache.addAll(newValues);
                    break;
                case ADD_CLEAN_ON_GET:
                    cache.addAll(newValues);
                    break;
            }

            if (maxCacheSize > 0 && cache.size() > maxCacheSize)
                cache = new LinkedList<>(cache.subList(cache.size() - maxCacheSize, cache.size()));

            currentStartPosition = 0;
            executionContextTool.addMessage(cache.size());
        } else if (!cache.isEmpty() && currentStartPosition < cache.size()) {
            int startPosition = currentStartPosition;
            int endPosition = startPosition + outputSize;
            if (endPosition > cache.size())
                endPosition = cache.size();
            if (outputSize < 0)
                endPosition = cache.size();

            List<Object> list = cache.subList(startPosition, endPosition);
            /*
            if (outputSize > list.size()) {
                Object lastValue = list.get(list.size() - 1);
                Object newValue;
                if (lastValue instanceof String) {
                    newValue = " ";
                } else if (lastValue instanceof Number) {
                    newValue = 0;
                } else {
                    newValue = new byte[]{0};
                }
                for (int i = 0; i < list.size() - outputSize; i++) {
                    list.add(newValue);
                }
            }
            */

            list.forEach(v -> {
                try {
                    executionContextTool.addMessage(List.of(v));
                } catch (Exception e) {
                    // executionContextTool.addError(StringUtils.defaultIfBlank(e.getMessage(), e.getCause() != null ? e.getCause().getMessage() : "NULL"));
                }
            });
            currentStartPosition += incrementValue;

            if (updateCacheStrategy.equals(UpdateCacheStrategy.ADD_CLEAN_ON_GET))
                cache.clear();
        }
    }

    @Override
    public void stop(ConfigurationTool configurationTool) throws ModuleException {
        if (cache != null)
            cache.clear();
        cache = null;
        updateCacheStrategy = null;
        maxCacheSize = 0;
        incrementValue = 0;
        outputSize = 0;
    }

}
