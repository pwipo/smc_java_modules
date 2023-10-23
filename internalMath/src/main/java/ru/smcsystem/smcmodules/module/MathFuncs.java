package ru.smcsystem.smcmodules.module;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.apache.commons.math3.analysis.function.Sigmoid;
import ru.smcsystem.api.dto.IMessage;
import ru.smcsystem.api.exceptions.ModuleException;
import ru.smcsystem.api.module.Module;
import ru.smcsystem.api.tools.ConfigurationTool;
import ru.smcsystem.api.tools.execution.ExecutionContextTool;
import ru.smcsystem.smc.utils.ModuleUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MathFuncs implements Module {

    enum Type {
        SIGMOID,
        SIGMOID_DERIVATIVE,
        SUM,
        SUM_BY_PARAM,
        ABS,
        MIN,
        MIN_INDEX,
        MAX,
        MAX_INDEX,
        MULTIPLY,
        MULTIPLY_BY_PARAM,
        AVERAGE,
        RMS,
        DIFFERENCE,
        DIFFERENCE_BY_PARAM,
        COUNT,
        EXP4J,
        MIN_MAX,
    }

    private Type type;
    private Double param;
    private Double param2;

    private Sigmoid sigmoid;

    @Override
    public void start(ConfigurationTool configurationTool) throws ModuleException {
        type = Type.valueOf((String) configurationTool.getSetting("type").orElseThrow(() -> new ModuleException("type setting")).getValue());
        param = (Double) configurationTool.getSetting("param").orElseThrow(() -> new ModuleException("param setting")).getValue();
        param2 = (Double) configurationTool.getSetting("param2").orElseThrow(() -> new ModuleException("param2 setting")).getValue();
        sigmoid = new Sigmoid();
    }

    @Override
    public void update(ConfigurationTool configurationTool) throws ModuleException {
        stop(configurationTool);
        start(configurationTool);
    }

    @Override
    public void process(ConfigurationTool configurationTool, ExecutionContextTool executionContextTool) throws ModuleException {
        if (executionContextTool.countSource() <= 0)
            return;
        List<IMessage> messages = Stream.iterate(0, i -> i + 1)
                .limit(executionContextTool.countSource())
                .flatMap(i -> executionContextTool.getMessages(i).stream())
                .flatMap(action -> action.getMessages().stream())
                .collect(Collectors.toList());
        List<Number> values = messages.stream()
                // .filter(m -> !ValueType.BYTES.equals(m.getType()) && !ValueType.STRING.equals(m.getType()))
                .filter(ModuleUtils::isNumber)
                .map(ModuleUtils::getNumber)
                .collect(Collectors.toList());
        Type type = Objects.equals(executionContextTool.getType(), "default") ? this.type : Type.valueOf(executionContextTool.getType().toUpperCase());
        if (values.isEmpty() && (!type.equals(Type.COUNT) && !type.equals(Type.EXP4J)))
            return;

        switch (type) {
            case SIGMOID:
                values.forEach(value -> executionContextTool.addMessage(sigmoid.value(param * value.doubleValue())));
                break;
            case SIGMOID_DERIVATIVE:
                values.forEach(value -> executionContextTool.addMessage(sigmoid.derivative().value(param * value.doubleValue())));
                break;
            case SUM: {
                executionContextTool.addMessage(List.of(sum(values)));
                break;
            }
            case SUM_BY_PARAM: {
                values.forEach(n -> executionContextTool.addMessage(List.of(sum(n, param, 1))));
                break;
            }
            case ABS:
                values.forEach(value -> {
                    if (value instanceof Byte) {
                        value = (byte) Math.abs(value.byteValue());
                    } else if (value instanceof Short) {
                        value = (short) Math.abs(value.shortValue());
                    } else if (value instanceof Integer) {
                        value = (int) Math.abs(value.intValue());
                    } else if (value instanceof Long) {
                        value = (long) Math.abs(value.longValue());
                    } else if (value instanceof BigInteger) {
                        value = ((BigInteger) value).abs();
                    } else if (value instanceof Float) {
                        value = (float) Math.abs(value.floatValue());
                    } else if (value instanceof Double) {
                        value = (double) Math.abs(value.doubleValue());
                    } else if (value instanceof BigDecimal) {
                        value = ((BigDecimal) value).abs();
                    }
                    executionContextTool.addMessage(List.of(value));
                });
                break;
            case MIN:
            case MIN_INDEX: {
                ArrayList<Number> sortedValues = new ArrayList<>(values);
                sortedValues.sort((o1, o2) -> {
                                        /*
                                        Double d1 = (o1.getValue() == null) ? Double.POSITIVE_INFINITY : ((Number) o1.getValue()).doubleValue();
                                        Double d2 = (o2.getValue() == null) ? Double.POSITIVE_INFINITY : ((Number) o2.getValue()).doubleValue();
                                        return d1.compareTo(d2);
                                        */
                    if (o1 instanceof Long && o2 instanceof Long) {
                        return Long.compare(o1.longValue(), o2.longValue());
                    } else {
                        return Double.compare(o1.doubleValue(), o2.doubleValue());
                    }
                });
                Number number = sortedValues.get(0);
                executionContextTool.addMessage(List.of(Type.MIN.equals(type) ? number : values.indexOf(number)));
                break;
            }
            case MAX:
            case MAX_INDEX: {
                ArrayList<Number> sortedValues = new ArrayList<>(values);
                sortedValues.sort((o1, o2) -> {
                    if (o1 instanceof Long && o2 instanceof Long) {
                        return Long.compare(o1.longValue(), o2.longValue());
                    } else {
                        return Double.compare(o1.doubleValue(), o2.doubleValue());
                    }
                });
                Number number = sortedValues.get(sortedValues.size() - 1);
                executionContextTool.addMessage(List.of(Type.MAX.equals(type) ? number : values.indexOf(number)));
                break;
            }
            case MULTIPLY:
                if (values.stream().anyMatch(v -> v instanceof Double || v instanceof Float)) {
                    executionContextTool.addMessage(values.stream().mapToDouble(Number::doubleValue).reduce((r, e) -> r * e).orElse(0.0));
                } else {
                    executionContextTool.addMessage(values.stream().mapToDouble(Number::longValue).reduce((r, e) -> r * e).orElse(0.0));
                }
                break;
            case MULTIPLY_BY_PARAM:
                values.stream().mapToDouble(Number::doubleValue).forEach(v ->
                        executionContextTool.addMessage(v * param));
                break;
            case AVERAGE: {
                Number sum = sum(values);
                if (sum instanceof Double) {
                    executionContextTool.addMessage(sum.doubleValue() / values.size());
                } else {
                    executionContextTool.addMessage((double) (sum.longValue() / values.size()));
                }
                break;
            }
            case RMS:
                executionContextTool.addMessage(List.of(rootMeanSquare(values)));
                break;
            case DIFFERENCE:
                executionContextTool.addMessage(List.of(difference(values)));
                break;
            case DIFFERENCE_BY_PARAM:
                values.stream().mapToDouble(Number::doubleValue).forEach(v ->
                        executionContextTool.addMessage(v - param));
                break;
            case COUNT:
                executionContextTool.addMessage(messages.size());
                break;
            case EXP4J: {
                List<String> strings = messages.stream()
                        .filter(ModuleUtils::isString)
                        .map(ModuleUtils::getString)
                        .collect(Collectors.toList());
                if (strings.isEmpty())
                    return;
                String expression = strings.get(0);
                ExpressionBuilder expressionBuilder = new ExpressionBuilder(expression);
                int countVariables = strings.size() - 1;
                if (countVariables > 0) {
                    for (int i = 0; i < countVariables; i++)
                        expressionBuilder.variable(strings.get(i + 1));
                    Expression e = expressionBuilder.build();
                    for (int j = 0; j + countVariables - 1 < values.size(); j = j + countVariables) {
                        for (int i = 0; i < countVariables; i++)
                            e.setVariable(strings.get(i + 1), values.get(j + i).doubleValue());
                        executionContextTool.addMessage(BigDecimal.valueOf(e.evaluate()).stripTrailingZeros().toPlainString());
                    }
                } else {
                    Expression e = expressionBuilder.build();
                    executionContextTool.addMessage(BigDecimal.valueOf(e.evaluate()).stripTrailingZeros().toPlainString());
                }
                break;
            }
            case MIN_MAX: {
                executionContextTool.addMessage(
                        values.stream()
                                .map(n -> {
                                    if (n instanceof Long) {
                                        return Math.min(Math.max(n.longValue(), param), param2);
                                    } else {
                                        return Math.min(Math.max(n.doubleValue(), param), param2);
                                    }
                                })
                                .collect(Collectors.toList()));
                break;
            }
        }
    }

    @Override
    public void stop(ConfigurationTool configurationTool) throws ModuleException {
        sigmoid = null;
        type = null;
        param = null;
        param2 = null;
    }

    private Number sum(List<Number> values/*, int multiplier*/) {
        return values.stream().reduce((r, e) -> sum(r, e, 1)).orElse(0L);
    }

    private Number sum(Number r, Number e, int multiplier) {
        if (r instanceof Double || e instanceof Double) {
            r = r.doubleValue() + e.doubleValue() * multiplier;
        } else if (r instanceof Float || e instanceof Float) {
            if (r instanceof Long || e instanceof Long) {
                r = r.doubleValue() + e.doubleValue() * multiplier;
            } else {
                r = r.floatValue() + e.floatValue() * multiplier;
            }
        } else if (r instanceof Long || e instanceof Long) {
            r = r.longValue() + e.longValue() * multiplier;
        } else {
            r = r.intValue() + e.intValue() * multiplier;
        }
        return r;
    }

    private List<Number> difference(List<Number> values/*, int multiplier*/) {
        List<Number> list = new ArrayList<>();
        for (int i = 0; i < values.size() - 1; i = i + 2) {
            Number r = values.get(i);
            Number e = values.get(i + 1);
            list.add(sum(r, e, 1));
        }
        return list;
    }

    // calc mean square error (Root Mean Square, RMS)
    private Number rootMeanSquare(List<Number> values) {
        List<Number> povValues = values.stream()
                .map(e -> (Number) Math.pow(e.doubleValue(), 2)).collect(Collectors.toList());
        return Math.sqrt(sum(povValues).doubleValue() / Math.max(1, values.size() - 1));
    }

}
