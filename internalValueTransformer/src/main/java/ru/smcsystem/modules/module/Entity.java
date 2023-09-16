package ru.smcsystem.modules.module;

import ru.smcsystem.api.dto.IMessage;
import ru.smcsystem.api.enumeration.ValueType;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class Entity {
    private Number patternNumber;
    private Pattern pattern;
    private byte[] patternBytes;
    private List<Object> resultLst;

    public Entity(Number patternNumber, Pattern pattern, byte[] patternBytes, List<Object> resultLst) {
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
            return Arrays.equals(patternBytes, (byte[]) message.getValue());
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

    public List<Object> getResultLst() {
        return resultLst;
    }
}
