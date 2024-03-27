package ru.smcsystem.smcmodules.module;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import ru.smcsystem.api.dto.IMessage;
import ru.smcsystem.api.dto.IValue;
import ru.smcsystem.api.enumeration.CommandType;
import ru.smcsystem.api.exceptions.ModuleException;
import ru.smcsystem.api.module.Module;
import ru.smcsystem.api.tools.ConfigurationTool;
import ru.smcsystem.api.tools.execution.ExecutionContextTool;
import ru.smcsystem.smc.utils.ModuleUtils;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StringUtil implements Module {

    enum Type {
        JOIN,
        JOIN_INNER_WITH_TIME,
        SPLIT,
        SPLIT_REGEXP,
        APPEND,
        APPEND_FIRST,
        REPLACE,
        REPLACE_REGEXP,
        REPLACE_REGEXP_GROUP_ONE,
        CONTAIN,
        MATCH,
        PLACEHOLDERS,
        PLACEHOLDERS_OR_EMPTY,
        FILTER_SIZE,
        REGEXP_GROUP_ONE,
        ESCAPE_SQL,
        SIZE,
        IS_BLANK,
        IS_NOT_BLANK,
        FILTER,
        FILTER_ACTION,
        SUBSTRING,
        TRIM,
        REPLACE_REGEXP_MULTILINE,
        PLACEHOLDERS_DYNAMIC,
        PLACEHOLDERS_DYNAMIC_OR_EMPTY,
    }

    private Type type;
    private String value;
    private List<String> values;
    private List<Number> numberValues;

    private List<Pattern> patterns;
    private Integer string_size;

    @Override
    public void start(ConfigurationTool configurationTool) throws ModuleException {
        type = Type.valueOf((String) configurationTool.getSetting("type").orElseThrow(() -> new ModuleException("type setting")).getValue());
        value = (String) configurationTool.getSetting("value").orElseThrow(() -> new ModuleException("value setting")).getValue();
        patterns = null;
        values = null;
        numberValues = null;
        updateSettings();
    }

    private void updateSettings() {
        switch (type) {
            case SPLIT:
            case SPLIT_REGEXP:
                if (StringUtils.isBlank(value))
                    value = null;
                break;
            case REPLACE:
            case REPLACE_REGEXP:
            case REPLACE_REGEXP_GROUP_ONE:
            case REPLACE_REGEXP_MULTILINE:
                if (StringUtils.isBlank(value) || !value.contains("::"))
                    throw new ModuleException("wrong value");
                if (value.endsWith("::"))
                    value = value.substring(0, value.length() - 2);
                values = Arrays.asList(value.split("::"));
                if (values.isEmpty())
                    throw new ModuleException("wrong value");
                if (values.size() == 1)
                    values = List.of(values.get(0), "");
                if (type.equals(Type.REPLACE_REGEXP) || type.equals(Type.REPLACE_REGEXP_GROUP_ONE)) {
                    patterns = List.of(Pattern.compile(values.get(0)));
                    values = values.subList(1, values.size());
                } else if (type.equals(Type.REPLACE_REGEXP_MULTILINE)) {
                    patterns = List.of(Pattern.compile(values.get(0), Pattern.MULTILINE));
                    values = values.subList(1, values.size());
                }
                break;
            case CONTAIN:
                values = Arrays.asList(value.split("::"));
                if (values.isEmpty() || values.stream().anyMatch(StringUtils::isBlank))
                    throw new ModuleException("wrong value");
                break;
            case MATCH:
            case REGEXP_GROUP_ONE:
            case FILTER:
            case FILTER_ACTION:
                if (StringUtils.isBlank(value))
                    break;
                values = Arrays.asList(value.split("::"));
                // if (values.isEmpty() || values.stream().anyMatch(StringUtils::isBlank))
                //     throw new ModuleException("wrong value");
                patterns = values.stream()
                        .filter(StringUtils::isNoneBlank)
                        .map(Pattern::compile)
                        .collect(Collectors.toList());
                break;
            case FILTER_SIZE:
                if (StringUtils.isBlank(value))
                    throw new ModuleException("wrong value");
                string_size = Integer.valueOf(value);
                break;
            case SUBSTRING:
                if (StringUtils.isBlank(value))
                    throw new ModuleException("wrong value");
                numberValues = Arrays.stream(value.split("::", 0))
                        .filter(NumberUtils::isCreatable)
                        .map(NumberUtils::createInteger)
                        .collect(Collectors.toList());
                break;
        }
    }

    @Override
    public void update(ConfigurationTool configurationTool) throws ModuleException {
        stop(configurationTool);
        start(configurationTool);
    }

    @Override
    public void process(ConfigurationTool configurationTool, ExecutionContextTool executionContextTool) throws ModuleException {
        Type type = Objects.equals(executionContextTool.getType(), "default") ? this.type : Type.valueOf(executionContextTool.getType().toUpperCase());
        if (type != this.type)
            updateSettings();
        switch (type) {
            case JOIN: {
                String result = Stream.iterate(0, n -> n + 1)
                        .limit(executionContextTool.countSource())
                        .map(i -> executionContextTool.getMessages(i).stream()
                                .map(a -> a.getMessages().stream()
                                        // .filter(m -> ValueType.STRING.equals(m.getType()))
                                        .map(ModuleUtils::toString)
                                        .collect(Collectors.joining(value)))
                                .collect(Collectors.joining(value)))
                        .collect(Collectors.joining(value));
                if (StringUtils.isNotBlank(result))
                    executionContextTool.addMessage(result);
                break;
            }
            case JOIN_INNER_WITH_TIME: {
                Stream.iterate(0, n -> n + 1)
                        .limit(executionContextTool.countSource())
                        .flatMap(i -> executionContextTool.getMessages(i).stream())
                        .filter(a -> !a.getMessages().isEmpty())
                        .forEach(a -> {
                            String msg = a.getMessages().stream().map(ModuleUtils::toString).collect(Collectors.joining(value));
                            if (StringUtils.isBlank(msg))
                                return;
                            executionContextTool.addMessage(msg);
                            executionContextTool.addMessage(a.getMessages().get(0).getDate().getTime());
                        });
                break;
            }
            case SPLIT:
                Stream.iterate(0, n -> n + 1)
                        .limit(executionContextTool.countSource())
                        .forEach(i -> executionContextTool.getMessages(i).forEach(a -> a.getMessages().stream()
                                // .filter(m -> ValueType.STRING.equals(m.getType()))
                                .map(ModuleUtils::toString)
                                .forEach(s -> Arrays.stream(value != null ? StringUtils.split(s, value) : StringUtils.split(s))
                                        .forEach(v -> executionContextTool.addMessage(v.trim())))));
                break;
            case SPLIT_REGEXP:
                Stream.iterate(0, n -> n + 1)
                        .limit(executionContextTool.countSource())
                        .forEach(i -> executionContextTool.getMessages(i).forEach(a -> a.getMessages().stream()
                                .map(ModuleUtils::toString)
                                .forEach(val -> Arrays.stream(value != null ? val.split(value) : StringUtils.split(val))
                                        .forEach(v -> executionContextTool.addMessage(v.trim())))));
                break;
            case APPEND:
                Stream.iterate(0, n -> n + 1)
                        .limit(executionContextTool.countSource())
                        .forEach(i -> executionContextTool.getMessages(i).forEach(a -> a.getMessages().stream()
                                // .filter(m -> ValueType.STRING.equals(m.getType()))
                                .map(m -> ModuleUtils.toString(m) + value)
                                .forEach(v -> executionContextTool.addMessage(v.trim()))));
                break;
            case APPEND_FIRST:
                Stream.iterate(0, n -> n + 1)
                        .limit(executionContextTool.countSource())
                        .forEach(i -> executionContextTool.getMessages(i).forEach(a -> a.getMessages().stream()
                                // .filter(m -> ValueType.STRING.equals(m.getType()))
                                .map(m -> value + ModuleUtils.toString(m))
                                .forEach(v -> executionContextTool.addMessage(v.trim()))));
                break;
            case REPLACE:
                Stream.iterate(0, n -> n + 1)
                        .limit(executionContextTool.countSource())
                        .forEach(i -> executionContextTool.getMessages(i).forEach(a -> a.getMessages().stream()
                                // .filter(m -> ValueType.STRING.equals(m.getType()))
                                .map(ModuleUtils::toString)
                                .map(s -> StringUtils.replace(s, values.get(0), values.get(1)))
                                .forEach(v -> executionContextTool.addMessage(v.trim()))));
                break;
            case REPLACE_REGEXP:
            case REPLACE_REGEXP_MULTILINE:
                Stream.iterate(0, n -> n + 1)
                        .limit(executionContextTool.countSource())
                        .forEach(i -> executionContextTool.getMessages(i).forEach(a -> a.getMessages().stream()
                                // .filter(m -> ValueType.STRING.equals(m.getType()))
                                .map(ModuleUtils::toString)
                                .map(s -> {
                                    // StringUtils.replaceAll(s, values.get(0), values.get(1));
                                    Matcher matcher = patterns.get(0).matcher(s);
                                    if (executionContextTool.getFlowControlTool().countManagedExecutionContexts() > 0) {
                                        return matcher.replaceAll(mr -> {
                                            executionContextTool.getFlowControlTool().executeNow(CommandType.EXECUTE, 0, List.of(values.get(0).substring(mr.start(), mr.end())));
                                            return executionContextTool.getFlowControlTool().getMessagesFromExecuted(0).stream()
                                                    .flatMap(a1 -> a1.getMessages().stream())
                                                    .map(ModuleUtils::toString)
                                                    .collect(Collectors.joining());
                                        });
                                    } else {
                                        return matcher.replaceAll(values.get(0));
                                    }
                                })
                                .forEach(v -> executionContextTool.addMessage(v.trim()))));
                break;
            case REPLACE_REGEXP_GROUP_ONE:
                if (patterns == null)
                    break;
                Stream.iterate(0, n -> n + 1)
                        .limit(executionContextTool.countSource())
                        .flatMap(i -> executionContextTool.getMessages(i).stream())
                        .flatMap(a -> a.getMessages().stream())
                        // .filter(m -> ValueType.STRING.equals(m.getType()))
                        .map(ModuleUtils::toString)
                        .forEach(value -> {
                            LinkedList<Integer> startPositions = new LinkedList<>();
                            LinkedList<Integer> endPositions = new LinkedList<>();
                            Matcher matcher = patterns.get(0).matcher(value);
                            int groupId = 1;
                            while (matcher.find()) {
                                startPositions.add(matcher.start(groupId));
                                endPositions.add(matcher.end(groupId));
                            }
                            StringBuilder sb = new StringBuilder(value);
                            while (!startPositions.isEmpty()) {
                                int start = startPositions.poll();
                                int end = endPositions.poll();
                                if (start >= 0 && end >= 0) {
                                    // String valueGroup1 = value.substring(start, end);
                                    String valueGroup1Result = values.get(0);
                                    if (executionContextTool.getFlowControlTool().countManagedExecutionContexts() > 0) {
                                        executionContextTool.getFlowControlTool().executeNow(CommandType.EXECUTE, 0, List.of(value.substring(start, end)));
                                        valueGroup1Result = executionContextTool.getFlowControlTool().getMessagesFromExecuted(0).stream()
                                                .flatMap(a1 -> a1.getMessages().stream())
                                                .map(ModuleUtils::toString)
                                                .collect(Collectors.joining());
                                    }
                                    sb.replace(start, end, valueGroup1Result);
                                }
                            }
                            executionContextTool.addMessage(sb.toString());
                        });
                break;
            case CONTAIN:
                Stream.iterate(0, n -> n + 1)
                        .limit(executionContextTool.countSource())
                        .forEach(i -> executionContextTool.getMessages(i).forEach(a -> a.getMessages().stream()
                                // .filter(m -> ValueType.STRING.equals(m.getType()))
                                .map(ModuleUtils::toString)
                                .forEach(value -> {
                                    for (int j = 0; j < values.size(); j++) {
                                        if (StringUtils.contains(values.get(j), value)) {
                                            executionContextTool.addMessage(j);
                                            break;
                                        }
                                    }
                                })));
                break;
            case MATCH:
                if (patterns == null)
                    break;
                Stream.iterate(0, n -> n + 1)
                        .limit(executionContextTool.countSource())
                        .forEach(i -> executionContextTool.getMessages(i).forEach(a -> a.getMessages().stream()
                                // .filter(m -> ValueType.STRING.equals(m.getType()))
                                .map(ModuleUtils::toString)
                                .forEach(value -> {
                                    for (int j = 0; j < patterns.size(); j++) {
                                        if (patterns.get(j).matcher(value).find()) {
                                            executionContextTool.addMessage(j);
                                            break;
                                        }
                                    }
                                })));
                break;
            case PLACEHOLDERS:
                executionContextTool.addMessage(
                        new MessageFormat(value).format(
                                Stream.iterate(0, n -> n + 1)
                                        .limit(executionContextTool.countSource())
                                        .flatMap(i -> executionContextTool.getMessages(i).stream())
                                        .flatMap(a -> a.getMessages().stream())
                                        // .filter(m->ValueType.STRING.equals(m.getValue()))
                                        .map(ModuleUtils::toString)
                                        .toArray())
                );
                break;
            case PLACEHOLDERS_OR_EMPTY: {
                String result = new MessageFormat(value).format(
                        Stream.iterate(0, n -> n + 1)
                                .limit(executionContextTool.countSource())
                                .flatMap(i -> executionContextTool.getMessages(i).stream())
                                .flatMap(a -> a.getMessages().stream())
                                // .filter(m->ValueType.STRING.equals(m.getValue()))
                                .map(ModuleUtils::toString)
                                .toArray());
                if (!result.matches("(?s).*\\{\\d+\\}.*"))
                    executionContextTool.addMessage(result);
                break;
            }
            case FILTER_SIZE:
                executionContextTool.addMessage(
                        Stream.iterate(0, n -> n + 1)
                                .limit(executionContextTool.countSource())
                                .flatMap(i -> executionContextTool.getMessages(i).stream())
                                .flatMap(a -> a.getMessages().stream())
                                .map(ModuleUtils::toString)
                                .filter(v -> v.length() >= string_size)
                                .map(v -> v.substring(0, string_size))
                                .collect(Collectors.toList()));
                break;
            case REGEXP_GROUP_ONE:
                if (patterns == null)
                    break;
                Stream.iterate(0, n -> n + 1)
                        .limit(executionContextTool.countSource())
                        .flatMap(i -> executionContextTool.getMessages(i).stream())
                        .flatMap(a -> a.getMessages().stream())
                        // .filter(m -> ValueType.STRING.equals(m.getType()))
                        .map(ModuleUtils::toString)
                        .forEach(value -> {
                            Matcher matcher = patterns.get(0).matcher(value);
                            while (matcher.find()) {
                                executionContextTool.addMessage(matcher.group(1));
                            }
                        });
                break;
            case ESCAPE_SQL:
                Stream.iterate(0, n -> n + 1)
                        .limit(executionContextTool.countSource())
                        .flatMap(i -> executionContextTool.getMessages(i).stream())
                        .flatMap(a -> a.getMessages().stream())
                        // .filter(m -> ValueType.STRING.equals(m.getType()))
                        .map(ModuleUtils::toString)
                        .forEach(value -> {
                            // executionContextTool.addMessage(StringEscapeUtils.escapeSql(value));
                            executionContextTool.addMessage(StringUtils.replace(value, "'", "''"));
                        });
                break;
            case SIZE:
                Stream.iterate(0, n -> n + 1)
                        .limit(executionContextTool.countSource())
                        .flatMap(i -> executionContextTool.getMessages(i).stream())
                        .flatMap(a -> a.getMessages().stream())
                        // .filter(m -> ValueType.STRING.equals(m.getType()))
                        .map(ModuleUtils::toString)
                        .forEach(value -> executionContextTool.addMessage(value.length()));
                break;
            case IS_BLANK:
                Stream.iterate(0, n -> n + 1)
                        .limit(executionContextTool.countSource())
                        .flatMap(i -> executionContextTool.getMessages(i).stream())
                        .flatMap(a -> a.getMessages().stream())
                        // .filter(m -> ValueType.STRING.equals(m.getType()))
                        .map(ModuleUtils::toString)
                        .filter(String::isBlank)
                        .forEach(value -> executionContextTool.addMessage(1));
                break;
            case IS_NOT_BLANK:
                Stream.iterate(0, n -> n + 1)
                        .limit(executionContextTool.countSource())
                        .flatMap(i -> executionContextTool.getMessages(i).stream())
                        .flatMap(a -> a.getMessages().stream())
                        // .filter(m -> ValueType.STRING.equals(m.getType()))
                        .map(ModuleUtils::toString)
                        .filter(value -> !value.isBlank())
                        .forEach(value -> executionContextTool.addMessage(1));
                break;
            case FILTER:
                if (patterns == null)
                    break;
                Stream.iterate(0, n -> n + 1)
                        .limit(executionContextTool.countSource())
                        .forEach(i -> executionContextTool.getMessages(i).forEach(a -> a.getMessages().stream()
                                // .filter(m -> ValueType.STRING.equals(m.getType()))
                                .map(ModuleUtils::toString)
                                .forEach(value -> {
                                    for (Pattern pattern : patterns) {
                                        if (pattern.matcher(value).find()) {
                                            executionContextTool.addMessage(value);
                                            break;
                                        }
                                    }
                                })));
                break;
            case FILTER_ACTION:
                if (patterns == null)
                    break;
                Stream.iterate(0, n -> n + 1)
                        .limit(executionContextTool.countSource())
                        .flatMap(i -> executionContextTool.getMessages(i).stream())
                        .filter(a -> a.getMessages().stream()
                                // .filter(m -> ValueType.STRING.equals(m.getType()))
                                .map(ModuleUtils::toString)
                                .anyMatch(value -> patterns.stream().anyMatch(pattern -> pattern.matcher(value).find())))
                        .forEach(a -> {
                            executionContextTool.addMessage(a.getMessages().size());
                            executionContextTool.addMessage(a.getMessages().stream().map(IValue::getValue).collect(Collectors.toList()));
                        });
                break;
            case SUBSTRING:
                int start = numberValues.get(0).intValue();
                Integer end = numberValues.size() > 1 ? numberValues.get(1).intValue() : null;
                Stream.iterate(0, n -> n + 1)
                        .limit(executionContextTool.countSource())
                        .flatMap(i -> executionContextTool.getMessages(i).stream())
                        .flatMap(a -> a.getMessages().stream())
                        .forEach(m -> executionContextTool.addMessage(end != null ? StringUtils.substring(ModuleUtils.toString(m), start, end) : StringUtils.substring(ModuleUtils.toString(m), start)));
                break;
            case TRIM:
                Stream.iterate(0, n -> n + 1)
                        .limit(executionContextTool.countSource())
                        .flatMap(i -> executionContextTool.getMessages(i).stream())
                        .flatMap(a -> a.getMessages().stream())
                        .map(ModuleUtils::toString)
                        .forEach(s -> executionContextTool.addMessage(StringUtils.trim(s)));
                break;
            case PLACEHOLDERS_DYNAMIC: {
                LinkedList<IMessage> messages = new LinkedList<>(ModuleUtils.getMessagesJoin(executionContextTool));
                String template = ModuleUtils.getString(messages.poll());
                if (template == null) {
                    executionContextTool.addError("need template");
                    break;
                }
                executionContextTool.addMessage(
                        new MessageFormat(template).format(
                                messages.stream()
                                        .map(ModuleUtils::toString)
                                        .toArray())
                );
                break;
            }
            case PLACEHOLDERS_DYNAMIC_OR_EMPTY: {
                LinkedList<IMessage> messages = new LinkedList<>(ModuleUtils.getMessagesJoin(executionContextTool));
                String template = ModuleUtils.getString(messages.poll());
                if (template == null) {
                    executionContextTool.addError("need template");
                    break;
                }
                String result = new MessageFormat(template).format(
                        messages.stream()
                                .map(ModuleUtils::toString)
                                .toArray());
                if (!result.matches("(?s).*\\{\\d+\\}.*"))
                    executionContextTool.addMessage(result);
                break;
            }
        }

    }

    @Override
    public void stop(ConfigurationTool configurationTool) throws ModuleException {
        type = null;
        value = null;
        values = null;
        patterns = null;
    }

}
