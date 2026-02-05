package ru.smcsystem.modules.module;

import ru.smcsystem.api.dto.IMessage;
import ru.smcsystem.api.dto.IValue;
import ru.smcsystem.api.dto.ObjectArray;
import ru.smcsystem.api.dto.ObjectElement;
import ru.smcsystem.api.exceptions.ModuleException;
import ru.smcsystem.api.module.Module;
import ru.smcsystem.api.tools.ConfigurationTool;
import ru.smcsystem.api.tools.execution.ExecutionContextTool;
import ru.smcsystem.smc.utils.ModuleUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LogicGui implements Module {
    private Operation operation;

    @Override
    public void start(ConfigurationTool configurationTool) throws ModuleException {
        String shapeId = configurationTool.getSetting("shapeId").map(ModuleUtils::toString).orElseThrow(() -> new ModuleException("shapeId setting"));
        Shape shape = null;
        List<Shape> shapes = null;
        if (!shapeId.isBlank()) {
            // configurationTool.loggerTrace("start shape generator");
            /*
            shapes = shapes = configurationTool.getInfo("decorationShapes").map(ModuleUtils::getObjectArray)
                    .filter(ModuleUtils::isArrayContainObjectElements)
                    .map(a -> ModuleUtils.convertFromObjectArray(a, Shape.class, true, true))
                    .orElse(List.of());
            */
            shapes = ModuleUtils.convertFromObjectArray(configurationTool.getContainer().getDecorationShapes(), Shape.class, true, true);
            shape = shapes.stream().filter(s -> Objects.equals(s.getName(), shapeId)).findFirst().orElse(null);
        }
        if (shape == null)
            throw new ModuleException("shape not found");
        operation = new Operation(configurationTool, shape, shapes, new LinkedList<>());
        ObjectElement resultConfig = operation.toObject();
        configurationTool.loggerTrace(resultConfig.toString());
        configurationTool.setVariable("config", new ObjectArray(resultConfig));
    }

    @Override
    public void update(ConfigurationTool configurationTool) throws ModuleException {
        stop(configurationTool);
        start(configurationTool);
    }

    @Override
    public void process(ConfigurationTool configurationTool, ExecutionContextTool executionContextTool) throws ModuleException {
        ModuleUtils.processMessagesAll(configurationTool, executionContextTool, (id, messages) -> {
            OperationType operationType = OperationType.valueOf(executionContextTool.getType().toUpperCase());
            switch (operationType) {
                case EXECUTE:
                    execute(configurationTool, executionContextTool, messages);
                    break;
            }
        });
    }

    @Override
    public void stop(ConfigurationTool configurationTool) throws ModuleException {
        operation = null;
    }

    private void execute(ConfigurationTool configurationTool, ExecutionContextTool executionContextTool, List<LinkedList<IMessage>> messageList) {
        Map<String, List<Object>> map = new HashMap<>();
        Stream.iterate(0, i -> i + 1)
                .limit(messageList.size())
                .filter(n -> !messageList.get(n).isEmpty())
                .map(n -> Map.entry(n, messageList.get(n).stream().map(IValue::getValue).collect(Collectors.toList())))
                .forEach(e -> map.put(Operation.VAR_NAME_SOURCE + e.getKey(), e.getValue()));
        map.put(Operation.VAR_NAME_RESULT, new ArrayList<>());
        operation.execute(configurationTool, executionContextTool, map, null);
        List<Object> resultLst = map.get(Operation.VAR_NAME_RESULT);
        if (!resultLst.isEmpty())
            executionContextTool.addMessage(resultLst);
    }

    private enum OperationType {
        EXECUTE
    }

}
