package ru.smcsystem.modules.configurationControllerExecutionContextEnableToggle;

import ru.smcsystem.api.dto.IConfigurationManaged;
import ru.smcsystem.api.dto.IExecutionContextManaged;
import ru.smcsystem.api.exceptions.ModuleException;
import ru.smcsystem.api.module.Module;
import ru.smcsystem.api.tools.ConfigurationTool;
import ru.smcsystem.api.tools.execution.ExecutionContextTool;

public class ExecutionContextToggle implements Module {

    // private List<String[]> names;

    @Override
    public void process(ConfigurationTool configurationTools, ExecutionContextTool executionContextTool) throws ModuleException {
        // for (String[] name : names) {
        for (int i = 0; i < executionContextTool.getConfigurationControlTool().countManagedConfigurations(); i++) {
            IConfigurationManaged configurationManaged = executionContextTool.getConfigurationControlTool().getManagedConfiguration(i).get();
            for (int j = 0; j < configurationManaged.countExecutionContexts(); j++) {
                IExecutionContextManaged executionContextManaged = configurationManaged.getExecutionContext(j).get();
                Boolean executionContextEnable = executionContextManaged.isEnable();
                executionContextManaged.setEnable(!executionContextEnable);
                executionContextTool.addMessage(String.format("execution context %d %d %s", i, j, executionContextEnable.toString()));
            }
        }
    }

    @Override
    public void start(ConfigurationTool configurationTool) throws ModuleException {
        /*
        String strNames = (String) configurationTool.getSetting("names").orElseThrow(() -> new ModuleException("key setting not found")).getValue();
        this.names = new ArrayList<>();
        for (String name : strNames.split(",")) {
            String[] split = StringUtils.split(StringUtils.trim(name), ".");
            if (split == null || split.length != 2)
                throw new ModuleException("wrong name " + name);
            this.names.add(split);
        }
        */
    }

    @Override
    public void update(ConfigurationTool configurationTool) throws ModuleException {
        stop(configurationTool);
        start(configurationTool);
    }

    @Override
    public void stop(ConfigurationTool configurationTool) throws ModuleException {
        // this.names = null;
    }
}
