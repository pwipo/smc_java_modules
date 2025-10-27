package ru.smcsystem.modules.module.predicators;

import ru.seits.projects.lineardb.IElement;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public class MyPredicateNumber implements Predicate<IElement> {
    private List<String> fields;
    private NumberCompareType numberCompareType;
    private String fieldName;
    private Object value;

    public MyPredicateNumber(List<String> fields, NumberCompareType numberCompareType, String fieldName, Object value) {
        this.fields = fields;
        this.numberCompareType = numberCompareType;
        this.fieldName = fieldName;
        this.value = value;
    }

    public boolean test(IElement e) {
        Objects.requireNonNull(numberCompareType);
        Object fieldValue = MyPredicateUtils.getFieldValue(e, fields, fieldName);
        switch (numberCompareType) {
            case GREATER_OR_EQUAL:
            case LESS_OR_EQUAL:
            case GREATER:
            case LESS: {
                Object o1 = fieldValue;
                Object o2 = value;
                if (Objects.equals(o1, o2) && (numberCompareType == NumberCompareType.LESS_OR_EQUAL || numberCompareType == NumberCompareType.GREATER_OR_EQUAL))
                    return true;
                if (!(o1 instanceof Number) || !(o2 instanceof Number))
                    return false;
                if (o1 instanceof Double) {
                    if (numberCompareType == NumberCompareType.LESS) {
                        return ((Number) o1).doubleValue() < ((Number) o2).doubleValue();
                    } else if (numberCompareType == NumberCompareType.LESS_OR_EQUAL) {
                        return ((Number) o1).doubleValue() <= ((Number) o2).doubleValue();
                    } else if (numberCompareType == NumberCompareType.GREATER) {
                        return ((Number) o1).doubleValue() > ((Number) o2).doubleValue();
                    } else if (numberCompareType == NumberCompareType.GREATER_OR_EQUAL) {
                        return ((Number) o1).doubleValue() >= ((Number) o2).doubleValue();
                    }
                } else {
                    if (numberCompareType == NumberCompareType.LESS) {
                        return ((Number) o1).longValue() < ((Number) o2).longValue();
                    } else if (numberCompareType == NumberCompareType.LESS_OR_EQUAL) {
                        return ((Number) o1).longValue() <= ((Number) o2).longValue();
                    } else if (numberCompareType == NumberCompareType.GREATER) {
                        return ((Number) o1).longValue() > ((Number) o2).longValue();
                    } else if (numberCompareType == NumberCompareType.GREATER_OR_EQUAL) {
                        return ((Number) o1).longValue() >= ((Number) o2).longValue();
                    }
                }
                return false;
            }
            default:
                throw new IllegalArgumentException("Unsupported type: " + numberCompareType);
        }
    }

    public enum NumberCompareType {
        LESS, GREATER, LESS_OR_EQUAL, GREATER_OR_EQUAL
    }

}
