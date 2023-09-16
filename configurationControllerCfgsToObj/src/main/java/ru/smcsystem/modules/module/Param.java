package ru.smcsystem.modules.module;

import org.apache.commons.lang3.math.NumberUtils;

public class Param {
    public static enum Type {
        CFG,
        PARAMETER,
        VARIABLE,
        CONTEXT
    }

    private final Integer cfgId;
    private final Type type;
    private final String name;
    private final String publicName;
    private final Object param;

    public Param(Integer cfgId, Type type, String name, String publicName) {
        this.cfgId = cfgId;
        this.type = type;
        this.publicName = publicName;
        if (type == Type.CONTEXT) {
            String[] params = name.split("\\.");
            param = NumberUtils.isCreatable(params[0]) ? Integer.parseInt(params[0]) : params[0];
            this.name = params[1];
        } else {
            param = null;
            this.name = name;
        }
    }

    public Integer getCfgId() {
        return cfgId;
    }

    public Type getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getPublicName() {
        return publicName;
    }

    public Object getParam() {
        return param;
    }
}
