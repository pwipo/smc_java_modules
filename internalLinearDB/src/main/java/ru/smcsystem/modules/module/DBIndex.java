package ru.smcsystem.modules.module;

import ru.smcsystem.api.dto.ObjectField;
import ru.smcsystem.api.enumeration.ValueType;
import ru.smcsystem.smc.utils.ModuleUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DBIndex {

    public static int countBytes(ValueType type) {
        Objects.requireNonNull(type, "type is null");
        switch (type) {
            case STRING:
                return 1/*is null*/ + 4/*hash*/ + 4/*size*/;
            case BYTE:
                return 1/*is null*/ + 1/*value*/;
            case SHORT:
                return 1/*is null*/ + 2/*value*/;
            case INTEGER:
                return 1/*is null*/ + 4/*value*/;
            case LONG:
                return 1/*is null*/ + 8/*value*/;
            case BIG_INTEGER:
                return 1/*is null*/ + 8/*value*/;
            case FLOAT:
                return 1/*is null*/ + 4/*value*/;
            case DOUBLE:
                return 1/*is null*/ + 8/*value*/;
            case BIG_DECIMAL:
                return 1/*is null*/ + 8/*value*/;
            case BYTES:
                return 1/*is null*/ + 4/*hash*/ + 4/*size*/;
            case OBJECT_ARRAY:
                return 0;
            case BOOLEAN:
                return 1/*is null*/ + 1/*value*/;
        }
        throw new IllegalArgumentException("Unsupported type " + type);
    }

    public static Object readValue(ValueType type, DataInputStream dis) throws IOException {
        Objects.requireNonNull(type, "type is null");
        Objects.requireNonNull(dis, "dis is null");
        boolean isNull = dis.readBoolean();
        if (isNull)
            return null;
        switch (type) {
            case STRING: {
                int hash = dis.readInt();
                int size = dis.readInt();
                return Map.entry(hash, size);
            }
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
                int hash = dis.readInt();
                int size = dis.readInt();
                return Map.entry(hash, size);
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
            case STRING: {
                String value = ModuleUtils.getString(field);
                return value != null ? Map.entry(value.hashCode(), value.length()) : null;
            }
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
            case BYTES: {
                byte[] value = ModuleUtils.getBytes(field);
                return value != null ? Map.entry(Arrays.hashCode(value), value.length) : List.of();
            }
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
        switch (type) {
            case STRING:
                dos.writeInt(value instanceof Map.Entry ? ((Map.Entry<Integer, Integer>) value).getKey() : 0);
                dos.writeInt(value instanceof Map.Entry ? ((Map.Entry<Integer, Integer>) value).getValue() : 0);
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
                dos.writeInt(value instanceof Map.Entry ? ((Map.Entry<Integer, Integer>) value).getKey() : 0);
                dos.writeInt(value instanceof Map.Entry ? ((Map.Entry<Integer, Integer>) value).getValue() : 0);
                break;
            case OBJECT_ARRAY:
                break;
            case BOOLEAN:
                dos.writeBoolean(value instanceof Boolean ? (Boolean) value : false);
                break;
        }
    }

}
