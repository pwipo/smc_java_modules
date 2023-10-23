package ru.smcsystem.smcmodules.module;

import ru.smcsystem.api.exceptions.ModuleException;
import ru.smcsystem.api.module.Module;
import ru.smcsystem.api.tools.ConfigurationTool;
import ru.smcsystem.api.tools.execution.ExecutionContextTool;
import ru.smcsystem.smc.utils.ModuleUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;

public class PrintDate implements Module {

    // private String format;
    private DateTimeFormatter dtf;
    private ZoneId zoneId;

    private enum Type {
        TO_STRING,
        TO_NUMBER,
        NUMBER_TO_XLS
    }

    private Type type;

    @Override
    public void start(ConfigurationTool configurationTool) throws ModuleException {
        String format = (String) configurationTool.getSetting("format").orElseThrow(() -> new ModuleException("format setting")).getValue();
        String local = (String) configurationTool.getSetting("local").orElseThrow(() -> new ModuleException("local setting")).getValue();
        zoneId = ZoneId.of((String) configurationTool.getSetting("zoneId").orElseThrow(() -> new ModuleException("zoneId setting")).getValue());
        dtf = DateTimeFormatter.ofPattern(format, new Locale(local)).withZone(zoneId);
        type = Type.valueOf((String) configurationTool.getSetting("type").orElseThrow(() -> new ModuleException("type setting")).getValue());
    }

    @Override
    public void update(ConfigurationTool configurationTool) throws ModuleException {
        stop(configurationTool);
        start(configurationTool);
    }

    @Override
    public void process(ConfigurationTool configurationTool, ExecutionContextTool executionContextTool) throws ModuleException {
        Type type = Objects.equals(executionContextTool.getType(), "default") ? this.type : Type.valueOf(executionContextTool.getType().toUpperCase());
        switch (type) {
            case TO_STRING:
                if (executionContextTool.countSource() > 0) {
                    Stream.iterate(0, n -> n + 1)
                            .limit(executionContextTool.countSource())
                            .flatMap(n -> executionContextTool.getMessages(n).stream())
                            .flatMap(a -> a.getMessages().stream())
                            .filter(ModuleUtils::isNumber)
                            .map(m -> ((Number) m.getValue()).longValue())
                            .forEach(v -> executionContextTool.addMessage(dtf.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(v), zoneId))));
                } else {
                    executionContextTool.addMessage(LocalDateTime.now(zoneId).format(dtf));
                }
                break;
            case TO_NUMBER:
                if (executionContextTool.countSource() > 0) {
                    Stream.iterate(0, n -> n + 1)
                            .limit(executionContextTool.countSource())
                            .flatMap(n -> executionContextTool.getMessages(n).stream())
                            .flatMap(a -> a.getMessages().stream())
                            .filter(ModuleUtils::isString)
                            .map(ModuleUtils::getString)
                            .forEach(v -> executionContextTool.addMessage(ZonedDateTime.from(dtf.parse(v)).toInstant().toEpochMilli()));
                } else {
                    executionContextTool.addMessage(Instant.now().toEpochMilli());
                }
                break;
            case NUMBER_TO_XLS:
                if (executionContextTool.countSource() > 0) {
                    Stream.iterate(0, n -> n + 1)
                            .limit(executionContextTool.countSource())
                            .flatMap(n -> executionContextTool.getMessages(n).stream())
                            .flatMap(a -> a.getMessages().stream())
                            .filter(ModuleUtils::isNumber)
                            .map(m -> ((Number) m.getValue()).longValue())
                            .forEach(v -> executionContextTool.addMessage((v / (1000.0 * 86400)) + 25569));
                } else {
                    executionContextTool.addMessage((Instant.now().toEpochMilli() / (1000.0 * 86400)) + 25569);
                }
                break;
        }
    }

    @Override
    public void stop(ConfigurationTool configurationTool) throws ModuleException {
        dtf = null;
        zoneId = null;
        type = null;
    }

}
