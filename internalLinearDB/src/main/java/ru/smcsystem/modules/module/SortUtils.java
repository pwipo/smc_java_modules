package ru.smcsystem.modules.module;

import ru.seits.projects.lineardb.IElement;
import ru.smcsystem.api.dto.ObjectElement;
import ru.smcsystem.modules.module.predicators.MyPredicateUtils;
import ru.smcsystem.smc.utils.ModuleUtils;

import java.util.*;

public class SortUtils {

    public static Comparator<IElement> parse(ObjectElement element, List<String> fields) {
        if (element == null)
            return Comparator.comparing(IElement::getId);
        String fieldName = element.findFieldIgnoreCase("f").map(ModuleUtils::toString).orElse(null);
        if (fieldName == null)
            return Comparator.comparing(IElement::getId);
        Comparator<IElement> comparator = Comparator.comparing(e -> (Comparable) MyPredicateUtils.getFieldValue(e, fields, fieldName));
        SortType type = element.findFieldIgnoreCase("t")
                .flatMap(f -> SortType.parse(ModuleUtils.toString(f).toLowerCase()))
                .orElse(SortType.ASC);
        if (type == SortType.DESC)
            comparator = comparator.reversed();
        return comparator;
    }

    private enum SortType {
        DESC(Set.of("desk")),
        ASC(Set.of("asc"));

        private Set<String> names;

        SortType(Set<String> names) {
            this.names = names;
        }

        public static Optional<SortType> parse(String name) {
            return Arrays.stream(SortType.values()).filter(t -> t.names.contains(name)).findFirst();
        }
    }


}
