package ru.smcsystem.modules.module;

import ru.seits.projects.lineardb.DB;
import ru.seits.projects.lineardb.IElement;
import ru.smcsystem.api.dto.ObjectArray;
import ru.smcsystem.api.dto.ObjectElement;
import ru.smcsystem.api.dto.ObjectField;
import ru.smcsystem.api.enumeration.ObjectType;
import ru.smcsystem.api.enumeration.ValueType;
import ru.smcsystem.api.tools.ConfigurationTool;
import ru.smcsystem.smc.utils.ModuleUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DBIndex {
    private DB<ObjectElement> db;
    private List<IElement> indexElements;
    private final List<Map<Object, Set<IElement>>> indexes;
    private boolean indexElementsReady;
    private final List<ValueType> fieldTypes;

    public DBIndex() {
        db = null;
        indexElements = null;
        indexes = new ArrayList<>();
        indexElementsReady = false;
        fieldTypes = new ArrayList<>(List.of(ValueType.LONG, ValueType.LONG));
    }

    public void setDb(DB<ObjectElement> db) {
        this.db = db;
    }

    public void markDirtyIndexElements() {
        this.indexElementsReady = false;
    }

    public List<Map<Object, Set<IElement>>> getIndexes(ConfigurationTool configurationTool) {
        getIndexElements(configurationTool);
        return indexes;
    }

    public List<IElement> getIndexElements(ConfigurationTool configurationTool) {
        if (indexElements != null && indexElementsReady)
            return indexElements;

        configurationTool.loggerDebug("rebuild indexes start");
        //get all elements
        indexElements = db.getIndexElements();

        //create indexes
        indexes.clear();
        this.fieldTypes.forEach(v -> {
            if (ModuleUtils.isNumber(v)) {
                Comparator<Object> nullFirstComparator = (Comparator) Comparator.nullsFirst(Comparator.naturalOrder());
                indexes.add(new TreeMap<>(nullFirstComparator));
            } else {
                indexes.add(new HashMap<>());
            }
        });

        //fill indexes
        Map<Object, Set<IElement>> mapId = indexes.get(0);
        Map<Object, Set<IElement>> mapDate = indexes.get(1);
        indexElements.forEach(e -> insertIndexElement(mapId, mapDate, e));
        configurationTool.loggerDebug("rebuild indexes end, count elements " + indexElements.size());

        indexElementsReady = true;
        return indexElements;
    }

    private void insertIndexElement(Map<Object, Set<IElement>> mapId, Map<Object, Set<IElement>> mapDate, IElement element) {
        Objects.requireNonNull(mapId, "mapId");
        Objects.requireNonNull(mapDate, "mapDate");
        Objects.requireNonNull(element, "element");
        mapId.put(element.getId(), Set.of(element));
        mapDate.computeIfAbsent(element.getDate(), k -> new HashSet<>()).add(element);
        for (int i = 0; i < element.getAdditionalData().size(); i++) {
            Map<Object, Set<IElement>> indexMap = indexes.get(2 + i);
            Object v = element.getAdditionalData().get(i);
            indexMap.computeIfAbsent(convert(v, fieldTypes.get(2 + i)), k -> new HashSet<>()).add(element);
        }
    }

    public void insertOrUpdateIndexElement(ConfigurationTool configurationTool, IElement element) {
        Objects.requireNonNull(element, "element");
        Map<Object, Set<IElement>> mapId = getIndexes(configurationTool).get(0);
        Map<Object, Set<IElement>> mapDate = getIndexes(configurationTool).get(1);

        if (mapId.containsKey(element.getId()))
            removeIndexElement(configurationTool, element);
        insertIndexElement(mapId, mapDate, element);
    }

    public void removeIndexElement(ConfigurationTool configurationTool, IElement e) {
        if (e == null)
            return;
        Map<Object, Set<IElement>> mapId = getIndexes(configurationTool).get(0);
        Set<IElement> elements = mapId.remove(e.getId());
        if (elements == null || elements.isEmpty())
            return;

        IElement element = elements.iterator().next();
        Map<Object, Set<IElement>> mapDate = getIndexes(configurationTool).get(1);
        elements = mapDate.get(element.getDate());
        if (elements != null)
            elements.remove(element);
        for (int i = 0; i < element.getAdditionalData().size(); i++) {
            Map<Object, Set<IElement>> indexMap = getIndexes(configurationTool).get(2 + i);
            Object v = element.getAdditionalData().get(i);
            elements = indexMap.get(convert(v, fieldTypes.get(2 + i)));
            if (elements != null)
                elements.remove(element);
        }
    }

    public void updateIndexTypes(List<Map.Entry<String, ValueType>> fieldsIndexed) {
        Objects.requireNonNull(fieldsIndexed);
        this.fieldTypes.clear();
        this.fieldTypes.add(ValueType.LONG); //id
        this.fieldTypes.add(ValueType.LONG); //date
        this.fieldTypes.addAll(fieldsIndexed.stream().map(Map.Entry::getValue).collect(Collectors.toList()));
        markDirtyIndexElements();
    }

    public Optional<IElement> findOne(ConfigurationTool configurationTool, Long id) {
        return Optional.ofNullable(id).map(n -> getIndexes(configurationTool).get(0).get(n)).map(c -> c.iterator().next());
    }

    public Stream<IElement> findEquals(ConfigurationTool configurationTool, int fieldId, Object value) {
        Map<Object, Set<IElement>> indexMap = getIndexes(configurationTool).get(fieldId);
        ValueType valueType = fieldTypes.get(fieldId);
        Object valueConverted = convert(value, valueType);
        return indexMap.getOrDefault(valueConverted, Set.of()).stream();
    }

    public Stream<IElement> findNotEquals(ConfigurationTool configurationTool, int fieldId, Object value) {
        Map<Object, Set<IElement>> indexMap = getIndexes(configurationTool).get(fieldId);
        ValueType valueType = fieldTypes.get(fieldId);
        Object valueConverted = convert(value, valueType);
        return indexMap.entrySet().stream()
                .filter(e -> !Objects.equals(e.getKey(), valueConverted))
                .flatMap(e -> e.getValue().stream());
    }

    public Stream<IElement> findLess(ConfigurationTool configurationTool, int fieldId, Object value, boolean inclusive) {
        Map<Object, Set<IElement>> indexMap = getIndexes(configurationTool).get(fieldId);
        ValueType valueType = fieldTypes.get(fieldId);
        if (ModuleUtils.isNumber(valueType)) {
            Object valueConverted = convert(value, valueType);
            TreeMap<Object, Set<IElement>> map = (TreeMap<Object, Set<IElement>>) indexMap;
            return map.headMap(valueConverted, inclusive).values().stream().flatMap(Collection::stream);
        }
        return Stream.of();
    }

    public Stream<IElement> findGreater(ConfigurationTool configurationTool, int fieldId, Object value, boolean inclusive) {
        Map<Object, Set<IElement>> indexMap = getIndexes(configurationTool).get(fieldId);
        ValueType valueType = fieldTypes.get(fieldId);
        if (ModuleUtils.isNumber(valueType)) {
            Object valueConverted = convert(value, valueType);
            TreeMap<Object, Set<IElement>> map = (TreeMap<Object, Set<IElement>>) indexMap;
            return map.tailMap(valueConverted, inclusive).values().stream().flatMap(Collection::stream);
        }
        return Stream.of();
    }

    public Stream<IElement> findLike(ConfigurationTool configurationTool, int fieldId, String value) {
        Map<Object, Set<IElement>> indexMap = getIndexes(configurationTool).get(fieldId);
        ValueType valueType = fieldTypes.get(fieldId);
        if (valueType == ValueType.STRING) {
            Pattern pattern = Pattern.compile(convertSqlLikeToRegex(value));
            Map<String, Set<IElement>> map = (Map<String, Set<IElement>>) (Map) indexMap;
            return map.entrySet().stream()
                    .filter(e -> e.getKey() != null && pattern.matcher(e.getKey()).matches())
                    .flatMap(e -> e.getValue().stream());
        }
        return Stream.of();
    }

    private String convertSqlLikeToRegex(String sqlLikePattern) {
        // Escape characters that are special in regex but not in SQL LIKE
        String regex = sqlLikePattern.replace(".", "\\.")
                .replace("*", "\\*")
                .replace("+", "\\+")
                .replace("?", "\\?")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("{", "\\{")
                .replace("}", "\\}")
                .replace("^", "\\^")
                .replace("$", "\\$")
                .replace("|", "\\|")
                .replace("\\", "\\\\"); // Escape backslash itself

        // Replace SQL LIKE wildcards with regex equivalents
        regex = regex.replace("%", ".*");
        regex = regex.replace("_", ".");

        // Add start and end anchors for exact matching (optional, depending on desired behavior)
        // If you want to match anywhere within a string, omit these.
        regex = "^" + regex + "$";

        return regex;
    }

    private Object convert(Object value, ValueType type) {
        if (value == null)
            return null;
        switch (type) {
            case STRING:
                return value.toString();
            case BYTE:
                return value instanceof Number ? ((Number) value).byteValue() : (byte) 0;
            case SHORT:
                return value instanceof Number ? ((Number) value).shortValue() : (short) 0;
            case INTEGER:
                return value instanceof Number ? ((Number) value).intValue() : (int) 0;
            case LONG:
                return value instanceof Number ? ((Number) value).longValue() : (long) 0;
            case BIG_INTEGER:
                return value instanceof Number ? BigInteger.valueOf(((Number) value).longValue()) : BigInteger.ZERO;
            case FLOAT:
                return value instanceof Number ? ((Number) value).floatValue() : (float) 0;
            case DOUBLE:
                return value instanceof Number ? ((Number) value).doubleValue() : (double) 0;
            case BIG_DECIMAL:
                return value instanceof Number ? BigDecimal.valueOf(((Number) value).doubleValue()) : BigDecimal.ZERO;
            case BYTES:
                return value instanceof byte[] ? value : value.toString().getBytes();
            case OBJECT_ARRAY:
                return value instanceof ObjectArray ? value : new ObjectArray(List.of(value), ObjectType.VALUE_ANY);
            case BOOLEAN:
                return value instanceof Boolean ? value : (value instanceof Number ? ((Number) value).intValue() > 0 : Boolean.parseBoolean(value.toString()));
        }
        return value;
    }

    // public static int countBytes(ValueType type) {
    //     Objects.requireNonNull(type, "type is null");
    //     switch (type) {
    //         case STRING:
    //             return 1/*is null*/ + 4/*hash*/ + 4/*size*/;
    //         case BYTE:
    //             return 1/*is null*/ + 1/*value*/;
    //         case SHORT:
    //             return 1/*is null*/ + 2/*value*/;
    //         case INTEGER:
    //             return 1/*is null*/ + 4/*value*/;
    //         case LONG:
    //             return 1/*is null*/ + 8/*value*/;
    //         case BIG_INTEGER:
    //             return 1/*is null*/ + 8/*value*/;
    //         case FLOAT:
    //             return 1/*is null*/ + 4/*value*/;
    //         case DOUBLE:
    //             return 1/*is null*/ + 8/*value*/;
    //         case BIG_DECIMAL:
    //             return 1/*is null*/ + 8/*value*/;
    //         case BYTES:
    //             return 1/*is null*/ + 4/*hash*/ + 4/*size*/;
    //         case OBJECT_ARRAY:
    //             return 0;
    //         case BOOLEAN:
    //             return 1/*is null*/ + 1/*value*/;
    //     }
    //     throw new IllegalArgumentException("Unsupported type " + type);
    // }

    public static Object readValue(ValueType type, DataInputStream dis) throws IOException {
        Objects.requireNonNull(type, "type is null");
        Objects.requireNonNull(dis, "dis is null");
        boolean isNull = dis.readBoolean();
        if (isNull)
            return null;
        switch (type) {
            case STRING:
                return dis.readUTF();
            case BYTE:
                return dis.readByte()/*value*/;
            case SHORT:
                return dis.readShort()/*value*/;
            case INTEGER:
                return dis.readInt()/*value*/;
            case LONG:
                return dis.readLong()/*value*/;
            case BIG_INTEGER:
                return dis.readLong()/*value*/;
            case FLOAT:
                return dis.readFloat()/*value*/;
            case DOUBLE:
                return dis.readDouble()/*value*/;
            case BIG_DECIMAL:
                return dis.readDouble()/*value*/;
            case BYTES: {
                int size = dis.readInt();
                byte[] data = new byte[size];
                if (dis.read(data) == size)
                    return data;
                return null;
            }
            case OBJECT_ARRAY:
                return null;
            case BOOLEAN:
                return dis.readBoolean()/*value*/;
        }
        throw new IllegalArgumentException("Unsupported type " + type);
    }

    public static Object getValue(ValueType type, ObjectField field) {
        Objects.requireNonNull(type, "type is null");
        Objects.requireNonNull(field, "field is null");
        switch (type) {
            case STRING:
                return ModuleUtils.getString(field);
            case BYTE: {
                Number value = ModuleUtils.getNumber(field);
                return value != null ? value.byteValue() : null;
            }
            case SHORT: {
                Number value = ModuleUtils.getNumber(field);
                return value != null ? value.shortValue() : null;
            }
            case INTEGER: {
                Number value = ModuleUtils.getNumber(field);
                return value != null ? value.intValue() : null;
            }
            case LONG: {
                Number value = ModuleUtils.getNumber(field);
                return value != null ? value.longValue() : null;
            }
            case BIG_INTEGER: {
                Number value = ModuleUtils.getNumber(field);
                return value != null ? value.longValue() : null;
            }
            case FLOAT: {
                Number value = ModuleUtils.getNumber(field);
                return value != null ? value.floatValue() : null;
            }
            case DOUBLE: {
                Number value = ModuleUtils.getNumber(field);
                return value != null ? value.doubleValue() : null;
            }
            case BIG_DECIMAL: {
                Number value = ModuleUtils.getNumber(field);
                return value != null ? value.doubleValue() : null;
            }
            case BYTES:
                return ModuleUtils.getBytes(field);
            case OBJECT_ARRAY:
                return null;
            case BOOLEAN:
                return ModuleUtils.toBoolean(field);
        }
        throw new IllegalArgumentException("Unsupported type " + type);
    }

    public static void writeValue(DataOutputStream dos, ValueType type, Object value) throws IOException {
        Objects.requireNonNull(dos, "dos is null");
        Objects.requireNonNull(type, "type is null");
        dos.writeBoolean(value == null);
        if (value == null)
            return;
        switch (type) {
            case STRING:
                dos.writeUTF(value instanceof String ? (String) value : "");
                break;
            case BYTE:
                dos.writeByte(value instanceof Number ? ((Number) value).byteValue() : 0);
                break;
            case SHORT:
                dos.writeShort(value instanceof Number ? ((Number) value).shortValue() : 0);
                break;
            case INTEGER:
                dos.writeInt(value instanceof Number ? ((Number) value).intValue() : 0);
                break;
            case LONG:
                dos.writeLong(value instanceof Number ? ((Number) value).longValue() : 0);
                break;
            case BIG_INTEGER:
                dos.writeLong(value instanceof Number ? ((Number) value).longValue() : 0);
                break;
            case FLOAT:
                dos.writeFloat(value instanceof Number ? ((Number) value).floatValue() : 0);
                break;
            case DOUBLE:
                dos.writeDouble(value instanceof Number ? ((Number) value).doubleValue() : 0);
                break;
            case BIG_DECIMAL:
                dos.writeDouble(value instanceof Number ? ((Number) value).doubleValue() : 0);
                break;
            case BYTES:
                dos.writeInt(value instanceof byte[] ? ((byte[]) value).length : 0);
                if (value instanceof byte[])
                    dos.write((byte[]) value);
                break;
            case OBJECT_ARRAY:
                break;
            case BOOLEAN:
                dos.writeBoolean(value instanceof Boolean ? (Boolean) value : false);
                break;
        }
    }

}
