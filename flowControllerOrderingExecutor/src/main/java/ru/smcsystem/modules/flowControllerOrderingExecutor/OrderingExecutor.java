package ru.smcsystem.modules.flowControllerOrderingExecutor;

import ru.smcsystem.api.dto.IAction;
import ru.smcsystem.api.dto.IValue;
import ru.smcsystem.api.enumeration.CommandType;
import ru.smcsystem.api.enumeration.MessageType;
import ru.smcsystem.api.exceptions.ModuleException;
import ru.smcsystem.api.module.Module;
import ru.smcsystem.api.tools.ConfigurationTool;
import ru.smcsystem.api.tools.execution.ExecutionContextTool;
import ru.smcsystem.smc.utils.ModuleUtils;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class OrderingExecutor implements Module {

    private ExecutionType executionType;
    private CommandType type;
    private Boolean isNeedReturnDataFromLast;
    private Boolean breakWhenError;
    private Boolean breakWhenNoDataFromPrev;
    private Boolean isNeedBreakAndReturnDataFromAny;

    @Override
    public void start(ConfigurationTool configurationTool) throws ModuleException {
        executionType = configurationTool.getSetting("executionType").map(ModuleUtils::getString).map(ExecutionType::valueOf)
                .orElseThrow(() -> new ModuleException("executionType setting"));
        type = configurationTool.getSetting("type").map(ModuleUtils::getString).map(String::toUpperCase).map(CommandType::valueOf)
                .orElseThrow(() -> new ModuleException("type setting"));
        isNeedReturnDataFromLast = configurationTool.getSetting("isNeedReturnDataFromLast").map(ModuleUtils::toBoolean)
                .orElseThrow(() -> new ModuleException("isNeedReturnDataFromLast setting"));
        breakWhenError = configurationTool.getSetting("breakWhenError").map(ModuleUtils::toBoolean)
                .orElseThrow(() -> new ModuleException("breakWhenError setting"));
        breakWhenNoDataFromPrev = configurationTool.getSetting("breakWhenNoDataFromPrev").map(ModuleUtils::toBoolean)
                .orElseThrow(() -> new ModuleException("breakWhenNoDataFromPrev setting"));
        isNeedBreakAndReturnDataFromAny = configurationTool.getSetting("isNeedBreakAndReturnDataFromAny").map(ModuleUtils::toBoolean)
                .orElseThrow(() -> new ModuleException("isNeedBreakAndReturnDataFromAny setting"));
    }

    @Override
    public void update(ConfigurationTool configurationTool) throws ModuleException {
        stop(configurationTool);
        start(configurationTool);
    }

    @Override
    public void process(ConfigurationTool configurationTools, ExecutionContextTool executionContextTool) throws ModuleException {
        if (executionContextTool.getFlowControlTool().countManagedExecutionContexts() == 0)
            return;
        CommandType type = Objects.equals(executionContextTool.getType(), "default") ? this.type : CommandType.valueOf(executionContextTool.getType().toUpperCase());
        switch (type) {
            case START:
            case STOP:
            case UPDATE:
                switch (executionType) {
                    case now:
                        for (int i = 0; i < executionContextTool.getFlowControlTool().countManagedExecutionContexts(); i++) {
                            executionContextTool.getFlowControlTool().executeNow(type, i, null);
                            List<IAction> messagesFromExecuted = executionContextTool.getFlowControlTool().getMessagesFromExecuted(i);
                            if (breakWhenError && messagesFromExecuted.stream().anyMatch(a -> a.getMessages().stream().anyMatch(m -> MessageType.ERROR.equals(m.getMessageType()))))
                                break;
                        }
                        break;
                    case later:
                        executionContextTool.getFlowControlTool().executeParallel(type, IntStream.range(0, executionContextTool.getFlowControlTool().countManagedExecutionContexts()).boxed().collect(Collectors.toList()), null, 1, 0);
                        break;
                    case parallel:
                        IntStream.range(0, executionContextTool.getFlowControlTool().countManagedExecutionContexts()).forEach(i -> executionContextTool.getFlowControlTool().executeParallel(type, List.of(i), null, 0, 0));
                        break;
                }
                break;
            case EXECUTE: {
                List<Object> inputValues = executionContextTool.countSource() > 0 ?
                        Stream.iterate(0, n -> n + 1)
                                .limit(executionContextTool.countSource())
                                .flatMap(n -> executionContextTool.getMessages(n).stream())
                                .flatMap(a -> a.getMessages().stream())
                                .map(IValue::getValue)
                                .collect(Collectors.toList()) :
                        null;
                List<Object> result = null;
                switch (executionType) {
                    case now:
                        for (int i = 0; i < executionContextTool.getFlowControlTool().countManagedExecutionContexts(); i++) {
                            if (breakWhenNoDataFromPrev && i > 0 && executionContextTool.getFlowControlTool().getMessagesFromExecuted(i - 1).stream()
                                    .noneMatch(a -> a.getMessages().stream().anyMatch(m -> MessageType.DATA.equals(m.getMessageType())))) {
                                /*
                                if(!isNeedReturnDataFromLast)
                                    executionContextTool.addMessage(String.format("no messages from configuration %d", i - 1));
                                */
                                break;
                            }

                            executionContextTool.getFlowControlTool().executeNow(type, i, inputValues);
                            List<IAction> messagesFromExecuted = executionContextTool.getFlowControlTool().getMessagesFromExecuted(i);
                            if (breakWhenError && messagesFromExecuted.stream().anyMatch(a -> a.getMessages().stream().anyMatch(m -> MessageType.ERROR.equals(m.getMessageType()))))
                                break;
                            if (isNeedBreakAndReturnDataFromAny && messagesFromExecuted.stream().anyMatch(ModuleUtils::hasData)) {
                                result = ModuleUtils.getFirstActionWithDataList(messagesFromExecuted)
                                        .map(l -> l.stream().map(IValue::getValue).collect(Collectors.toList()))
                                        .orElse(null);
                                break;
                            }
                        }
                        if (result == null && isNeedReturnDataFromLast && executionContextTool.getFlowControlTool().countManagedExecutionContexts() > 0)
                            result = ModuleUtils.getFirstActionWithDataList(executionContextTool.getFlowControlTool()
                                            .getMessagesFromExecuted(executionContextTool.getFlowControlTool().countManagedExecutionContexts() - 1))
                                    .map(l -> l.stream().map(IValue::getValue).collect(Collectors.toList()))
                                    .orElse(null);
                        break;
                    case later:
                        executionContextTool.getFlowControlTool().executeParallel(type, IntStream.range(0, executionContextTool.getFlowControlTool().countManagedExecutionContexts()).boxed().collect(Collectors.toList()), inputValues, 1, 0);
                        break;
                    case parallel:
                        IntStream.range(0, executionContextTool.getFlowControlTool().countManagedExecutionContexts()).forEach(i -> executionContextTool.getFlowControlTool().executeParallel(type, List.of(i), inputValues, 0, 0));
                        break;
                    case parallel_wait: {
                        List<Long> ids = IntStream.range(0, executionContextTool.getFlowControlTool().countManagedExecutionContexts())
                                .mapToObj(i -> executionContextTool.getFlowControlTool().executeParallel(type, List.of(i), inputValues, 0, 0))
                                .collect(Collectors.toList());
                        ids.forEach(id -> {
                            do {
                                try {
                                    Thread.sleep(100);
                                } catch (Exception e) {
                                }
                            } while (!executionContextTool.isNeedStop() && executionContextTool.getFlowControlTool().isThreadActive(id));
                        });
                        if (!ids.isEmpty() && executionContextTool.getFlowControlTool().countManagedExecutionContexts() > 0) {
                            if (isNeedBreakAndReturnDataFromAny) {
                                for (int i = 0; i < executionContextTool.getFlowControlTool().countManagedExecutionContexts(); i++) {
                                    result = ModuleUtils.getFirstActionWithDataList(executionContextTool.getFlowControlTool().getMessagesFromExecuted(ids.get(i), i))
                                            .map(l -> l.stream().map(IValue::getValue).collect(Collectors.toList()))
                                            .orElse(null);
                                    if (result != null)
                                        break;
                                }
                            }
                            if (result == null && isNeedReturnDataFromLast)
                                result = ModuleUtils.getFirstActionWithDataList(executionContextTool.getFlowControlTool()
                                                .getMessagesFromExecuted(ids.get(ids.size() - 1), executionContextTool.getFlowControlTool().countManagedExecutionContexts() - 1))
                                        .map(l -> l.stream().map(IValue::getValue).collect(Collectors.toList()))
                                        .orElse(null);
                        }
                        break;
                    }
                }
                if (result != null)
                    executionContextTool.addMessage(result);
                break;
            }
        }
    }

    @Override
    public void stop(ConfigurationTool configurationTool) throws ModuleException {
        type = null;
        executionType = null;
        isNeedReturnDataFromLast = null;
        breakWhenError = null;
        isNeedBreakAndReturnDataFromAny = null;
    }

    enum ExecutionType {
        now,
        later,
        parallel,
        parallel_wait
    }
}
