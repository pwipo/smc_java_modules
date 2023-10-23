package ru.smcsystem.modules.flowControllerOrderingExecutor;

import ru.smcsystem.api.dto.IAction;
import ru.smcsystem.api.dto.IValue;
import ru.smcsystem.api.enumeration.CommandType;
import ru.smcsystem.api.enumeration.MessageType;
import ru.smcsystem.api.exceptions.ModuleException;
import ru.smcsystem.api.module.Module;
import ru.smcsystem.api.tools.ConfigurationTool;
import ru.smcsystem.api.tools.execution.ExecutionContextTool;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class OrderingExecutor implements Module {

    enum ExecutionType {
        now,
        later,
        parallel,
        parallel_wait
    }

    private ExecutionType executionType;
    private CommandType type;
    private Boolean isNeedReturnDataFromLast;
    private Boolean breakWhenError;
    private Boolean breakWhenNoDataFromPrev;

    @Override
    public void start(ConfigurationTool configurationTool) throws ModuleException {
        executionType = ExecutionType.valueOf((String) configurationTool.getSetting("executionType").orElseThrow(() -> new ModuleException("executionType setting")).getValue());
        type = CommandType.valueOf(((String) configurationTool.getSetting("type").orElseThrow(() -> new ModuleException("type setting")).getValue()).toUpperCase());
        isNeedReturnDataFromLast = Boolean.valueOf((String) configurationTool.getSetting("isNeedReturnDataFromLast").orElseThrow(() -> new ModuleException("isNeedReturnDataFromLast setting not found")).getValue());
        breakWhenError = Boolean.valueOf((String) configurationTool.getSetting("breakWhenError").orElseThrow(() -> new ModuleException("breakWhenError setting not found")).getValue());
        breakWhenNoDataFromPrev = Boolean.valueOf((String) configurationTool.getSetting("breakWhenNoDataFromPrev").orElseThrow(() -> new ModuleException("breakWhenNoDataFromPrev setting not found")).getValue());
    }

    @Override
    public void update(ConfigurationTool configurationTool) throws ModuleException {
        stop(configurationTool);
        start(configurationTool);
    }

    @Override
    public void process(ConfigurationTool configurationTools, ExecutionContextTool executionContextTool) throws ModuleException {
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
            case EXECUTE:
                List<Object> inputValues = executionContextTool.countSource() > 0 ?
                        Stream.iterate(0, n -> n + 1)
                                .limit(executionContextTool.countSource())
                                .flatMap(n -> executionContextTool.getMessages(n).stream())
                                .flatMap(a -> a.getMessages().stream())
                                .map(IValue::getValue)
                                .collect(Collectors.toList()) :
                        null;
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
                        }
                        if (isNeedReturnDataFromLast && executionContextTool.getFlowControlTool().countManagedExecutionContexts() > 0)
                            executionContextTool.getFlowControlTool().getMessagesFromExecuted(executionContextTool.getFlowControlTool().countManagedExecutionContexts() - 1).forEach(action ->
                                    executionContextTool.addMessage(action.getMessages().stream().map(IValue::getValue).collect(Collectors.toList())));
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
                        if (!ids.isEmpty() && isNeedReturnDataFromLast && executionContextTool.getFlowControlTool().countManagedExecutionContexts() > 0)
                            executionContextTool.getFlowControlTool().getMessagesFromExecuted(ids.get(ids.size() - 1), executionContextTool.getFlowControlTool().countManagedExecutionContexts() - 1)
                                    .forEach(action -> executionContextTool.addMessage(action.getMessages().stream().map(IValue::getValue).collect(Collectors.toList())));
                        break;
                    }
                }
                break;
        }
    }

    @Override
    public void stop(ConfigurationTool configurationTool) throws ModuleException {
        type = null;
        executionType = null;
        isNeedReturnDataFromLast = null;
        breakWhenError = null;
    }
}
