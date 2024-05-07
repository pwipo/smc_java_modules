package ru.smcsystem.smcmodules.module;

import org.apache.commons.lang3.StringUtils;
import ru.smcsystem.api.dto.IAction;
import ru.smcsystem.api.enumeration.ValueType;
import ru.smcsystem.api.exceptions.ModuleException;
import ru.smcsystem.api.module.Module;
import ru.smcsystem.api.tools.ConfigurationTool;
import ru.smcsystem.api.tools.execution.ExecutionContextTool;
import ru.smcsystem.smc.utils.ModuleUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Executor implements Module {

    private ProcessBuilder pb;
    private String encoding;
    private Integer maxWorkTime;

    @Override
    public void start(ConfigurationTool configurationTool) throws ModuleException {
        String commandPath = (String) configurationTool.getSetting("commandPath").orElseThrow(() -> new ModuleException("commandPath setting")).getValue();
        String workDirectory = (String) configurationTool.getSetting("workDirectory").orElseThrow(() -> new ModuleException("workDirectory setting")).getValue();
        String strArgs = (String) configurationTool.getSetting("args").orElseThrow(() -> new ModuleException("args setting")).getValue();
        encoding = (String) configurationTool.getSetting("encoding").orElseThrow(() -> new ModuleException("args encoding")).getValue();
        maxWorkTime = (Integer) configurationTool.getSetting("maxWorkTime").orElseThrow(() -> new ModuleException("maxWorkTime setting")).getValue();

        pb = null;
        if (StringUtils.isNotBlank(commandPath)) {
            File exeFile = new File(commandPath);
            if (!exeFile.exists())
                throw new ModuleException("commandPath not exist");
            if (!exeFile.canExecute())
                throw new ModuleException("can't execute commandPath");

            List<String> commands = new LinkedList<>();
            commands.add(commandPath);

            if (StringUtils.isNotBlank(strArgs)) {
                String[] args = StringUtils.splitByWholeSeparator(strArgs, "::");
                commands.addAll(Arrays.asList(args));
            }

            pb = new ProcessBuilder(commands);

            if (StringUtils.isNotBlank(workDirectory))
                pb.directory(new File(workDirectory));
        }
    }

    @Override
    public void update(ConfigurationTool configurationTool) throws ModuleException {
        stop(configurationTool);
        start(configurationTool);
    }

    @Override
    public void process(ConfigurationTool configurationTool, ExecutionContextTool executionContextTool) throws ModuleException {
        if (executionContextTool.countSource() > 0) {
            Stream.iterate(0, n -> n + 1)
                    .limit(executionContextTool.countSource())
                    .flatMap(i -> executionContextTool.getMessages(i).stream())
                    .map(IAction::getMessages)
                    .filter(Objects::nonNull)
                    .filter(messages -> !messages.isEmpty())
                    .filter(messages -> ValueType.STRING.equals(messages.get(0).getType()))
                    .forEach(messages -> {
                        ProcessBuilder processBuilder = new ProcessBuilder(messages.stream()
                                .map(m -> m.getValue().toString())
                                .collect(Collectors.toList()));
                        String command = (String) messages.get(0).getValue();
                        if (command.contains("\\") || command.contains("/")) {
                            File file = new File(command);
                            if (file.exists() && file.isFile())
                                processBuilder.directory(file.getParentFile());
                        }
                        execute(configurationTool, executionContextTool, processBuilder);
                    });
        } else {
            if (pb == null)
                throw new ModuleException("commandPath not set");
            execute(configurationTool, executionContextTool, pb);
        }
    }

    private void execute(ConfigurationTool configurationTool, ExecutionContextTool executionContextTool, ProcessBuilder processBuilder) {
        Process process = null;
        try {
            process = processBuilder.start();

            Integer exitValue = null;
            long startTime = System.currentTimeMillis();
            do {
                try {
                    Thread.sleep(100);
                } catch (Exception ignore) {
                }
                try {
                    exitValue = process.exitValue();
                } catch (Exception ignore) {
                }
            } while (exitValue == null && !executionContextTool.isNeedStop() &&
                    (maxWorkTime == -1 || maxWorkTime > (System.currentTimeMillis() - startTime)));

            executionContextTool.addMessage(exitValue != null ? exitValue : -1);
        } catch (Exception e) {
            // throw new ModuleException("error", e);
            executionContextTool.addError(ModuleUtils.getErrorMessageOrClassName(e));
            configurationTool.loggerWarn(ModuleUtils.getStackTraceAsString(e));
            if (process != null && process.isAlive())
                process.destroy();
        }
        if (process != null) {
            try {
                try (InputStreamReader isr = new InputStreamReader(process.getInputStream(), encoding); BufferedReader br = new BufferedReader(isr)) {
                    String result = br.lines().collect(Collectors.joining("\n"));
                    if (StringUtils.isNotBlank(result))
                        executionContextTool.addMessage(result);
                }

                try (InputStreamReader isr = new InputStreamReader(process.getErrorStream(), encoding); BufferedReader br = new BufferedReader(isr)) {
                    String result = br.lines().collect(Collectors.joining("\n"));
                    if (StringUtils.isNotBlank(result))
                        executionContextTool.addError(result);
                }
            } catch (Exception e) {
                // throw new ModuleException("error", e);
                executionContextTool.addError(ModuleUtils.getErrorMessageOrClassName(e));
                configurationTool.loggerWarn(ModuleUtils.getStackTraceAsString(e));
            }
        }
    }

    @Override
    public void stop(ConfigurationTool configurationTool) throws ModuleException {
        pb = null;
        encoding = null;
        maxWorkTime = null;
    }

}
