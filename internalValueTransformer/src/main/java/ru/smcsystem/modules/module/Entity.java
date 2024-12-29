package ru.smcsystem.modules.module;

import ru.smcsystem.api.dto.IMessage;
import ru.smcsystem.api.enumeration.ValueType;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class Entity {
    private String originPattern;
    private Number patternNumber;
    private Pattern pattern;
    private byte[] patternBytes;
    private List<Object> resultLst;

    public Entity(String originPattern, Number patternNumber, Pattern pattern, byte[] patternBytes, List<Object> resultLst) {
        this.originPattern = originPattern;
        this.patternNumber = patternNumber;
        this.pattern = pattern;
        this.patternBytes = patternBytes;
        this.resultLst = resultLst;
    }

    public boolean match(IMessage message) {
        /*
        if (value == null && (pattern != null || patternBytes != null))
            return false;
        */
        if (ValueType.BYTES.equals(message.getType())) {
            return patternBytes != null && Arrays.equals(patternBytes, (byte[]) message.getValue());
        } else if (ValueType.STRING.equals(message.getType())) {
            return pattern != null && pattern.matcher(message.getValue().toString()).find();
        } else {
            Number number = (Number) message.getValue();
            if (patternNumber != null) {
                return patternNumber.equals(number.longValue()) || patternNumber.equals(number.doubleValue());
            } else {
                return pattern != null && pattern.matcher(number.toString()).find();
            }
        }
    }

    public String getOriginPattern() {
        return originPattern;
    }

    public String getType() {
        if (patternBytes != null) {
            return "Bytes";
        } else if (patternNumber != null) {
            return "Number";
        } else if (pattern != null) {
            return "String";
        }
        return "None";
    }

    public List<Object> getResultLst() {
        return resultLst;
    }
}
