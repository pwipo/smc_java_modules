package ru.smcsystem.modules.module;

import org.apache.commons.lang.ArrayUtils;
import ru.smcsystem.api.dto.ObjectArray;
import ru.smcsystem.api.enumeration.ValueType;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Objects;

public class ValueUtils {

    public static byte[] toByteArray(ValueType valueType, Object value) {
        Objects.requireNonNull(valueType, "valueType not set");
        Objects.requireNonNull(value, "value not set");

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); DataOutputStream dos = new DataOutputStream(baos)) {
            toByteArray(valueType, value, dos);
            dos.flush();
            return baos.toByteArray();
        } catch (Exception e) {
            // throw new RuntimeException("exception", e);
            // logger.warn("exception", e);
        }
        return null;
    }

    public static void toByteArray(ValueType valueType, Object value, DataOutputStream dos) throws IOException {
        Objects.requireNonNull(valueType, "valueType not set");
        Objects.requireNonNull(value, "value not set");

        switch (valueType) {
            case BYTE:
            case SHORT:
            case INTEGER:
            case LONG:
            case BIG_INTEGER:
            case FLOAT:
            case DOUBLE:
            case BIG_DECIMAL:
                toByteArray(valueType, (Number) value, dos);
                break;
            case STRING:
                dos.write(((String) value).getBytes());
                break;
            case BYTES:
                dos.write((byte[]) value);
                break;
            case OBJECT_ARRAY:
                ObjectArrayConverter.toByteArray((ObjectArray) value, dos);
                break;
            case BOOLEAN:
                dos.writeBoolean((Boolean) value);
                break;
            default:
                throw new RuntimeException("wrong type " + valueType);
        }

    }

    private static void toByteArray(ValueType valueType, Number value, DataOutputStream dos) throws IOException {
        Objects.requireNonNull(valueType, "valueType not set");
        Objects.requireNonNull(value, "value not set");
        Objects.requireNonNull(dos, "dos not set");

        switch (valueType) {
            case BYTE:
                dos.writeByte((Byte) value);
                break;
            case SHORT:
                dos.writeShort((Short) value);
                break;
            case INTEGER:
                dos.writeInt((Integer) value);
                break;
            case LONG:
                dos.writeLong((Long) value);
                break;
            case BIG_INTEGER:
                // dos.write(((BigInteger) value).toByteArray());
                dos.write(((BigInteger) value).toString().getBytes());
                break;
            case FLOAT: {
                // dos.writeFloat((Float) value);
                // dos.write(Float.toString((Float) value).getBytes());
                byte[] bytes = new byte[4];
                ByteBuffer.wrap(bytes).putFloat((Float) value);
                dos.write(bytes);
                break;
            }
            case DOUBLE: {
                // dos.writeDouble((Double) value);
                // dos.write(Double.toString((Double) value).getBytes());
                byte[] bytes = new byte[8];
                ByteBuffer.wrap(bytes).putDouble((Double) value);
                dos.write(bytes);
                break;
            }
            case BIG_DECIMAL:
                // dos.write(bigDecimalToByte((BigDecimal) value));
                dos.write(((BigDecimal) value).toString().getBytes());
                break;
            default:
                throw new RuntimeException("wrong type " + valueType);
        }
    }

    public static Object valueObjectFromByteArray(ValueType type, byte[] valueArray) {
        /*
        List<Object> objects = valueFromByteArray(value);
        byte[] valueArray = (byte[]) objects.get(1);
        ValueType type = (ValueType) objects.get(0);
        */
        Object result = null;
        try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(valueArray))) {
            result = valueObjectFromByteArray(type, dis, valueArray.length);
        } catch (Exception e) {
            // throw new RuntimeException("exception", e);
            // logger.warn("exception", e);
        }
        return result;
    }

    public static Object valueObjectFromByteArray(ValueType type, DataInputStream dis, int size) {
        /*
        List<Object> objects = valueFromByteArray(value);
        byte[] valueArray = (byte[]) objects.get(1);
        ValueType type = (ValueType) objects.get(0);
        */
        Object result = null;
        try {
            switch (type) {
                case STRING:
                    result = dis.available() >= size ? new String(dis.readNBytes(size)) : "";
                    break;
                case BYTE:
                case SHORT:
                case INTEGER:
                case LONG:
                case BIG_INTEGER:
                case FLOAT:
                case DOUBLE:
                case BIG_DECIMAL:
                    result = bytesToValueNumber2(type, dis, size);
                    break;
                case BYTES:
                    result = dis.available() >= size ? dis.readNBytes(size) : null;
                    break;
                case OBJECT_ARRAY:
                    result = ObjectArrayConverter.objectArrayFromByteArray(dis);
                    break;
                case BOOLEAN:
                    result = bytesToValueBoolean(dis.readNBytes(1));
                    break;
            }
        } catch (Exception e) {
            // throw new RuntimeException("exception", e);
            // logger.warn("exception", e);
        }
        return result;
    }

    private static Number bytesToValueNumber2(ValueType valueType, DataInputStream dis, int size) throws IOException {
        Number result = null;
        switch (valueType) {
            case BYTE:
                result = dis.readByte();
                break;
            case SHORT:
                result = dis.readShort();
                break;
            case INTEGER:
                result = dis.readInt();
                break;
            case LONG:
                result = dis.readLong();
                break;
            case BIG_INTEGER:
                // result = dis.available() >= size ? new BigInteger(dis.readNBytes(size)) : BigInteger.ZERO;
                result = dis.available() >= size ? new BigInteger(new String(dis.readNBytes(size))) : BigInteger.ZERO;
                break;
            case FLOAT:
                // result = dis.readFloat();
                // result = Float.valueOf(new String(valueArray));
                result = dis.available() >= size ? ByteBuffer.wrap(dis.readNBytes(4)).getFloat() : 0.0f;
                break;
            case DOUBLE:
                // result = dis.readDouble();
                // result = Double.valueOf(new String(valueArray));
                result = dis.available() >= size ? ByteBuffer.wrap(dis.readNBytes(8)).getDouble() : 0.0d;
                break;
            case BIG_DECIMAL:
                // result = dis.available() >= size ? new BigDecimal(new String(dis.readNBytes(size))) : BigDecimal.ZERO;
                result = dis.available() >= size ? byteToBigDecimal(dis.readNBytes(size)) : BigDecimal.ZERO;
                break;
            default:
                throw new RuntimeException("wrong type " + valueType);
        }
        return result;
    }

    public static Boolean bytesToValueBoolean(byte[] bytes) {
        return !ArrayUtils.isEmpty(bytes) && bytes[0] > 0;
    }

    public static BigDecimal byteToBigDecimal(byte[] raw) {
        int scale = (raw[0] & 0xFF) << 24 |
                (raw[1] & 0xFF) << 16 |
                (raw[2] & 0xFF) << 8 |
                (raw[3] & 0xFF);
        BigInteger sig = new BigInteger(ArrayUtils.subarray(raw, 4, raw.length));
        return new BigDecimal(sig, scale);
    }

}
