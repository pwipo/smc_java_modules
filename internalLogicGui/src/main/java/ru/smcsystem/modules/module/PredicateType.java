package ru.smcsystem.modules.module;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

public enum PredicateType {
    EQUALS(Set.of("eq", "=")),
    NOT_EQUALS(Set.of("ne", "!=")),
    LESS(Set.of("lt", "<")),
    GREATER(Set.of("gt", ">")),
    LESS_OR_EQUAL(Set.of("le", "<=")),
    GREATER_OR_EQUAL(Set.of("ge", ">=")),
    CONTAINS(Set.of("contains")),
    START_WITH(Set.of("startWith")),
    END_WITH(Set.of("endWith"));

    private Set<String> names;

    PredicateType(Set<String> names) {
        this.names = names;
    }

    public static Optional<PredicateType> parse(String name) {
        return Arrays.stream(PredicateType.values()).filter(t -> t.names.contains(name)).findFirst();
    }
}
