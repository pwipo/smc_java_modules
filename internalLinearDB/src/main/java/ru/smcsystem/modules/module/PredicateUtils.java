package ru.smcsystem.modules.module;

import ru.seits.projects.lineardb.IElement;
import ru.smcsystem.api.dto.ObjectArray;
import ru.smcsystem.api.dto.ObjectElement;
import ru.smcsystem.api.dto.ObjectField;
import ru.smcsystem.smc.utils.ModuleUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PredicateUtils {

    public static List<IElement> find(ObjectElement element, List<String> fields, DBIndex dbIndex) {
        return findPrv(element, fields, dbIndex).distinct().collect(Collectors.toList());
    }

    private static Stream<IElement> findPrv(ObjectElement element, List<String> fields, DBIndex dbIndex) {
        if (element == null || element.getFields().isEmpty())
            return dbIndex.getIndexElements().stream();
        if (fields == null || dbIndex == null)
            return Stream.of();
        return element.findFieldIgnoreCase("t")
                .flatMap(f -> PredicateType.parse(ModuleUtils.toString(f).toLowerCase()))
                .map(t -> {
                    switch (t) {
                        case EQUALS: {
                            String fieldName = element.findFieldIgnoreCase("f").map(ModuleUtils::toString).orElse(null);
                            if (fieldName == null)
                                return null;
                            Object value = element.findFieldIgnoreCase("v")
                                    .map(ObjectField::getValue)
                                    .orElse(null);
                            return dbIndex.findEquals(fields.indexOf(fieldName), value);
                        }
                        case NOT_EQUALS: {
                            String fieldName = element.findFieldIgnoreCase("f").map(ModuleUtils::toString).orElse(null);
                            if (fieldName == null)
                                return null;
                            Object value = element.findFieldIgnoreCase("v")
                                    .map(ObjectField::getValue)
                                    .orElse(null);
                            return dbIndex.findNotEquals(fields.indexOf(fieldName), value);
                        }
                        case LESS:
                        case LESS_OR_EQUAL: {
                            String fieldName = element.findFieldIgnoreCase("f").map(ModuleUtils::toString).orElse(null);
                            if (fieldName == null)
                                return null;
                            Object value = element.findFieldIgnoreCase("v")
                                    .map(ObjectField::getValue)
                                    .orElse(null);
                            return dbIndex.findLess(fields.indexOf(fieldName), value, t == PredicateType.LESS_OR_EQUAL);
                        }
                        case GREATER:
                        case GREATER_OR_EQUAL: {
                            String fieldName = element.findFieldIgnoreCase("f").map(ModuleUtils::toString).orElse(null);
                            if (fieldName == null)
                                return null;
                            Object value = element.findFieldIgnoreCase("v")
                                    .map(ObjectField::getValue)
                                    .orElse(null);
                            return dbIndex.findGreater(fields.indexOf(fieldName), value, t == PredicateType.GREATER_OR_EQUAL);
                        }
                        case LIKE: {
                            String fieldName = element.findFieldIgnoreCase("f").map(ModuleUtils::toString).orElse(null);
                            if (fieldName == null)
                                return null;
                            String value = element.findFieldIgnoreCase("v")
                                    .map(ModuleUtils::toString)
                                    .orElse("");
                            return dbIndex.findLike(fields.indexOf(fieldName), value);
                        }
                        case IN: {
                            String fieldName = element.findFieldIgnoreCase("f").map(ModuleUtils::toString).orElse(null);
                            if (fieldName == null)
                                return null;
                            ObjectArray arr = element.findFieldIgnoreCase("v").map(ModuleUtils::getObjectArray).orElse(null);
                            if (arr == null)
                                return null;
                            int indexOf = fields.indexOf(fieldName);
                            return Stream.iterate(0, i -> i + 1)
                                    .limit(arr.size())
                                    .map(arr::get)
                                    .flatMap(o -> dbIndex.findEquals(indexOf, o));
                        }
                        case AND: {
                            List<IElement> lst1 = element.findFieldIgnoreCase("p1").map(ModuleUtils::getObjectElement)
                                    .map(e -> findPrv(e, fields, dbIndex).collect(Collectors.toList())).orElse(List.of());
                            List<IElement> lst2 = element.findFieldIgnoreCase("p2").map(ModuleUtils::getObjectElement)
                                    .map(e -> findPrv(e, fields, dbIndex).collect(Collectors.toList())).orElse(List.of());
                            List<IElement> resultLst = new ArrayList<>(lst1);
                            resultLst.retainAll(lst2);
                            return resultLst.stream();
                        }
                        case OR: {
                            return Stream.concat(
                                    element.findFieldIgnoreCase("p1").map(ModuleUtils::getObjectElement)
                                            .map(e -> findPrv(e, fields, dbIndex)).orElse(Stream.of()),
                                    element.findFieldIgnoreCase("p2").map(ModuleUtils::getObjectElement)
                                            .map(e -> findPrv(e, fields, dbIndex)).orElse(Stream.of())
                            );
                        }
                    }
                    return (Stream<IElement>) (Stream) Stream.of();
                })
                .orElse(Stream.of());
    }

    private enum PredicateType {
        EQUALS(Set.of("eq", "=")),
        NOT_EQUALS(Set.of("ne", "!=")),
        LESS(Set.of("lt", "<")),
        GREATER(Set.of("gt", ">")),
        LESS_OR_EQUAL(Set.of("le", "<=")),
        GREATER_OR_EQUAL(Set.of("ge", ">=")),
        LIKE(Set.of("like")),
        IN(Set.of("in")),
        AND(Set.of("and", "&&")),
        OR(Set.of("or", "||"));

        private Set<String> names;

        PredicateType(Set<String> names) {
            this.names = names;
        }

        public static Optional<PredicateType> parse(String name) {
            return Arrays.stream(PredicateType.values()).filter(t -> t.names.contains(name)).findFirst();
        }
    }

}
