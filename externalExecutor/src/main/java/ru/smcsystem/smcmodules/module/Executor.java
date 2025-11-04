package ru.smcsystem.smcmodules.module;

import org.apache.commons.lang3.StringUtils;
import ru.smcsystem.api.dto.IMessage;
import ru.smcsystem.api.exceptions.ModuleException;
import ru.smcsystem.api.module.Module;
import ru.smcsystem.api.tools.ConfigurationTool;
import ru.smcsystem.api.tools.execution.ExecutionContextTool;
import ru.smcsystem.smc.utils.ModuleUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class Executor implements Module {
    private ProcessBuilder pb;
    private String encoding;
    private Integer maxWorkTime;
    private Integer sleepTimeInterval;

    @Override
    public void start(ConfigurationTool configurationTool) throws ModuleException {
        String commandPath = (String) configurationTool.getSetting("commandPath").orElseThrow(() -> new ModuleException("commandPath setting")).getValue();
        String workDirectory = (String) configurationTool.getSetting("workDirectory").orElseThrow(() -> new ModuleException("workDirectory setting")).getValue();
        String strArgs = (String) configurationTool.getSetting("args").orElseThrow(() -> new ModuleException("args setting")).getValue();
        encoding = (String) configurationTool.getSetting("encoding").orElseThrow(() -> new ModuleException("args encoding")).getValue();
        maxWorkTime = (Integer) configurationTool.getSetting("maxWorkTime").orElseThrow(() -> new ModuleException("maxWorkTime setting")).getValue();
        sleepTimeInterval = (Integer) configurationTool.getSetting("sleepTimeInterval").orElseThrow(() -> new ModuleException("sleepTimeInterval setting")).getValue();

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
        ModuleUtils.processMessagesAll(configurationTool, executionContextTool, (unused, messagesList) -> {
            Type type = Type.valueOf(executionContextTool.getType().toUpperCase());
            boolean needWait = type == Type.DEFAULT;
            if (!messagesList.isEmpty()) {
                for (LinkedList<IMessage> messages : messagesList) {
                    if (messages.isEmpty() || !ModuleUtils.isString(messages.peek()))
                        continue;
                    ProcessBuilder processBuilder = new ProcessBuilder(messages.stream()
                            .map(ModuleUtils::toString)
                            .collect(Collectors.toList()));
                    String command = ModuleUtils.toString(messages.poll());
                    if (command.contains("\\") || command.contains("/")) {
                        File file = new File(command);
                        if (file.exists() && file.isFile())
                            processBuilder.directory(file.getParentFile());
                    }
                    execute(configurationTool, executionContextTool, processBuilder, needWait);
                }
            } else {
                if (pb == null)
                    throw new ModuleException("commandPath not set");
                execute(configurationTool, executionContextTool, pb, needWait);
            }
        });
    }

    private void execute(ConfigurationTool configurationTool, ExecutionContextTool executionContextTool, ProcessBuilder processBuilder, boolean needWait) throws IOException {
        Process process = processBuilder.start();
        try {
            Integer exitValue = null;
            long startTime = System.currentTimeMillis();
            do {
                try {
                    Thread.sleep(sleepTimeInterval);
                } catch (Exception ignore) {
                }
                try {
                    exitValue = process.exitValue();
                } catch (Exception ignore) {
                }
            } while (needWait && exitValue == null && !executionContextTool.isNeedStop() &&
                    (maxWorkTime == -1 || maxWorkTime > (System.currentTimeMillis() - startTime)));
            if (needWait && process.isAlive())
                process.destroy();
            executionContextTool.addMessage(exitValue != null ? exitValue : -1);
        } catch (Exception e) {
            // throw new ModuleException("error", e);
            executionContextTool.addError(ModuleUtils.getErrorMessageOrClassName(e));
            configurationTool.loggerWarn(ModuleUtils.getStackTraceAsString(e));
            if (process.isAlive())
                process.destroy();
        }
        if (needWait) {
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
        }
    }

    @Override
    public void stop(ConfigurationTool configurationTool) throws ModuleException {
        pb = null;
        encoding = null;
        maxWorkTime = null;
    }

    enum Type {
        DEFAULT, LAUNCHER
    }
}
