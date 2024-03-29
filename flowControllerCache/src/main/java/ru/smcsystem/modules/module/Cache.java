package ru.smcsystem.modules.module;

import com.google.common.cache.CacheBuilder;
import org.apache.commons.collections4.CollectionUtils;
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
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Cache implements Module {

    private boolean cacheNull;
    private int maxValueSize;

    private com.google.common.cache.Cache<String, List<Object>> cache;

    private enum Type {
        get_or_load, size, clear_all, get, put, invalidate, get_all, get_or_load_file_part, get_size
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
            ModuleUtils.processMessages(configurationTool, executionContextTool, (i, messages) -> {
                while (!messages.isEmpty())
                    process(executionContextTool, messages, executionContextTool.getFlowControlTool().countManagedExecutionContexts() > 0 ? Type.get_or_load : null);
            });
        } else {
            Type type = Type.valueOf(executionContextTool.getType().toLowerCase());
            ModuleUtils.processMessages(configurationTool, executionContextTool, 0, (i, messages) ->
                    process(executionContextTool, messages, type));
        }
    }

    private void process(ExecutionContextTool executionContextTool, LinkedList<IMessage> messagesList, Type type) throws ExecutionException {
        if (type == null)
            type = Type.values()[getNumber(messagesList).intValue()];
        switch (type) {
            case get_or_load: {
                String name = genName(messagesList);
                List<Object> objects = cache.get(name, () -> {
                            executionContextTool.getFlowControlTool().executeNow(
                                    CommandType.EXECUTE,
                                    0,
                                    messagesList.stream()
                                            .map(IValue::getValue)
                                            .collect(Collectors.toList()));
                            return executionContextTool.getFlowControlTool().getMessagesFromExecuted(0).stream()
                                    .flatMap(a2 -> a2.getMessages().stream())
                                    // .filter(m -> maxValueSize <= 0 || (ModuleUtils.isBytes(m) && ((byte[]) m.getValue()).length < maxValueSize) || (ModuleUtils.isString(m) && ((String) m.getValue()).length() < maxValueSize))
                                    .map(IValue::getValue)
                                    .collect(Collectors.toList());
                        }
                );
                messagesList.clear();
                if (CollectionUtils.isNotEmpty(objects)) {
                    executionContextTool.addMessage(objects);
                } else if (!cacheNull || (maxValueSize > 0 && objects.stream().anyMatch(o -> ((o instanceof byte[]) && (((byte[]) o).length > maxValueSize)) || ((o instanceof String) && (((String) o).length() > maxValueSize))))) {
                    cache.invalidate(name);
                }
                break;
            }
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
            case get_or_load_file_part: {
                String key = ModuleUtils.toString(messagesList.poll());
                Number position = ModuleUtils.getNumber(messagesList.poll());
                Number size = ModuleUtils.getNumber(messagesList.poll());
                long positionL = position != null ? position.longValue() : 0;
                int sizeI = size != null ? size.intValue() : Integer.MAX_VALUE;
                List<Object> list = cache.getIfPresent(key);
                boolean isNotExist = false;
                if (CollectionUtils.isEmpty(list)) {
                    isNotExist = true;
                    list = ModuleUtils.executeAndGetMessages(executionContextTool, 0, List.of(key, positionL, sizeI))
                            .stream()
                            .flatMap(Collection::stream)
                            .filter(ModuleUtils::isBytes)
                            .map(ModuleUtils::getBytes)
                            .collect(Collectors.toList());
                }
                byte[] content = null;
                if (CollectionUtils.isNotEmpty(list)) {
                    Object value = list.get(0);
                    if (value instanceof byte[])
                        content = (byte[]) value;
                }
                if (content != null) {
                    if (isNotExist && size != null && content.length < sizeI && positionL == 0 && (maxValueSize == 0 || maxValueSize > content.length))
                        cache.put(key, List.of(content));
                    executionContextTool.addMessage(content);
                }
                break;
            }
            case get_size: {
                String key = ModuleUtils.toString(messagesList.poll());
                List<Object> list = cache.getIfPresent(key);
                long size = 0;
                if (CollectionUtils.isNotEmpty(list)) {
                    Object value = list.get(0);
                    if (value instanceof byte[])
                        size = ((byte[]) value).length;
                } else if (executionContextTool.getFlowControlTool().countManagedExecutionContexts() > 0) {
                    size = ModuleUtils.executeAndGetMessages(executionContextTool, 0, List.of(key))
                            .stream()
                            .flatMap(Collection::stream)
                            .filter(ModuleUtils::isNumber)
                            .map(ModuleUtils::getNumber)
                            .findFirst()
                            .map(Number::longValue)
                            .orElse(0L);
                }
                executionContextTool.addMessage(size);
                break;
            }
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
