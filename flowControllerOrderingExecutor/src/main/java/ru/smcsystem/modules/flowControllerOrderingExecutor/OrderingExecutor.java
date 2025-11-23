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
import java.util.Optional;
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
    private Boolean isNeedReturnErrorFromLast;

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
        isNeedReturnErrorFromLast = configurationTool.getSetting("isNeedReturnErrorFromLast").map(ModuleUtils::toBoolean)
                .orElseThrow(() -> new ModuleException("isNeedReturnErrorFromLast setting"));
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
                List<Object> resultData = null;
                List<Object> resultErrors = null;
                switch (executionType) {
                    case now: {
                        Optional<IAction> lastAction = Optional.empty();
                        for (int i = 0; i < executionContextTool.getFlowControlTool().countManagedExecutionContexts(); i++) {
                            if (breakWhenNoDataFromPrev && i > 0 && lastAction.stream().noneMatch(
                                    a -> !a.getMessages().isEmpty() && !ModuleUtils.hasErrors(a))) {
                                /*
                                if(!isNeedReturnDataFromLast)
                                    executionContextTool.addMessage(String.format("no messages from configuration %d", i - 1));
                                */
                                break;
                            }

                            executionContextTool.getFlowControlTool().executeNow(type, i, inputValues);
                            lastAction = ModuleUtils.getLastActionExecuteWithMessagesFromCommands(executionContextTool.getFlowControlTool().getCommandsFromExecuted(i));
                            if (breakWhenError && lastAction.stream().anyMatch(ModuleUtils::hasErrors))
                                break;
                            if (isNeedBreakAndReturnDataFromAny && lastAction.isPresent() && !ModuleUtils.hasErrors(lastAction.get())) {
                                resultData = lastAction
                                        .map(ModuleUtils::getData)
                                        .map(l -> l.stream().map(IValue::getValue).collect(Collectors.toList()))
                                        .orElse(null);
                                break;
                            }
                        }
                        if (resultData == null)
                            resultData = getData(lastAction);
                        resultErrors = getError(lastAction);
                        break;
                    }
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
                            Optional<IAction> lastAction = Optional.empty();
                            if (isNeedBreakAndReturnDataFromAny) {
                                for (int i = executionContextTool.getFlowControlTool().countManagedExecutionContexts() - 1; i >= 0; i--) {
                                    lastAction = ModuleUtils.getLastActionExecuteWithMessagesFromCommands(executionContextTool.getFlowControlTool().getCommandsFromExecuted(i));
                                    if (lastAction.isPresent() && !ModuleUtils.hasErrors(lastAction.get())) {
                                        resultData = lastAction
                                                .map(ModuleUtils::getData)
                                                .map(l -> l.stream().map(IValue::getValue).collect(Collectors.toList()))
                                                .orElse(null);
                                        break;
                                    }
                                }
                            }
                            if (resultData == null)
                                resultData = getData(lastAction);
                            resultErrors = getError(lastAction);
                        }
                        break;
                    }
                }
                if (resultData != null)
                    executionContextTool.addMessage(resultData);
                if (resultErrors != null)
                    executionContextTool.addError(resultErrors);
                break;
            }
        }
    }

    private List<Object> getData(Optional<IAction> lastAction) {
        if (isNeedReturnDataFromLast && lastAction.isPresent() && !ModuleUtils.hasErrors(lastAction.get())) {
            return lastAction
                    .map(ModuleUtils::getData)
                    .map(l -> l.stream().map(IValue::getValue).collect(Collectors.toList()))
                    .orElse(null);
        }
        return null;
    }

    private List<Object> getError(Optional<IAction> lastAction) {
        if (isNeedReturnErrorFromLast && lastAction.isPresent()) {
            return lastAction
                    .map(ModuleUtils::getErrors)
                    .map(l -> l.stream().map(IValue::getValue).collect(Collectors.toList()))
                    .orElse(null);
        }
        return null;
    }

    @Override
    public void stop(ConfigurationTool configurationTool) throws ModuleException {
        type = null;
        executionType = null;
        isNeedReturnDataFromLast = null;
        breakWhenError = null;
        isNeedBreakAndReturnDataFromAny = null;
        isNeedReturnErrorFromLast = null;
    }

    enum ExecutionType {
        now,
        later,
        parallel,
        parallel_wait
    }
}
