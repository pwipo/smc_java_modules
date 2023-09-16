package ru.smcsystem.modules.module;

import ru.smcsystem.api.exceptions.ModuleException;
import ru.smcsystem.api.module.Module;
import ru.smcsystem.api.tools.ConfigurationTool;
import ru.smcsystem.api.tools.execution.ExecutionContextTool;

import java.util.ArrayList;
import java.util.List;

public class Logic implements Module {

    private enum Type {
        AND,
        OR,
        NOT
    }

    private Type type;

    @Override
    public void start(ConfigurationTool configurationTool) throws ModuleException {
        type = Type.valueOf((String) configurationTool.getSetting("type").orElseThrow(() -> new ModuleException("type setting")).getValue());
    }

    @Override
    public void update(ConfigurationTool configurationTool) throws ModuleException {
        stop(configurationTool);
        start(configurationTool);
    }

    @Override
    public void process(ConfigurationTool configurationTool, ExecutionContextTool executionContextTool) throws ModuleException {
        if (executionContextTool.countSource() == 0) {
            executionContextTool.addError("need sources");
            return;
        }

        List<Boolean> sourcesHasData = new ArrayList<>(executionContextTool.countSource() + 1);
        for (int i = 0; i < executionContextTool.countSource(); i++) {
            sourcesHasData.add(executionContextTool.getMessages(i).stream()
                    .anyMatch(a -> !a.getMessages().isEmpty()));
        }
        switch (type) {
            case AND:
                if (sourcesHasData.stream().allMatch(b -> b))
                    executionContextTool.addMessage(1);
                break;
            case OR:
                if (sourcesHasData.stream().anyMatch(b -> b))
                    executionContextTool.addMessage(1);
                break;
            case NOT:
                if (!sourcesHasData.get(0))
                    executionContextTool.addMessage(1);
                break;
        }
    }

    @Override
    public void stop(ConfigurationTool configurationTool) throws ModuleException {
        type = null;
    }

}
