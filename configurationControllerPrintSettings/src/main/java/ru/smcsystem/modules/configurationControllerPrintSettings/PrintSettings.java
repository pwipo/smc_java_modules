package ru.smcsystem.modules.configurationControllerPrintSettings;

import ru.smcsystem.api.dto.IConfigurationManaged;
import ru.smcsystem.api.exceptions.ModuleException;
import ru.smcsystem.api.module.Module;
import ru.smcsystem.api.tools.ConfigurationTool;
import ru.smcsystem.api.tools.execution.ExecutionContextTool;

import java.util.LinkedList;
import java.util.List;

public class PrintSettings implements Module {

    @Override
    public void process(ConfigurationTool configurationTools, ExecutionContextTool executionContextTool) throws ModuleException {
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

    @Override
    public void start(ConfigurationTool configurationTool) throws ModuleException {

    }

    @Override
    public void update(ConfigurationTool configurationTool) throws ModuleException {

    }

    @Override
    public void stop(ConfigurationTool configurationTool) throws ModuleException {

    }
}
