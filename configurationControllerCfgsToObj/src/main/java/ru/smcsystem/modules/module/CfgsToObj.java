package ru.smcsystem.modules.module;

import org.apache.commons.lang3.StringUtils;
import ru.smcsystem.api.dto.*;
import ru.smcsystem.api.enumeration.ObjectType;
import ru.smcsystem.api.exceptions.ModuleException;
import ru.smcsystem.api.module.Module;
import ru.smcsystem.api.tools.ConfigurationTool;
import ru.smcsystem.api.tools.execution.ExecutionContextTool;
import ru.smcsystem.smc.utils.ModuleUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CfgsToObj implements Module {
    private List<Param> paramList;

    @Override
    public void start(ConfigurationTool configurationTool) throws ModuleException {
        String params = (String) configurationTool.getSetting("params").orElseThrow(() -> new ModuleException("params setting not found")).getValue();
        paramList = Arrays.stream(params.split("::"))
                .map(s -> s.trim().split(","))
                .filter(l -> l.length > 3)
                .map(l -> new Param(Integer.valueOf(l[0].trim()), Param.Type.values()[Integer.parseInt(l[1].trim())], l[2], l[3]))
                .collect(Collectors.toList());
    }

    @Override
    public void update(ConfigurationTool configurationTool) throws ModuleException {
        stop(configurationTool);
        start(configurationTool);
    }

    @Override
    public void process(ConfigurationTool configurationTool, ExecutionContextTool executionContextTool) throws ModuleException {
        int size = executionContextTool.getConfigurationControlTool().countManagedConfigurations();
        Stream.iterate(0, n -> n + 1)
                .limit(executionContextTool.countSource())
                .flatMap(i -> executionContextTool.getMessages(i).stream())
                .map(IAction::getMessages)
                .forEach(messagesList -> {
                    try {
                        LinkedList<IMessage> messages = new LinkedList<>(messagesList);
                        switch (ModuleUtils.getNumber(messages.poll()).intValue()) {
                            case 1:
                                //read all
                                Set<String> names = new HashSet<>();
                                executionContextTool.addMessage(
                                        new ObjectArray(List.of(
                                                new ObjectElement(paramList.stream()
                                                        .filter(p -> !names.contains(p.getPublicName()))
                                                        .map(p -> {
                                                            names.add(p.getPublicName());
                                                            Object value = "";
                                                            if (size <= p.getCfgId()) {
                                                                executionContextTool.addError(String.format("wrong cfg id %s %d", p.getPublicName(), p.getCfgId()));
                                                                return new ObjectField(p.getPublicName(), ModuleUtils.getObjectType(value), value);
                                                            }
                                                            IConfigurationManaged iConfigurationManaged = executionContextTool.getConfigurationControlTool().getManagedConfiguration(p.getCfgId()).get();
                                                            switch (p.getType()) {
                                                                case CFG:
                                                                    switch (p.getName()) {
                                                                        case "enable":
                                                                            value = String.valueOf(iConfigurationManaged.isEnable());
                                                                            break;
                                                                        case "bufferSize":
                                                                            value = iConfigurationManaged.getBufferSize();
                                                                            break;
                                                                        case "name":
                                                                            value = iConfigurationManaged.getName();
                                                                            break;
                                                                        case "active":
                                                                            value = String.valueOf(iConfigurationManaged.isActive());
                                                                            break;
                                                                        default:
                                                                            executionContextTool.addError(String.format("not find %s", p.getPublicName()));
                                                                            break;
                                                                    }
                                                                    break;
                                                                case PARAMETER:
                                                                    value = iConfigurationManaged.getSetting(p.getName()).get().getValue();
                                                                    break;
                                                                case VARIABLE:
                                                                    value = iConfigurationManaged.getVariable(p.getName()).get().getValue();
                                                                    break;
                                                                case CONTEXT: {
                                                                    IExecutionContextManaged executionContextManaged = null;
                                                                    if (p.getParam() instanceof Integer) {
                                                                        Integer ecId = (Integer) p.getParam();
                                                                        executionContextManaged = iConfigurationManaged.countExecutionContexts() > ecId ? iConfigurationManaged.getExecutionContext(ecId).orElse(null) : null;
                                                                    } else {
                                                                        String exName = (String) p.getParam();
                                                                        int countEC = iConfigurationManaged.countExecutionContexts();
                                                                        for (int i = 0; i < countEC; i++) {
                                                                            IExecutionContextManaged executionContext = iConfigurationManaged.getExecutionContext(i).orElse(null);
                                                                            if (executionContext != null && executionContext.getName().equalsIgnoreCase(exName)) {
                                                                                executionContextManaged = executionContext;
                                                                                break;
                                                                            }
                                                                        }
                                                                    }
                                                                    if (executionContextManaged != null) {
                                                                        switch (p.getName()) {
                                                                            case "enable":
                                                                                value = String.valueOf(executionContextManaged.isEnable());
                                                                                break;
                                                                            case "name":
                                                                                value = executionContextManaged.getName();
                                                                                break;
                                                                            case "maxWorkInterval":
                                                                                value = String.valueOf(executionContextManaged.getMaxWorkInterval());
                                                                                break;
                                                                            case "active":
                                                                                value = String.valueOf(executionContextManaged.isActive());
                                                                                break;
                                                                            default:
                                                                                executionContextTool.addError(String.format("not find %s", p.getPublicName()));
                                                                                break;
                                                                        }
                                                                    }
                                                                    break;
                                                                }

                                                            }
                                                            return new ObjectField(p.getPublicName(), ModuleUtils.getObjectType(value), value);
                                                        })
                                                        .collect(Collectors.toList()))), ObjectType.OBJECT_ELEMENT));
                                break;
                            case 2:
                                //change
                                ObjectArray objectArray = ModuleUtils.deserializeToObject(messages);
                                if (!ModuleUtils.isArrayContainObjectElements(objectArray))
                                    break;
                                ((ObjectElement) objectArray.get(0)).getFields().forEach(f -> paramList.stream()
                                        .filter(p -> p.getPublicName().equalsIgnoreCase(f.getName()))
                                        .filter(p -> size > p.getCfgId())
                                        .forEach(p -> {
                                            IConfigurationManaged iConfigurationManaged = executionContextTool.getConfigurationControlTool().getManagedConfiguration(p.getCfgId()).get();
                                            try {
                                                switch (p.getType()) {
                                                    case CFG:
                                                        switch (p.getName()) {
                                                            case "enable":
                                                                if (iConfigurationManaged.isEnable() != Boolean.parseBoolean(f.getValue().toString()))
                                                                    iConfigurationManaged.setEnable(Boolean.parseBoolean(f.getValue().toString()));
                                                                break;
                                                            case "bufferSize":
                                                                if (iConfigurationManaged.getBufferSize() != ModuleUtils.getNumber(f).longValue())
                                                                    iConfigurationManaged.setBufferSize(ModuleUtils.getNumber(f).longValue());
                                                                break;
                                                            case "name":
                                                                if (!StringUtils.equals(iConfigurationManaged.getName(), f.getValue().toString()))
                                                                    iConfigurationManaged.setName(f.getValue().toString());
                                                                break;
                                                        }
                                                        break;
                                                    case PARAMETER: {
                                                        Optional<IValue> setting = iConfigurationManaged.getSetting(p.getName());
                                                        if (setting.isPresent() && !Objects.equals(setting.get().getValue(), f.getValue())) {
                                                            if (ModuleUtils.isObjectElement(f)) {
                                                                iConfigurationManaged.setSetting(p.getName(), new ObjectArray(List.of(f.getValue()), ObjectType.OBJECT_ELEMENT));
                                                            } else {
                                                                switch (ModuleUtils.toValueType(f)) {
                                                                    case OBJECT_ARRAY:
                                                                        iConfigurationManaged.setSetting(p.getName(), (ObjectArray) f.getValue());
                                                                        break;
                                                                    case STRING:
                                                                        iConfigurationManaged.setSetting(p.getName(), (String) f.getValue());
                                                                        break;
                                                                    case BYTE:
                                                                        iConfigurationManaged.setSetting(p.getName(), (Byte) f.getValue());
                                                                        break;
                                                                    case SHORT:
                                                                        iConfigurationManaged.setSetting(p.getName(), (Short) f.getValue());
                                                                        break;
                                                                    case INTEGER:
                                                                        iConfigurationManaged.setSetting(p.getName(), (Integer) f.getValue());
                                                                        break;
                                                                    case LONG:
                                                                        iConfigurationManaged.setSetting(p.getName(), (Long) f.getValue());
                                                                        break;
                                                                    case FLOAT:
                                                                        iConfigurationManaged.setSetting(p.getName(), (Float) f.getValue());
                                                                        break;
                                                                    case DOUBLE:
                                                                        iConfigurationManaged.setSetting(p.getName(), (Double) f.getValue());
                                                                        break;
                                                                    case BIG_INTEGER:
                                                                        iConfigurationManaged.setSetting(p.getName(), (BigInteger) f.getValue());
                                                                        break;
                                                                    case BIG_DECIMAL:
                                                                        iConfigurationManaged.setSetting(p.getName(), (BigDecimal) f.getValue());
                                                                        break;
                                                                    case BYTES:
                                                                        iConfigurationManaged.setSetting(p.getName(), (byte[]) f.getValue());
                                                                        break;

                                                                }
                                                            }
                                                        }
                                                        break;
                                                    }
                                                    case VARIABLE: {
                                                        Optional<IValue> variable = iConfigurationManaged.getVariable(p.getName());
                                                        if (variable.isEmpty() || !Objects.equals(variable.get().getValue(), f.getValue())) {
                                                            if (ModuleUtils.isObjectElement(f)) {
                                                                iConfigurationManaged.setSetting(p.getName(), new ObjectArray(List.of(f.getValue()), ObjectType.OBJECT_ELEMENT));
                                                            } else {
                                                                switch (ModuleUtils.toValueType(f)) {
                                                                    case OBJECT_ARRAY:
                                                                        iConfigurationManaged.setVariable(p.getName(), (ObjectArray) f.getValue());
                                                                        break;
                                                                    case STRING:
                                                                        iConfigurationManaged.setVariable(p.getName(), (String) f.getValue());
                                                                        break;
                                                                    case BYTE:
                                                                        iConfigurationManaged.setVariable(p.getName(), (Byte) f.getValue());
                                                                        break;
                                                                    case SHORT:
                                                                        iConfigurationManaged.setVariable(p.getName(), (Short) f.getValue());
                                                                        break;
                                                                    case INTEGER:
                                                                        iConfigurationManaged.setVariable(p.getName(), (Integer) f.getValue());
                                                                        break;
                                                                    case LONG:
                                                                        iConfigurationManaged.setVariable(p.getName(), (Long) f.getValue());
                                                                        break;
                                                                    case FLOAT:
                                                                        iConfigurationManaged.setVariable(p.getName(), (Float) f.getValue());
                                                                        break;
                                                                    case DOUBLE:
                                                                        iConfigurationManaged.setVariable(p.getName(), (Double) f.getValue());
                                                                        break;
                                                                    case BIG_INTEGER:
                                                                        iConfigurationManaged.setVariable(p.getName(), (BigInteger) f.getValue());
                                                                        break;
                                                                    case BIG_DECIMAL:
                                                                        iConfigurationManaged.setVariable(p.getName(), (BigDecimal) f.getValue());
                                                                        break;
                                                                    case BYTES:
                                                                        iConfigurationManaged.setVariable(p.getName(), (byte[]) f.getValue());
                                                                        break;
                                                                }
                                                            }
                                                        }
                                                        break;
                                                    }
                                                    case CONTEXT: {
                                                        IExecutionContextManaged executionContextManaged = null;
                                                        if (p.getParam() instanceof Integer) {
                                                            Integer ecId = (Integer) p.getParam();
                                                            executionContextManaged = iConfigurationManaged.countExecutionContexts() > ecId ? iConfigurationManaged.getExecutionContext(ecId).orElse(null) : null;
                                                        } else {
                                                            String exName = (String) p.getParam();
                                                            int countEC = iConfigurationManaged.countExecutionContexts();
                                                            for (int i = 0; i < countEC; i++) {
                                                                IExecutionContextManaged executionContext = iConfigurationManaged.getExecutionContext(i).orElse(null);
                                                                if (executionContext != null && executionContext.getName().equalsIgnoreCase(exName)) {
                                                                    executionContextManaged = executionContext;
                                                                    break;
                                                                }
                                                            }
                                                        }
                                                        if (executionContextManaged != null) {
                                                            switch (p.getName()) {
                                                                case "enable":
                                                                    if (executionContextManaged.isEnable() != Boolean.parseBoolean(f.getValue().toString()))
                                                                        executionContextManaged.setEnable(Boolean.parseBoolean(f.getValue().toString()));
                                                                    break;
                                                                case "name":
                                                                    if (!StringUtils.equals(executionContextManaged.getName(), f.getValue().toString()))
                                                                        executionContextManaged.setName(f.getValue().toString());
                                                                    break;
                                                                case "maxWorkInterval":
                                                                    if (executionContextManaged.getMaxWorkInterval() != ModuleUtils.getNumber(f).intValue())
                                                                        executionContextManaged.setMaxWorkInterval(ModuleUtils.getNumber(f).intValue());
                                                                    break;
                                                            }
                                                        }
                                                        break;
                                                    }
                                                }
                                            } catch (Exception e) {
                                                executionContextTool.addError(ModuleUtils.getErrorMessageOrClassName(e));
                                                configurationTool.loggerWarn(ModuleUtils.getStackTraceAsString(e));
                                            }
                                        }));
                                break;
                        }
                    } catch (Exception e) {
                        executionContextTool.addError(ModuleUtils.getErrorMessageOrClassName(e));
                        configurationTool.loggerWarn(ModuleUtils.getStackTraceAsString(e));
                    }
                });
    }

    @Override
    public void stop(ConfigurationTool configurationTool) throws ModuleException {
        paramList = null;
    }

}
