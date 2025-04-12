package ru.smcsystem.smcmodules.module;

import ru.smcsystem.api.dto.IMessage;
import ru.smcsystem.api.dto.IValue;
import ru.smcsystem.api.dto.ObjectArray;
import ru.smcsystem.api.dto.ObjectElement;
import ru.smcsystem.api.enumeration.ObjectType;
import ru.smcsystem.api.exceptions.ModuleException;
import ru.smcsystem.api.module.Module;
import ru.smcsystem.api.tools.ConfigurationTool;
import ru.smcsystem.api.tools.execution.ExecutionContextTool;
import ru.smcsystem.smc.utils.ModuleUtils;

import java.util.*;
import java.util.stream.Collectors;

public class Iterator implements Module {

    private enum UpdateCacheStrategy {
        FORCE, ONY_EXIST, ADD, ADD_CLEAN_ON_GET
    }

    private enum OperationType {
        DEFAULT, ADD, GET, GET_ALL, CLEAN, UPDATE
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
        OperationType operationType = OperationType.valueOf(executionContextTool.getType().toUpperCase());
        if (operationType == OperationType.DEFAULT) {
            if (executionContextTool.countSource() > 0) {
                operationType = OperationType.ADD;
            } else if (!cache.isEmpty() && currentStartPosition < cache.size()) {
                operationType = OperationType.GET;
            }
        }
        OperationType operationTypeResult = operationType;
        ModuleUtils.processMessagesAll(configurationTool, executionContextTool, (id, messageList) -> {
            switch (operationTypeResult) {
                case ADD:
                    add(executionContextTool, messageList);
                    break;
                case GET:
                    get(executionContextTool);
                    break;
                case GET_ALL:
                    executionContextTool.addMessage(cache);
                    break;
                case CLEAN:
                    cache.clear();
                    currentStartPosition = 0;
                    break;
                case UPDATE:
                    update(configurationTool, executionContextTool, messageList);
                    break;
                default:
                    break;
            }
        });
    }

    private void update(ConfigurationTool configurationTool, ExecutionContextTool executionContextTool, List<LinkedList<IMessage>> messageList) throws ModuleException {
        if (messageList.isEmpty())
            return;

        ObjectArray objectArray = ModuleUtils.getObjectArray(messageList.get(0).poll());
        if (!ModuleUtils.isArrayContainObjectElements(objectArray)) {
            executionContextTool.addError("need object");
            return;
        }

        ObjectElement objectElement = (ObjectElement) objectArray.get(0);
        int maxSize = objectElement.findField("maxSize").map(ModuleUtils::toNumber).map(Number::intValue).orElse(20);
        int updateSize = objectElement.findField("updateSize").map(ModuleUtils::toNumber).map(Number::intValue).orElse(10);
        if (maxSize < updateSize || updateSize <= 0) {
            executionContextTool.addError("wrong maxSize and updateSize");
            return;
        }

        List<Object> startValues = objectElement.findField("startValues")
                .map(ModuleUtils::getObjectArray)
                .map(a -> {
                    if (a.getType() == ObjectType.OBJECT_ELEMENT) {
                        return List.of((Object) a);
                    } else {
                        List<Object> lst = new ArrayList<>(a.size() + 1);
                        for (int i = 0; i < a.size(); i++)
                            lst.add(a.get(i));
                        return lst;
                    }
                })
                .orElse(null);
        List<Object> endValues = objectElement.findField("endValues")
                .map(ModuleUtils::getObjectArray)
                .map(a -> {
                    if (a.getType() == ObjectType.OBJECT_ELEMENT) {
                        return List.of((Object) a);
                    } else {
                        List<Object> lst = new ArrayList<>(a.size() + 1);
                        for (int i = 0; i < a.size(); i++)
                            lst.add(a.get(i));
                        return lst;
                    }
                })
                .orElse(null);

        if (cache.size() > maxSize) {
            cache = new LinkedList<>(cache.subList(cache.size() - updateSize, cache.size()));
            configurationTool.loggerTrace("cut off begin");
        }
        if (startValues != null && !startValues.isEmpty() && (cache.size() < startValues.size() || !cache.subList(0, startValues.size()).equals(startValues))) {
            cache.addAll(0, startValues);
            configurationTool.loggerTrace("add start values");
        }
        if (endValues != null && !endValues.isEmpty() && (cache.size() < endValues.size() || !cache.subList(cache.size() - endValues.size(), cache.size()).equals(endValues))) {
            cache.addAll(0, endValues);
            configurationTool.loggerTrace("add end values");
        }
        executionContextTool.addMessage(cache.size());
    }

    private void add(ExecutionContextTool executionContextTool, List<LinkedList<IMessage>> messageList) {
        List<Object> newValues = messageList.stream()
                .flatMap(Collection::stream)
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
    }

    private void get(ExecutionContextTool executionContextTool) throws ModuleException {
        int startPosition = currentStartPosition;
        int endPosition = startPosition + outputSize;
        if (endPosition > cache.size())
            endPosition = cache.size();
        if (outputSize < 0)
            endPosition = cache.size();

        List<Object> list = cache.subList(startPosition, endPosition);

        list.forEach(v -> {
            try {
                executionContextTool.addMessage(List.of(v));
            } catch (Exception e) {
                // executionContextTool.addError(StringUtils.defaultIfBlank(e.getMessage(), e.getCause() != null ? e.getCause().getMessage() : "NULL"));
            }
        });
        currentStartPosition += incrementValue;

        if (updateCacheStrategy.equals(UpdateCacheStrategy.ADD_CLEAN_ON_GET)) {
            cache.clear();
            currentStartPosition = 0;
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
