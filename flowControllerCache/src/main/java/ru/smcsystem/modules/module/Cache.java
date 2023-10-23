package ru.smcsystem.modules.module;

import com.google.common.cache.CacheBuilder;
import org.apache.commons.collections4.CollectionUtils;
import ru.smcsystem.api.dto.IAction;
import ru.smcsystem.api.dto.IMessage;
import ru.smcsystem.api.dto.IValue;
import ru.smcsystem.api.enumeration.CommandType;
import ru.smcsystem.api.enumeration.ValueType;
import ru.smcsystem.api.exceptions.ModuleException;
import ru.smcsystem.api.module.Module;
import ru.smcsystem.api.tools.ConfigurationTool;
import ru.smcsystem.api.tools.execution.ExecutionContextTool;
import ru.smcsystem.smc.utils.ModuleUtils;

import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Cache implements Module {

    private boolean cacheNull;
    private int maxValueSize;

    private com.google.common.cache.Cache<String, List<Object>> cache;

    private enum Type {
        size, clear_all, get, put, invalidate, get_all
    }

    @Override
    public void start(ConfigurationTool configurationTool) throws ModuleException {
        int cacheSize = (Integer) configurationTool.getSetting("cacheSize").orElseThrow(() -> new ModuleException("cacheSize setting not found")).getValue();
        int expireTime = (Integer) configurationTool.getSetting("expireTime").orElseThrow(() -> new ModuleException("expireTime setting not found")).getValue();
        cacheNull = Boolean.valueOf((String) configurationTool.getSetting("cacheNull").orElseThrow(() -> new ModuleException("cacheNull setting not found")).getValue());
        maxValueSize = (Integer) configurationTool.getSetting("maxValueSize").orElseThrow(() -> new ModuleException("maxValueSize setting not found")).getValue();

        CacheBuilder<Object, Object> cb = CacheBuilder.newBuilder();
        // cb.removalListener(MY_LISTENER)
        if (cacheSize > 0)
            cb.maximumSize(cacheSize);
        if (expireTime > 0)
            // cb.expireAfterWrite(expireTime, TimeUnit.SECONDS);
            cb.expireAfterAccess(expireTime, TimeUnit.SECONDS);
        /*
        cache = cb.build(new CacheLoader<>() {
            @Override
            public List<Object> load(String key) {
                return Collections.EMPTY_LIST;
            }
        });
        */
        cache = cb.build();
    }

    @Override
    public void update(ConfigurationTool configurationTool) throws ModuleException {
        stop(configurationTool);
        start(configurationTool);
    }

    @Override
    public void process(ConfigurationTool configurationTool, ExecutionContextTool executionContextTool) throws ModuleException {
        if (executionContextTool.getType().equals("default")) {
            if (executionContextTool.getFlowControlTool().countManagedExecutionContexts() > 0) {
                Stream.iterate(0, n -> n + 1)
                        .limit(executionContextTool.countSource())
                        .flatMap(i -> executionContextTool.getMessages(i).stream())
                        .forEach(a -> {
                            String name = genName(a.getMessages());
                            try {
                                List<Object> objects = cache.get(name, () -> {
                                            executionContextTool.getFlowControlTool().executeNow(
                                                    CommandType.EXECUTE,
                                                    0,
                                                    a.getMessages().stream()
                                                            .map(IValue::getValue)
                                                            .collect(Collectors.toList()));
                                            return executionContextTool.getFlowControlTool().getMessagesFromExecuted(0).stream()
                                                    .flatMap(a2 -> a2.getMessages().stream())
                                                    // .filter(m -> maxValueSize <= 0 || (ModuleUtils.isBytes(m) && ((byte[]) m.getValue()).length < maxValueSize) || (ModuleUtils.isString(m) && ((String) m.getValue()).length() < maxValueSize))
                                                    .map(IValue::getValue)
                                                    .collect(Collectors.toList());
                                        }
                                );
                                if (CollectionUtils.isNotEmpty(objects)) {
                                    executionContextTool.addMessage(objects);
                                } else if (!cacheNull || (maxValueSize > 0 && objects.stream().anyMatch(o -> ((o instanceof byte[]) && (((byte[]) o).length > maxValueSize)) || ((o instanceof String) && (((String) o).length() > maxValueSize))))) {
                                    cache.invalidate(name);
                                }
                            } catch (ExecutionException e) {
                                throw new ModuleException("error", e);
                            }

                        });

            } else {
                Stream.iterate(0, n -> n + 1)
                        .limit(executionContextTool.countSource())
                        .flatMap(i -> executionContextTool.getMessages(i).stream())
                        .map(IAction::getMessages)
                        // .filter(m -> ValueType.BYTE.equals(m.getType()) || ValueType.SHORT.equals(m.getType()) || ValueType.INTEGER.equals(m.getType()) || ValueType.LONG.equals(m.getType()) || ValueType.FLOAT.equals(m.getType()) || ValueType.DOUBLE.equals(m.getType()) || ValueType.BIG_INTEGER.equals(m.getType()) || ValueType.BIG_DECIMAL.equals(m.getType()))
                        // .map(m -> (Number) m.getValue())
                        .forEach(messages -> {
                            LinkedList<IMessage> messagesList = new LinkedList<>(messages);
                            while (!messagesList.isEmpty()) {
                                Type type = Type.values()[getNumber(messagesList).intValue() - 1];
                                process(configurationTool, executionContextTool, messagesList, type);
                            }
                        });
            }
        } else {
            Type type = Type.valueOf(executionContextTool.getType().toUpperCase());
            List<IMessage> messages = type == Type.size || type == Type.clear_all || type == Type.get_all ? List.of() : ModuleUtils.getLastActionWithData(executionContextTool.getMessages(0)).map(IAction::getMessages).orElse(List.of());
            process(configurationTool, executionContextTool, new LinkedList<>(messages), type);
        }
    }

    private void process(ConfigurationTool configurationTool, ExecutionContextTool executionContextTool, LinkedList<IMessage> messagesList, Type type) {
        try {
            switch (type) {
                case size:
                    executionContextTool.addMessage(cache.size());
                    break;
                case clear_all:
                    executionContextTool.addMessage(cache.size());
                    cache.invalidateAll();
                    executionContextTool.addMessage(cache.size());
                    break;
                case get: {
                    //get
                    List<Object> list = cache.getIfPresent(getString(messagesList));
                    if (CollectionUtils.isNotEmpty(list))
                        executionContextTool.addMessage(list);
                    break;
                }
                case put: {
                    //put
                    String key = getString(messagesList);
                    List<Object> objects = messagesList.stream()
                            .filter(m -> maxValueSize <= 0 || (ModuleUtils.isBytes(m) && ((byte[]) m.getValue()).length < maxValueSize) || (ModuleUtils.isString(m) && ((String) m.getValue()).length() < maxValueSize))
                            .map(IValue::getValue)
                            .collect(Collectors.toList());
                    messagesList.clear();
                    if (CollectionUtils.isNotEmpty(objects) || cacheNull)
                        cache.put(key, objects);
                    break;
                }
                case invalidate:
                    //invalidate entry
                    cache.invalidate(getString(messagesList));
                    break;
                case get_all:
                    //get all entries keys
                    cache.asMap().forEach((k, v) -> {
                        executionContextTool.addMessage(k);
                        // executionContextTool.addMessage(v);
                    });
                    break;
            }
        } catch (Exception e) {
            executionContextTool.addError(ModuleUtils.getErrorMessageOrClassName(e));
            configurationTool.loggerWarn(ModuleUtils.getStackTraceAsString(e));
        }
    }

    private String getString(LinkedList<IMessage> messages) {
        IMessage m = messages.poll();
        if (m == null)
            throw new ModuleException("need message");
        return m.getValue().toString();
    }

    private Number getNumber(LinkedList<IMessage> messages) {
        IMessage m = messages.poll();
        if (m == null)
            throw new ModuleException("need message");
        if (!(ValueType.BYTE.equals(m.getType()) || ValueType.SHORT.equals(m.getType()) || ValueType.INTEGER.equals(m.getType()) || ValueType.LONG.equals(m.getType()) || ValueType.FLOAT.equals(m.getType()) || ValueType.DOUBLE.equals(m.getType()) || ValueType.BIG_INTEGER.equals(m.getType()) || ValueType.BIG_DECIMAL.equals(m.getType())))
            throw new ModuleException("need message of number type");
        return (Number) m.getValue();
    }

    @Override
    public void stop(ConfigurationTool configurationTool) throws ModuleException {
        cache.invalidateAll();
        cache = null;
    }

    private String genName(List<IMessage> messages) {
        String result = "";
        if (CollectionUtils.isNotEmpty(messages)) {
            result = messages.stream()
                    .map(m -> {
                        if (ValueType.BYTES.equals(m.getType())) {
                            return Base64.getEncoder().encodeToString((byte[]) m.getValue());
                        } else {
                            return m.getValue().toString();
                        }
                    })
                    .collect(Collectors.joining("_"));
        }
        return result;
    }

    /*
    private List<Object> executeParallel(ExecutionContextTool executionContextTool, int cfgId, List<Object> params) {
        long threadId = executionContextTool.getFlowControlTool().executeParallel(
                CommandType.EXECUTE,
                List.of(cfgId),
                params,
                0,
                0);
        List<Object> response = null;
        try {
            do {
                Thread.sleep(10);
            } while (executionContextTool.getFlowControlTool().isThreadActive(threadId));
            response = executionContextTool.getFlowControlTool().getMessagesFromExecuted(threadId, cfgId).stream()
                    .flatMap(a -> a.getMessages().stream().map(IValue::getValue))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("error", e);
        } finally {
            executionContextTool.getFlowControlTool().releaseThread(threadId);
        }
        return response;
    }
    */

}
