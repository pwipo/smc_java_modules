package ru.smcsystem.modules.module.predicators;

import ru.seits.projects.lineardb.IElement;

import java.util.List;
import java.util.function.Predicate;

public class MyPredicateIn implements Predicate<IElement> {
    private List<String> fields;
    private String fieldName;
    private Object value;

    public MyPredicateIn(List<String> fields, String fieldName, Object value) {
        this.fields = fields;
        this.fieldName = fieldName;
        this.value = value;
    }

    public boolean test(IElement e) {
        Object fieldValue = MyPredicateUtils.getFieldValue(e, fields, fieldName);
        if (fieldValue == null || !(value instanceof List))
            return false;
        return ((List<Object>) value).stream().anyMatch(fieldValue::equals);
    }

}
