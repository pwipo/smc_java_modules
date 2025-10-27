package ru.smcsystem.modules.module.predicators;

import ru.seits.projects.lineardb.IElement;
import ru.smcsystem.api.dto.ObjectArray;
import ru.smcsystem.api.dto.ObjectElement;
import ru.smcsystem.api.dto.ObjectField;
import ru.smcsystem.smc.utils.ModuleUtils;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MyPredicateUtils {
    public static Predicate<IElement> predicateOr(Predicate<IElement> predicateOne, Predicate<IElement> predicateTwo) {
        return e -> predicateOne.test(e) || predicateTwo.test(e);
    }

    public static Predicate<IElement> predicateAnd(Predicate<IElement> predicateOne, Predicate<IElement> predicateTwo) {
        return e -> predicateOne.test(e) && predicateTwo.test(e);
    }

    public static Predicate<IElement> predicateNot(Predicate<IElement> predicate) {
        return e -> !predicate.test(e);
    }

    public static Object getFieldValue(IElement e, List<String> fields, String fieldName) {
        Objects.requireNonNull(e);
        Objects.requireNonNull(fieldName);
        Object targetValue = null;
        int indexOf = fields.indexOf(fieldName);
        if (indexOf == 0) {
            targetValue = e.getId();
        } else if (indexOf == 1) {
            targetValue = e.getDate();
        } else if (indexOf > 1 && e.getAdditionalData().size() > (indexOf - 2)) {
            targetValue = e.getAdditionalData().get(indexOf - 2);
        }
        return targetValue;
    }

    public static Optional<Predicate<IElement>> parse(ObjectElement element, List<String> fields/*, LinkedList<Object> placeHolderValues*/) {
        // Objects.requireNonNull(element);
        if (element == null)
            return Optional.of(e -> true);
        return element.findFieldIgnoreCase("t")
                .flatMap(f -> PredicateType.parse(ModuleUtils.toString(f).toLowerCase()))
                .map(t -> {
                    switch (t) {
                        case EQUALS: {
                            String fieldName = element.findFieldIgnoreCase("f").map(ModuleUtils::toString).orElse(null);
                            if (fieldName == null)
                                return null;
                            Object value = element.findFieldIgnoreCase("v")
                                    // .map(v -> v.getType() == ObjectType.STRING ?
                                    //         (v.getValue() == "?" && !placeHolderValues.isEmpty() ? placeHolderValues.poll() : v.getValue()) :
                                    //         v.getValue())
                                    .map(ObjectField::getValue)
                                    .orElse(null);
                            return new MyPredicateEquals(fields, fieldName, value);
                        }
                        case LESS:
                        case GREATER:
                        case LESS_OR_EQUAL:
                        case GREATER_OR_EQUAL: {
                            String fieldName = element.findFieldIgnoreCase("f").map(ModuleUtils::toString).orElse(null);
                            if (fieldName == null)
                                return null;
                            Object value = element.findFieldIgnoreCase("v")
                                    // .map(v -> v.getType() == ObjectType.STRING ?
                                    //         (v.getValue() == "?" && !placeHolderValues.isEmpty() ? placeHolderValues.poll() : v.getValue()) :
                                    //         v.getValue())
                                    .map(ObjectField::getValue)
                                    .orElse(null);
                            return new MyPredicateNumber(fields, MyPredicateNumber.NumberCompareType.valueOf(t.name()), fieldName, value);
                        }
                        case IN: {
                            String fieldName = element.findFieldIgnoreCase("f").map(ModuleUtils::toString).orElse(null);
                            if (fieldName == null)
                                return null;
                            ObjectArray arr = element.findFieldIgnoreCase("v").map(ModuleUtils::getObjectArray).orElse(null);
                            if (arr == null)
                                return null;
                            List<Object> lst = Stream.iterate(0, i -> i + 1)
                                    .limit(arr.size())
                                    .map(arr::get)
                                    .collect(Collectors.toList());
                            return new MyPredicateIn(fields, fieldName, lst);
                        }
                        case AND: {
                            Predicate<IElement> p1 = element.findFieldIgnoreCase("p1").map(ModuleUtils::getObjectElement)
                                    .flatMap(e -> parse(e, fields/*, placeHolderValues*/)).orElse(null);
                            Predicate<IElement> p2 = element.findFieldIgnoreCase("p2").map(ModuleUtils::getObjectElement)
                                    .flatMap(e -> parse(e, fields/*, placeHolderValues*/)).orElse(null);
                            if (p1 == null || p2 == null)
                                return null;
                            return predicateAnd(p1, p2);
                        }
                        case OR: {
                            Predicate<IElement> p1 = element.findFieldIgnoreCase("p1").map(ModuleUtils::getObjectElement)
                                    .flatMap(e -> parse(e, fields/*, placeHolderValues*/)).orElse(null);
                            Predicate<IElement> p2 = element.findFieldIgnoreCase("p2").map(ModuleUtils::getObjectElement)
                                    .flatMap(e -> parse(e, fields/*, placeHolderValues*/)).orElse(null);
                            if (p1 == null || p2 == null)
                                return null;
                            return predicateOr(p1, p2);
                        }
                        case NOT:
                            return element.findFieldIgnoreCase("p1").map(ModuleUtils::getObjectElement)
                                    .flatMap(e -> parse(e, fields/*, placeHolderValues*/))
                                    .map(MyPredicateUtils::predicateNot)
                                    .orElse(null);
                    }
                    return null;
                });
    }

    private enum PredicateType {
        EQUALS(Set.of("eq", "=")),
        LESS(Set.of("lt", "<")),
        GREATER(Set.of("gt", ">")),
        LESS_OR_EQUAL(Set.of("le", "<=")),
        GREATER_OR_EQUAL(Set.of("ge", ">=")),
        IN(Set.of("in")),
        AND(Set.of("and", "&&")),
        OR(Set.of("or", "||")),
        NOT(Set.of("not", "!"));

        private Set<String> names;

        PredicateType(Set<String> names) {
            this.names = names;
        }

        public static Optional<PredicateType> parse(String name) {
            return Arrays.stream(PredicateType.values()).filter(t -> t.names.contains(name)).findFirst();
        }
    }


}
