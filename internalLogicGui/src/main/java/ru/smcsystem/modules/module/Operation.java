package ru.smcsystem.modules.module;

import ru.smcsystem.api.dto.IValue;
import ru.smcsystem.api.dto.ObjectArray;
import ru.smcsystem.api.dto.ObjectElement;
import ru.smcsystem.api.dto.ObjectField;
import ru.smcsystem.api.enumeration.ObjectType;
import ru.smcsystem.api.exceptions.ModuleException;
import ru.smcsystem.api.tools.ConfigurationTool;
import ru.smcsystem.api.tools.execution.ExecutionContextTool;
import ru.smcsystem.smc.utils.ModuleUtils;

import java.util.*;
import java.util.stream.Collectors;

public class Operation {
    public static final String VAR_NAME_SOURCE = "$src";
    public static final String VAR_NAME_EC = "$ec";
    public static final String VAR_NAME_RESULT = "$result";
    public static final String VAR_NAME_PARAM = "$param";
    private final OperationType type;
    private final List<Object> values;
    private final PredicateType predicateType;
    private final Integer ecId;
    private final String name;
    private final List<Operation> childs;

    public Operation(ConfigurationTool configurationTool, Shape shape, List<Shape> shapes, List<Shape> parents) {
        this.name = shape.getName();
        // String text = shape.getText().trim();
        // if (!shape.getDescription().isBlank())
        //     text = text + "\n" + shape.getDescription().trim();
        String text = (shape.getDescription().isBlank() ? shape.getText() : shape.getDescription()).trim();
        String[] textLines = text.split("\n");

        this.type = shape.getType() == ShapeType.rhombus ? OperationType.IF :
                (shape.getType() == ShapeType.rectangle ? OperationType./*CYCLE*/CALL :
                        shape.getType() == ShapeType.circle ? OperationType.RESULT : null);
        // (textLines[0].trim().toLowerCase().startsWith(VAR_NAME_RESULT) ? OperationType.RESULT :
        //         (textLines[0].trim().toLowerCase().startsWith(VAR_NAME_EC) ? OperationType.CALL : null)));
        if (this.type == null)
            throw new ModuleException("Operation type unrecognized " + text + " " + this.name);
        switch (type) {
            case IF: {
                if (textLines.length < 3)
                    throw new ModuleException("need 3 string in description" + " " + this.name);
                // valuePaths = List.of(Arrays.asList(split[0].trim().split("\\.")), Arrays.asList(split[2].trim().split("\\.")));
                values = new ArrayList<>();
                values.add(parseValue(textLines[0]));
                values.add(parseValue(textLines[2]));
                ecId = null;
                predicateType = PredicateType.parse(textLines[1].trim()).orElseThrow(() -> new ModuleException("predicate not found" + " " + this.name));
                break;
            }
            /*
            case CYCLE: {
                if (textLines.length < 3) {
                    // valuePaths = List.of(Arrays.asList(split[0].trim().split("\\.")));
                    values = new ArrayList<>();
                    values.add(parseValue(textLines[0]));
                    predicateType = null;
                } else {
                    // valuePaths = List.of(Arrays.asList(split[0].trim().split("\\.")), Arrays.asList(split[2].trim().split("\\.")));
                    values = new ArrayList<>();
                    values.add(parseValue(textLines[0]));
                    values.add(parseValue(textLines[2]));
                    predicateType = PredicateType.parse(textLines[1].trim()).orElseThrow(() -> new ModuleException("predicate not found" + " " + this.name));
                }
                ecId = null;
                break;
            }
            */
            case CALL: {
                values = Arrays.stream(textLines)
                        .skip(1)
                        .filter(s -> !s.isBlank())
                        .map(this::parseValue)
                        // .map(s -> Arrays.asList(s.trim().split("\\.")))
                        .collect(Collectors.toList());
                // String strOne = textLines[0].trim();
                // if (!strOne.toLowerCase().startsWith(VAR_NAME_EC))
                //     throw new ModuleException("wrong ecId " + strOne + " " + this.name);
                // ecId = Integer.parseInt(strOne.substring(VAR_NAME_EC.length())); //$ec
                ecId = Integer.parseInt(textLines[0].trim());
                predicateType = null;
                break;
            }
            case RESULT: {
                values = Arrays.stream(textLines)
                        .filter(s -> !s.isBlank())
                        .map(this::parseValue)
                        // .map(s -> Arrays.asList(s.trim().split("\\.")))
                        .collect(Collectors.toList());
                ecId = null;
                predicateType = null;
                break;
            }
            default:
                values = null;
                ecId = null;
                predicateType = null;
        }
        childs = shapes.stream()
                .filter(s -> s.getType() == ShapeType.line)
                .filter(s -> isPointInShape(s.getX(), s.getY(), shape) || isPointInShape(s.getPoint2X(), s.getPoint2Y(), shape))
                .map(s -> shapes.stream()
                        .filter(s2 -> s2.getType() != ShapeType.line)
                        .filter(s2 -> !s2.equals(s) && !s2.equals(shape) && !parents.contains(s2) && Objects.equals(s2.getParentName(), s.getParentName()))
                        .filter(s2 -> isPointInShape(s.getX(), s.getY(), s2) || isPointInShape(s.getPoint2X(), s.getPoint2Y(), s2))
                        .min(Comparator.comparing(s2 -> s2.getWidth() + s2.getHeight())).orElse(null))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(s -> s.getX() * s.getY()))
                .map(s -> {
                    try {
                        parents.add(shape);
                        return new Operation(configurationTool, s, shapes, parents);
                    } catch (Exception e) {
                        configurationTool.loggerWarn("error " + s.getName() + " " + ModuleUtils.getErrorMessageOrClassName(e));
                        configurationTool.loggerTrace(ModuleUtils.getStackTraceAsString(e));
                        return null;
                    } finally {
                        parents.remove(shape);
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private boolean isPointInShape(int x, int y, Shape shape) {
        return x >= shape.getX() && x <= (shape.getX() + shape.getWidth()) &&
                y >= shape.getY() && y <= (shape.getY() + shape.getHeight());
    }

    private Object parseValue(String value) {
        value = value.trim();
        if (value.toLowerCase().startsWith(VAR_NAME_SOURCE) || value.toLowerCase().startsWith(VAR_NAME_EC) || value.toLowerCase().startsWith(VAR_NAME_PARAM)) {
            return Arrays.asList(value.split("\\."));
        } else if (value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        } else if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            return Boolean.parseBoolean(value);
        } else if (value.equals("NULL")) {
            return null;
        } else {
            try {
                if (value.contains(".")) {
                    return Double.parseDouble(value);
                } else {
                    return Long.parseLong(value);
                }
            } catch (Exception e) {
                throw new ModuleException("unable to parse " + value + " " + this.name);
            }
        }
        return value;
    }

    public void execute(ConfigurationTool configurationTool, ExecutionContextTool executionContextTool, Map<String, List<Object>> map, List<Object> paramLst) throws ModuleException {
        switch (type) {
            case IF: {
                List<Object> pL1 = getValue(map, paramLst, values.get(0));
                List<Object> pL2 = getValue(map, paramLst, values.get(1));
                Object p1 = !pL1.isEmpty() ? pL1.get(0) : null;
                Object p2 = !pL2.isEmpty() ? pL2.get(0) : null;
                ArrayList<Object> lst = new ArrayList<>();
                lst.add(p1);
                lst.add(p2);
                if (doPredicate(p1, p2, predicateType)) {
                    if (!childs.isEmpty())
                        childs.get(0).execute(configurationTool, executionContextTool, map, lst);
                } else {
                    if (childs.size() > 1)
                        childs.get(1).execute(configurationTool, executionContextTool, map, lst);
                }
                break;
            }
            /*
            case CYCLE: {
                List<Object> params = getValue(map, paramLst, values.get(0));
                List<Object> pL2 = values.size() > 1 ? getValue(map, paramLst, values.get(1)) : null;
                Object p2 = pL2 != null && !pL2.isEmpty() ? pL2.get(0) : null;
                for (Object param : params) {
                    if (p2 != null && predicateType != null && !doPredicate(param, p2, predicateType))
                        break;
                    childs.forEach(child -> child.execute(configurationTool, executionContextTool, map, List.of(param)));
                }
                break;
            }
            */
            case CALL: {
                List<Object> params = values.stream()
                        .flatMap(v -> getValue(map, paramLst, v).stream())
                        .collect(Collectors.toList());
                if (executionContextTool.getFlowControlTool().countManagedExecutionContexts() <= ecId) {
                    executionContextTool.addError("not enough contexts");
                    break;
                }
                map.remove(VAR_NAME_EC + ecId);
                ModuleUtils.executeAndGetMessages(executionContextTool, ecId, params).ifPresent(
                        l -> map.put(VAR_NAME_EC + ecId, l.stream().map(IValue::getValue).collect(Collectors.toList())));
                childs.forEach(child -> child.execute(configurationTool, executionContextTool, map, map.get(VAR_NAME_EC + ecId)));
                break;
            }
            case RESULT: {
                List<Object> result = values.stream()
                        .flatMap(v -> getValue(map, paramLst, v).stream())
                        .collect(Collectors.toList());
                result.forEach(v -> map.get(VAR_NAME_RESULT).add(v));
                childs.forEach(child -> child.execute(configurationTool, executionContextTool, map, result));
                break;
            }
        }
    }

    private List<Object> getValue(Map<String, List<Object>> map, List<Object> paramLst, Object value) {
        if (value instanceof List) {
            List<String> path = (List) value;
            List<Object> curLst = path.toString().equals(VAR_NAME_PARAM) ? paramLst : map.get(path.get(0));
            if (curLst == null)
                return List.of();
            if (path.size() > 1) {
                int i = Integer.parseInt(path.get(1));
                if ((i > -1 && curLst.size() > i) || (i < 0 && curLst.size() + i >= 0)) {
                    Object v = i > -1 ? curLst.get(i) : curLst.get(curLst.size() + i);
                    if (path.size() > 2 && v instanceof ObjectArray) {
                        String objPath = String.join(".", path.subList(2, path.size()));
                        return ModuleUtils.findFields((ObjectArray) v, List.of(objPath)).stream()
                                .flatMap(Collection::stream)
                                .map(ObjectField::getValue)
                                .collect(Collectors.toList());
                    } else if ((v instanceof String) || (v instanceof Number) || (v instanceof Boolean)) {
                        return List.of(v);
                    }
                }
            } else {
                return curLst;
            }
            return List.of();
        }
        return value != null ? List.of(value) : List.of();
    }

    private boolean doPredicate(Object p1, Object p2, PredicateType predicateType) {
        switch (predicateType) {
            case EQUALS: {
                boolean b = Objects.equals(p1, p2);
                if (b)
                    return true;
                if (p1 instanceof Number && p2 instanceof Number) {
                    if (p1 instanceof Double || p2 instanceof Double || p1 instanceof Float || p2 instanceof Float) {
                        return Objects.equals(((Number) p1).doubleValue(), ((Number) p2).doubleValue());
                    } else {
                        return Objects.equals(((Number) p1).longValue(), ((Number) p2).longValue());
                    }
                } else if (p1 instanceof String || p2 instanceof String) {
                    return Objects.equals(p1.toString(), p2.toString());
                } else {
                    return false;
                }
            }
            case NOT_EQUALS: {
                boolean b = !Objects.equals(p1, p2);
                if (!b)
                    return false;
                if (p1 instanceof Number && p2 instanceof Number) {
                    if (p1 instanceof Double || p2 instanceof Double || p1 instanceof Float || p2 instanceof Float) {
                        return !Objects.equals(((Number) p1).doubleValue(), ((Number) p2).doubleValue());
                    } else {
                        return !Objects.equals(((Number) p1).longValue(), ((Number) p2).longValue());
                    }
                } else if (p1 instanceof String || p2 instanceof String) {
                    return !Objects.equals(p1.toString(), p2.toString());
                } else {
                    return true;
                }
            }
            case LESS:
                if (!(p1 instanceof Number) || !(p2 instanceof Number))
                    return false;
                if (p1 instanceof Double || p2 instanceof Double || p1 instanceof Float || p2 instanceof Float) {
                    return ((Number) p1).doubleValue() < ((Number) p2).doubleValue();
                } else {
                    return ((Number) p1).longValue() < ((Number) p2).longValue();
                }
            case GREATER:
                if (!(p1 instanceof Number) || !(p2 instanceof Number))
                    return false;
                if (p1 instanceof Double || p2 instanceof Double || p1 instanceof Float || p2 instanceof Float) {
                    return ((Number) p1).doubleValue() > ((Number) p2).doubleValue();
                } else {
                    return ((Number) p1).longValue() > ((Number) p2).longValue();
                }
            case LESS_OR_EQUAL:
                if (!(p1 instanceof Number) || !(p2 instanceof Number))
                    return false;
                if (p1 instanceof Double || p2 instanceof Double || p1 instanceof Float || p2 instanceof Float) {
                    return ((Number) p1).doubleValue() <= ((Number) p2).doubleValue();
                } else {
                    return ((Number) p1).longValue() <= ((Number) p2).longValue();
                }
            case GREATER_OR_EQUAL:
                if (!(p1 instanceof Number) || !(p2 instanceof Number))
                    return false;
                if (p1 instanceof Double || p2 instanceof Double || p1 instanceof Float || p2 instanceof Float) {
                    return ((Number) p1).doubleValue() >= ((Number) p2).doubleValue();
                } else {
                    return ((Number) p1).longValue() >= ((Number) p2).longValue();
                }
            case CONTAINS:
                if (p1 == null || p2 == null)
                    return false;
                if (p1 instanceof ObjectArray) {
                    ObjectArray objectArray = (ObjectArray) p1;
                    for (int i = 0; i < objectArray.size(); i++) {
                        if (Objects.equals(objectArray.get(i), p2))
                            return true;
                    }
                    return false;
                } else {
                    return p1.toString().contains(p2.toString());
                }
            case START_WITH:
                if (p1 == null || p2 == null)
                    return false;
                return p1.toString().startsWith(p2.toString());
            case END_WITH:
                if (p1 == null || p2 == null)
                    return false;
                return p1.toString().endsWith(p2.toString());
        }
        return true;
    }

    public ObjectElement toObject() {
        ObjectElement element = new ObjectElement(
                new ObjectField("name", name),
                new ObjectField("type", type.name())
        );
        switch (type) {
            case IF:
                element.getFields().add(new ObjectField("predicateType", predicateType != null ? predicateType.name() : null));
                break;
            case CALL:
                element.getFields().add(new ObjectField("ecId", ObjectType.INTEGER, ecId));
                break;
            case RESULT:
                break;
        }
        if (!values.isEmpty()) {
            element.getFields().add(new ObjectField("values", new ObjectArray(
                    values.stream()
                            .map(o -> o instanceof List ? new ObjectArray((List) o, ObjectType.STRING) : new ObjectArray(List.of(o), ObjectType.VALUE_ANY))
                            .collect(Collectors.toList()),
                    ObjectType.OBJECT_ARRAY)));
        }
        if (!childs.isEmpty()) {
            element.getFields().add(new ObjectField("childs", new ObjectArray(
                    childs.stream().map(Operation::toObject).collect(Collectors.toList()),
                    ObjectType.OBJECT_ELEMENT)));
        }
        return element;
    }

}
