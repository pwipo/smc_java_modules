package ru.smcsystem.modules.configurationControllerPrintSettings;

import ru.smcsystem.api.dto.IConfigurationManaged;
import ru.smcsystem.api.dto.IMessage;
import ru.smcsystem.api.exceptions.ModuleException;
import ru.smcsystem.api.module.Module;
import ru.smcsystem.api.tools.ConfigurationTool;
import ru.smcsystem.api.tools.execution.ExecutionContextTool;
import ru.smcsystem.smc.utils.ModuleUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class PrintSettings implements Module {

    @Override
    public void process(ConfigurationTool configurationTool, ExecutionContextTool executionContextTool) throws ModuleException {
        ModuleUtils.processMessagesAll(configurationTool, executionContextTool, (id, messages) -> {
            OperationType operationType = OperationType.valueOf(executionContextTool.getType().toUpperCase());
            switch (operationType) {
                case DEFAULT:
                    execute(configurationTool, executionContextTool, messages);
                    break;
                case SETTING:
                    setting(configurationTool, executionContextTool, messages);
                    break;
                case VARIABLE:
                    variable(configurationTool, executionContextTool, messages);
                    break;
            }
        });
    }

    @Override
    public void start(ConfigurationTool configurationTool) throws ModuleException {

    }

    @Override
    public void update(ConfigurationTool configurationTool) throws ModuleException {

    }

    @Override
    public void stop(ConfigurationTool configurationTool) throws ModuleException {
    }

    private void execute(ConfigurationTool configurationTool, ExecutionContextTool executionContextTool, List<LinkedList<IMessage>> messages) {
        // for (String name : configurationTools.getNames()) {
        List<Object> result = new LinkedList<>();
        result.add(executionContextTool.getConfigurationControlTool().countManagedConfigurations());
        for (int i = 0; i < executionContextTool.getConfigurationControlTool().countManagedConfigurations(); i++) {
            IConfigurationManaged configurationManaged = executionContextTool.getConfigurationControlTool().getManagedConfiguration(i).get();
            result.add(configurationManaged.getName());
            result.add(configurationManaged.getAllSettings().size());
            // configurationTools.getAllSettings(name).forEach((key, value) -> {
            configurationManaged.getAllSettings().forEach((key, value) -> {
                result.add(key);
                result.add(value.getValue());
            });
        }
        executionContextTool.addMessage(result);
    }

    private void setting(ConfigurationTool configurationTool, ExecutionContextTool executionContextTool, List<LinkedList<IMessage>> messages) {
        Set<String> names = messages.get(0).stream().map(ModuleUtils::getString).filter(Objects::nonNull).collect(Collectors.toSet());
        List<Object> result = new LinkedList<>();
        for (int i = 0; i < executionContextTool.getConfigurationControlTool().countManagedConfigurations(); i++) {
            executionContextTool.getConfigurationControlTool().getManagedConfiguration(i).ifPresent(configurationManaged -> {
                configurationManaged.getAllSettings().forEach((key, value) -> {
                    if (names.contains(key))
                        result.add(value.getValue());
                });
            });
        }
        executionContextTool.addMessage(result);
    }

    private void variable(ConfigurationTool configurationTool, ExecutionContextTool executionContextTool, List<LinkedList<IMessage>> messages) {
        Set<String> names = messages.get(0).stream().map(ModuleUtils::getString).filter(Objects::nonNull).collect(Collectors.toSet());
        List<Object> result = new LinkedList<>();
        for (int i = 0; i < executionContextTool.getConfigurationControlTool().countManagedConfigurations(); i++) {
            executionContextTool.getConfigurationControlTool().getManagedConfiguration(i).ifPresent(configurationManaged -> {
                configurationManaged.getAllVariables().forEach((key, value) -> {
                    if (names.contains(key))
                        result.add(value.getValue());
                });
            });
        }
        executionContextTool.addMessage(result);
    }

    private enum OperationType {
        DEFAULT, SETTING, VARIABLE
    }

}
