package ru.smcsystem.modules.module.predicators;

import ru.seits.projects.lineardb.IElement;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public class MyPredicateEquals implements Predicate<IElement> {
    private List<String> fields;
    private String fieldName;
    private Object value;

    public MyPredicateEquals(List<String> fields, String fieldName, Object value) {
        this.fields = fields;
        this.fieldName = fieldName;
        this.value = value;
    }

    public boolean test(IElement e) {
        Object fieldValue = MyPredicateUtils.getFieldValue(e, fields, fieldName);
        return Objects.equals(fieldValue, value);
    }

}
