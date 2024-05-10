package ru.smcsystem.modules.module;

import ru.smcsystem.api.dto.*;
import ru.smcsystem.api.enumeration.CommandType;
import ru.smcsystem.api.enumeration.SourceGetType;
import ru.smcsystem.api.exceptions.ModuleException;
import ru.smcsystem.api.module.Module;
import ru.smcsystem.api.tools.ConfigurationTool;
import ru.smcsystem.api.tools.FileTool;
import ru.smcsystem.api.tools.execution.ExecutionContextTool;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Example implements Module {

    private int counter;
    private String value;
    private String param;
    private String fileTextValue;

    private Random random;

    @Override
    public void start(ConfigurationTool configurationTool) throws ModuleException {
        // get settings
        value = (String) configurationTool.getSetting("value").orElseThrow(() -> new ModuleException("value setting not exist")).getValue();
        param = (String) configurationTool.getSetting("param").orElseThrow(() -> new ModuleException("param setting not exist")).getValue();
        counter = 1;

        // reed file from home folder
        fileTextValue = null;
        var fileToolRoot = configurationTool.getHomeFolder();
        var arrChildren = fileToolRoot.getChildrens();
        for (FileTool fileToolElement : arrChildren) {
            if (fileToolElement.getName().equals("text.txt")) {
                try (InputStream inputStream = fileToolElement.getInputStream();
                     InputStreamReader isr = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                     BufferedReader br = new BufferedReader(isr)) {

                    fileTextValue = br
                            .lines()
                            .collect(Collectors.joining("\n"));
                } catch (Exception e) {
                    throw new ModuleException("error", e);
                }
                break;
            }
        }
        if (fileTextValue == null)
            throw new ModuleException("file text.txt not exist");

        random = new Random();
    }

    @Override
    public void update(ConfigurationTool configurationTool) throws ModuleException {
        stop(configurationTool);
        start(configurationTool);
    }

    @Override
    public void process(ConfigurationTool configurationTool, ExecutionContextTool executionContextTool) throws ModuleException {
        // send messages
        executionContextTool.addMessage(counter++);
        executionContextTool.addMessage(value);
        executionContextTool.addMessage(fileTextValue);

        // read input messages
        executionContextTool.addMessage(
                Stream.iterate(0, n -> n + 1)
                        .limit(executionContextTool.countSource())
                        .flatMap(i -> executionContextTool.getMessages(i).stream())
                        .flatMap(a -> a.getMessages().stream())
                        .map(IValue::getValue)
                        .collect(Collectors.toList()));

        // execute all execution contexts and result messages send as own message
        Stream.iterate(0, n -> n + 1)
                .limit(executionContextTool.getFlowControlTool().countManagedExecutionContexts())
                .forEach(n -> {
                    executionContextTool.getFlowControlTool().executeNow(CommandType.EXECUTE, n, List.of(param));
                    executionContextTool.addMessage(
                            executionContextTool.getFlowControlTool().getMessagesFromExecuted(n).stream()
                                    .flatMap(a -> a.getMessages().stream())
                                    .map(IValue::getValue)
                                    .collect(Collectors.toList()));
                });

        // read managed configurations
        Stream.iterate(0, n -> n + 1)
                .limit(executionContextTool.getConfigurationControlTool().countManagedConfigurations())
                .forEach(n -> executionContextTool.addMessage(executionContextTool.getConfigurationControlTool().getManagedConfiguration(n).map(IConfiguration::getName).get()));

        // create new random configuration
        List<IModule> modules = executionContextTool.getConfigurationControlTool().getModules();
        IModule module = modules.get(random.nextInt(modules.size()));
        IConfigurationManaged configuration = executionContextTool.getConfigurationControlTool().createConfiguration(
                executionContextTool.getConfigurationControlTool().countManagedConfigurations()
                , configurationTool.getContainer()
                , module
                , String.format("cfg-%d", random.nextInt(Integer.MAX_VALUE)));
        executionContextTool.addMessage(String.format("created cfg %s", configuration.getName()));
        if (executionContextTool.getConfigurationControlTool().countManagedConfigurations() > 1) {
            IConfigurationManaged configurationManaged = executionContextTool.getConfigurationControlTool().getManagedConfiguration(0).get();
            if (configurationManaged.countExecutionContexts() > 0) {
                configurationManaged.getExecutionContext(0).ifPresent(ec -> {
                    IModule moduleMain = configurationManaged.getModule();
                    // add first execution context of created configuration to execution context list of first execution context first managed configuration
                    if ((moduleMain.getMinCountExecutionContexts(0) <= ec.countExecutionContexts() + 1) && (moduleMain.getMaxCountExecutionContexts(0) == -1 || moduleMain.getMaxCountExecutionContexts(0) > ec.countExecutionContexts())) {
                        IExecutionContextManaged iExecutionContextManaged = configuration.createExecutionContext("default", configurationManaged.getModule().getTypeName(0), -1);
                        // IExecutionContextManaged iExecutionContextManaged = configuration.getExecutionContext(0).get();
                        ec.insertExecutionContext(ec.countExecutionContexts(), iExecutionContextManaged);
                        executionContextTool.addMessage(String.format("add %s.%s to %s.%s", configuration.getName(), iExecutionContextManaged.getName(), configurationManaged.getName(), ec.getName()));
                    }
                    // add created configuration to managed configuration list of first execution context first managed configuration
                    if ((moduleMain.getMinCountManagedConfigurations(0) <= ec.countManagedConfigurations() + 1) && (moduleMain.getMaxCountManagedConfigurations(0) == -1 || moduleMain.getMaxCountManagedConfigurations(0) > ec.countManagedConfigurations())) {
                        ec.insertManagedConfiguration(ec.countManagedConfigurations(), configuration);
                        executionContextTool.addMessage(String.format("add %s to %s.%s", configuration.getName(), configurationManaged.getName(), ec.getName()));
                    }
                    // add first execution context of created configuration as source to first execution context first managed configuration
                    if ((moduleMain.getMinCountSources(0) <= ec.countSource() + 1) && (moduleMain.getMaxCountSources(0) == -1 || moduleMain.getMaxCountSources(0) > ec.countSource())) {
                        IExecutionContextManaged iExecutionContextManaged = configuration.getExecutionContext(0).get();
                        ISourceManaged sourceManaged = ec.createSource(iExecutionContextManaged, SourceGetType.NEW, 0, false);
                        sourceManaged.createFilter(false, List.of(0, -1), 0, 0, 0);
                        executionContextTool.addMessage(String.format("add %s.%s to %s.%s as source", configuration.getName(), iExecutionContextManaged.getName(), configurationManaged.getName(), ec.getName()));
                    }
                });
            }
        }
    }

    @Override
    public void stop(ConfigurationTool configurationTool) throws ModuleException {
        value = null;
        param = null;
        counter = 0;
        fileTextValue = null;
        random = null;
    }

}
