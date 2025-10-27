package ru.smcsystem.modules.module;

import ru.seits.projects.lineardb.DB;
import ru.seits.projects.lineardb.IElement;
import ru.smcsystem.api.dto.IMessage;
import ru.smcsystem.api.dto.ObjectArray;
import ru.smcsystem.api.dto.ObjectElement;
import ru.smcsystem.api.dto.ObjectField;
import ru.smcsystem.api.enumeration.ObjectType;
import ru.smcsystem.api.enumeration.ValueType;
import ru.smcsystem.api.exceptions.ModuleException;
import ru.smcsystem.api.module.Module;
import ru.smcsystem.api.tools.ConfigurationTool;
import ru.smcsystem.api.tools.execution.ExecutionContextTool;
import ru.smcsystem.modules.module.predicators.MyPredicateUtils;
import ru.smcsystem.smc.utils.ModuleUtils;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LinearDBModule implements Module {
    private static final int DB_VER = 1;
    // private static final String FIELD_VALUE_NULL = "NULL";
    // private static final Object INDEX_VALUE_NULL = DBIndex.getValue(ValueType.STRING, new ObjectField("none", (String) null));
    private DB<ObjectElement> db;
    private List<Map.Entry<String, ValueType>> fieldsIndexed;
    private int countAdditionalBytes;
    private List<Map.Entry<String[], ObjectType>> fieldsIndexed2;
    // private final List<Map<?, List<IElement>>> indexes;
    private List<String> fieldsIndexedNames;
    private String fieldNameId;
    private String fieldNameDate;
    private List<IElement> indexElements;
    private boolean indexElementsReady;

    @Override
    public void start(ConfigurationTool configurationTool) throws ModuleException {
        fieldNameId = configurationTool.getSetting("fieldNameId").map(ModuleUtils::toString).orElseThrow(() -> new ModuleException("fieldNameId setting not found"));
        fieldNameDate = configurationTool.getSetting("fieldNameDate").map(ModuleUtils::toString).orElseThrow(() -> new ModuleException("fieldNameDate setting not found"));
        String indexesStr = configurationTool.getSetting("indexes").map(ModuleUtils::toString).orElse("");
        String indexesStr2 = configurationTool.getVariable("indexes").map(ModuleUtils::toString).orElse(null);
        this.setFieldsIndexed(indexesStr2 != null ? indexesStr2 : indexesStr);
        indexElements = null;
        indexElementsReady = false;
        this.db = new DB<>(
                new File(configurationTool.getWorkDirectory()),
                "db",
                DB_VER,
                (ver, b) -> ObjectArrayConverter.objectElementFromByteArray(b),
                (ver, c) -> ObjectArrayConverter.toByteArray(c),
                o -> o.findField(fieldNameId).map(ModuleUtils::getNumber).map(Number::longValue).orElse(null),
                (o, id) -> o.findField(fieldNameId).ifPresentOrElse(
                        f -> f.setValue(id),
                        () -> o.getFields().add(new ObjectField(fieldNameId, id))),
                o -> o.findField(fieldNameDate).map(ModuleUtils::getNumber).map(Number::longValue).orElse(null),
                (o, id) -> o.findField(fieldNameDate).ifPresentOrElse(
                        f -> f.setValue(id),
                        () -> o.getFields().add(new ObjectField(fieldNameDate, id))),
                ver -> countAdditionalBytes,
                (ver, b) -> {
                    List<Object> lst = new ArrayList<>(this.fieldsIndexed.size());
                    try (ByteArrayInputStream bais = new ByteArrayInputStream(b); DataInputStream dis = new DataInputStream(bais)) {
                        for (Map.Entry<String, ValueType> e : this.fieldsIndexed) {
                            if (DBIndex.countBytes(e.getValue()) > dis.available())
                                break;
                            lst.add(DBIndex.readValue(e.getValue(), dis));
                        }
                    } catch (Exception e) {
                        for (int i = this.fieldsIndexed.size() - lst.size(); i < this.fieldsIndexed.size(); i++)
                            lst.add(null);
                    }
                    return lst;
                },
                (ver, list) -> {
                    try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); DataOutputStream dos = new DataOutputStream(baos)) {
                        for (int i = 0; i < list.size(); i++)
                            DBIndex.writeValue(dos, this.fieldsIndexed.get(i).getValue(), list.get(i));
                        dos.flush();
                        return baos.toByteArray();
                    } catch (Exception e) {
                        return new byte[countAdditionalBytes];
                    }
                },
                (ver, c) -> Optional.of(findFields(c, this.fieldsIndexed2))
                        .map(l -> {
                            List<Object> lst = new ArrayList<>(l.size());
                            for (int i = 0; i < l.size(); i++)
                                lst.add(DBIndex.getValue(this.fieldsIndexed.get(i).getValue(), l.get(i)));
                            return lst;
                        })
                        .orElse(List.of())
        );
        try {
            db.open();
        } catch (Exception e) {
            throw new ModuleException("error while open db", e);
        }
        if (indexesStr2 != null && !Objects.equals(indexesStr, indexesStr2)) {
            setFieldsIndexed(indexesStr);
            try {
                db.rebuildIndex();
            } catch (Exception e) {
                throw new ModuleException("error while rebuildIndex", e);
            }
            configurationTool.setVariable("indexes", indexesStr);
        }
    }

    @Override
    public void update(ConfigurationTool configurationTool) throws ModuleException {
        stop(configurationTool);
        start(configurationTool);
    }

    @Override
    public void process(ConfigurationTool configurationTool, ExecutionContextTool executionContextTool) throws ModuleException {
        ModuleUtils.processMessagesAll(configurationTool, executionContextTool, (unused, messagesList) -> {
            Type type = Type.valueOf(executionContextTool.getType().toUpperCase());
            switch (type) {
                case INSERT:
                    insert(configurationTool, executionContextTool, messagesList);
                    break;
                case UPDATE:
                    update(configurationTool, executionContextTool, messagesList);
                    break;
                case DELETE:
                    delete(configurationTool, executionContextTool, messagesList);
                    break;
                case FIND:
                    find(configurationTool, executionContextTool, messagesList);
                    break;
                case COUNT:
                    count(configurationTool, executionContextTool, messagesList);
                    break;
                case DELETE_WHERE:
                    deleteWhere(configurationTool, executionContextTool, messagesList);
                    break;
            }
        });
    }

    @Override
    public void stop(ConfigurationTool configurationTool) throws ModuleException {
        if (db != null) {
            try {
                db.close();
            } catch (Exception ignored) {
            }
            db = null;
        }
        fieldsIndexed = null;
        fieldsIndexed2 = null;
        fieldsIndexedNames = null;
        indexElements = null;
        indexElementsReady = false;
    }

    private void insert(ConfigurationTool configurationTool, ExecutionContextTool executionContextTool, List<LinkedList<IMessage>> messageList) throws IOException {
        ObjectArray objectArray = ModuleUtils.deserializeToObject(messageList.get(0));
        if (!ModuleUtils.isArrayContainObjectElements(objectArray)) {
            executionContextTool.addError("need objectArray elements");
            return;
        }
        configurationTool.loggerDebug("insert " + objectArray.size());
        List<ObjectElement> elements = Stream.iterate(0, i -> i + 1)
                .limit(objectArray.size())
                .map(n -> (ObjectElement) objectArray.get(n))
                .filter(o -> o.findField(fieldNameId).isEmpty())
                .map(e -> {
                    try {
                        return (ObjectElement) e.clone();
                    } catch (CloneNotSupportedException ignored) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        List<ObjectElement> result = db.save(elements);
        indexElementsReady = false;
        executionContextTool.addMessage(new ObjectArray(
                result.stream()
                        .flatMap(e -> e.findField(fieldNameId).map(ModuleUtils::getNumber).map(Number::longValue).stream())
                        .collect(Collectors.toList()),
                ObjectType.LONG
        ));
    }

    private void update(ConfigurationTool configurationTool, ExecutionContextTool executionContextTool, List<LinkedList<IMessage>> messageList) throws IOException {
        ObjectArray objectArray = ModuleUtils.deserializeToObject(messageList.get(0));
        if (!ModuleUtils.isArrayContainObjectElements(objectArray)) {
            executionContextTool.addError("need objectArray elements");
            return;
        }
        configurationTool.loggerDebug("update " + objectArray.size());
        List<ObjectElement> elements = Stream.iterate(0, i -> i + 1)
                .limit(objectArray.size())
                .map(n -> (ObjectElement) objectArray.get(n))
                .filter(o -> o.findField(fieldNameId).isPresent())
                .map(e -> {
                    try {
                        return (ObjectElement) e.clone();
                    } catch (CloneNotSupportedException ignored) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        List<ObjectElement> result = db.save(elements);
        indexElementsReady = false;
        executionContextTool.addMessage(new ObjectArray(
                result.stream()
                        .flatMap(e -> e.findField(fieldNameId).map(ModuleUtils::getNumber).map(Number::longValue).stream())
                        .collect(Collectors.toList()),
                ObjectType.LONG
        ));
    }

    private void delete(ConfigurationTool configurationTool, ExecutionContextTool executionContextTool, List<LinkedList<IMessage>> messageList) throws IOException {
        List<Long> ids = messageList.get(0).stream()
                .filter(ModuleUtils::isNumber)
                .map(ModuleUtils::getNumber)
                .map(Number::longValue)
                .collect(Collectors.toList());

        configurationTool.loggerDebug("delete " + ids.size());
        List<IElement> elements = getIndexElements().stream().filter(e -> ids.contains(e.getId())).collect(Collectors.toList());
        db.delete(elements);
        indexElementsReady = false;
        executionContextTool.addMessage(new ObjectArray(
                elements.stream()
                        .map(IElement::getId)
                        .collect(Collectors.toList()),
                ObjectType.LONG
        ));
    }

    private void find(ConfigurationTool configurationTool, ExecutionContextTool executionContextTool, List<LinkedList<IMessage>> messageList) {
        ObjectArray objectArray = ModuleUtils.deserializeToObject(messageList.get(0));
        if (!ModuleUtils.isArrayContainObjectElements(objectArray)) {
            executionContextTool.addError("need objectArray elements");
            return;
        }
        ObjectElement objectElement = ModuleUtils.isArrayContainObjectElements(objectArray) ? (ObjectElement) objectArray.get(0) : null;
        int skip = objectElement != null ? objectElement.findField("skip").map(ModuleUtils::getNumber).map(Number::intValue).orElse(0) : 0;
        int limit = objectElement != null ? objectElement.findField("limit").map(ModuleUtils::getNumber).map(Number::intValue).orElse(10) : 10;
        ObjectElement filter = objectElement != null ? objectElement.findField("filter").map(ModuleUtils::getObjectElement).orElse(null) : null;
        ObjectElement sort = objectElement != null ? objectElement.findField("sort").map(ModuleUtils::getObjectElement).orElse(null) : null;
        // if (filter == null) {
        //     executionContextTool.addError("filter not find");
        //     return;
        // }
        // LinkedList<Object> placeHolderValues = messageList.size() > 1 ?
        //         messageList.get(1).stream().map(IValue::getValue).collect(Collectors.toCollection(LinkedList::new)) :
        //         new LinkedList<>();

        configurationTool.loggerTrace(String.format("find filter=%s, skip=%s, limit=%s, sort=%s", filter, skip, limit, sort));
        List<ObjectElement> elements = MyPredicateUtils.parse(filter, fieldsIndexedNames/*, placeHolderValues*/)
                .map(p -> getIndexElements().stream().filter(p)
                        .sorted(SortUtils.parse(sort, fieldsIndexedNames))
                        .skip(skip)
                        .limit(limit)
                        .collect(Collectors.toList()))
                .map(l -> db.get(l))
                .orElse(List.of());
        executionContextTool.addMessage(new ObjectArray((List) elements, ObjectType.OBJECT_ELEMENT));
    }

    private void count(ConfigurationTool configurationTool, ExecutionContextTool executionContextTool, List<LinkedList<IMessage>> messageList) {
        ObjectArray objectArray = ModuleUtils.deserializeToObject(messageList.get(0));
        if (!ModuleUtils.isArrayContainObjectElements(objectArray)) {
            executionContextTool.addError("need objectArray elements");
            return;
        }
        ObjectElement objectElement = ModuleUtils.isArrayContainObjectElements(objectArray) ? (ObjectElement) objectArray.get(0) : null;
        ObjectElement filter = objectElement != null ? objectElement.findField("filter").map(ModuleUtils::getObjectElement).orElse(null) : null;
        // if (filter == null) {
        //     executionContextTool.addError("filter not find");
        //     return;
        // }
        // LinkedList<Object> placeHolderValues = messageList.size() > 1 ?
        //         messageList.get(1).stream().map(IValue::getValue).collect(Collectors.toCollection(LinkedList::new)) :
        //         new LinkedList<>();

        configurationTool.loggerTrace(String.format("count filter=%s", filter));
        Long result = MyPredicateUtils.parse(filter, fieldsIndexedNames/*, placeHolderValues*/)
                .map(p -> getIndexElements().stream().filter(p).count())
                .orElse(0L);
        executionContextTool.addMessage(result);
    }

    private void deleteWhere(ConfigurationTool configurationTool, ExecutionContextTool executionContextTool, List<LinkedList<IMessage>> messageList) throws IOException {
        ObjectArray objectArray = ModuleUtils.deserializeToObject(messageList.get(0));
        if (!ModuleUtils.isArrayContainObjectElements(objectArray)) {
            executionContextTool.addError("need objectArray elements");
            return;
        }
        ObjectElement objectElement = ModuleUtils.isArrayContainObjectElements(objectArray) ? (ObjectElement) objectArray.get(0) : null;
        ObjectElement filter = objectElement != null ? objectElement.findField("filter").map(ModuleUtils::getObjectElement).orElse(null) : null;
        // if (filter == null) {
        //     executionContextTool.addError("filter not find");
        //     return;
        // }
        // LinkedList<Object> placeHolderValues = messageList.size() > 1 ?
        //         messageList.get(1).stream().map(IValue::getValue).collect(Collectors.toCollection(LinkedList::new)) :
        //         new LinkedList<>();

        configurationTool.loggerTrace(String.format("deleteWhere filter=%s", filter));
        List<IElement> elements = MyPredicateUtils.parse(filter, fieldsIndexedNames/*, placeHolderValues*/)
                .map(p -> getIndexElements().stream().filter(p).collect(Collectors.toList()))
                .orElse(List.of());

        db.delete(elements);
        indexElementsReady = false;
        executionContextTool.addMessage(new ObjectArray(
                elements.stream()
                        .map(IElement::getId)
                        .collect(Collectors.toList()),
                ObjectType.LONG
        ));
    }

    private List<ObjectField> findFields(ObjectElement objectElement, List<Map.Entry<String[], ObjectType>> fieldsIndexed) {
        if (objectElement == null || objectElement.getFields().isEmpty() || fieldsIndexed == null || fieldsIndexed.isEmpty())
            return List.of();
        List<ObjectElement> objectElements = List.of(objectElement);
        return fieldsIndexed.stream()
                .map(e -> {
                    List<ObjectField> fields = ModuleUtils.findFields(objectElements, e.getKey(), 0);
                    if (fields != null && !fields.isEmpty()) {
                        ObjectField field = fields.get(0);
                        if (field.getType() == e.getValue() ||
                                (e.getValue() == ObjectType.LONG && (field.getType() == ObjectType.BYTE || field.getType() == ObjectType.SHORT || field.getType() == ObjectType.INTEGER ||
                                        field.getType() == ObjectType.LONG || field.getType() == ObjectType.BIG_INTEGER)) ||
                                (e.getValue() == ObjectType.DOUBLE && (field.getType() == ObjectType.FLOAT || field.getType() == ObjectType.DOUBLE ||
                                        field.getType() == ObjectType.BIG_DECIMAL))) {
                            return field;
                        }
                    }
                    return null;
                })
                .collect(Collectors.toList());
    }

    private void setFieldsIndexed(String indexesStr) {
        this.fieldsIndexed = Arrays.stream(indexesStr.split(","))
                .map(s -> s.split("="))
                .filter(s -> s.length == 2)
                .map(a -> Map.entry(a[0].trim(), IndexValueType.valueOf(a[1].trim().toUpperCase())))
                .map(e -> Map.entry(
                        e.getKey(),
                        ValueType.valueOf(e.getValue().name())))
                .collect(Collectors.toList());

        // countAdditionalBytes = 4/*type*/ + 4/*size*/ +
        //         fieldsIndexed.stream().mapToInt(e -> DBIndex.countBytes(e.getValue())).sum() +
        //         fieldsIndexed.size() * 4/*value type*/;
        this.countAdditionalBytes = this.fieldsIndexed.stream().mapToInt(e -> DBIndex.countBytes(e.getValue())).sum();
        this.fieldsIndexed2 = this.fieldsIndexed.stream()
                .map(e -> Map.entry(ModuleUtils.splitFieldNames(e.getKey()), ModuleUtils.convertTo(e.getValue())))
                .collect(Collectors.toList());
        // indexes.clear();
        // fieldsIndexed.forEach(e -> {
        //     if (isNumber(e.getValue())) {
        //         indexes.add(new TreeMap<Number, List<IElement>>());
        //     } else {
        //         indexes.add(new HashMap<>());
        //     }
        // });
        this.fieldsIndexedNames = new ArrayList<>(fieldsIndexed.size() + 2);
        fieldsIndexedNames.add(fieldNameId);
        fieldsIndexedNames.add(fieldNameDate);
        fieldsIndexedNames.addAll(fieldsIndexed.stream().map(Map.Entry::getKey).collect(Collectors.toList()));
    }

    private List<IElement> getIndexElements() {
        if (indexElements != null && indexElementsReady)
            return indexElements;
        indexElements = db.getIndexElements();
        indexElementsReady = true;
        return indexElements;
    }

    private enum IndexValueType {
        STRING, LONG, DOUBLE
    }

    private enum Type {
        INSERT, UPDATE, DELETE, FIND, COUNT, DELETE_WHERE
    }

}
