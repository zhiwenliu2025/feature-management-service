package com.fms.domain.enums;

public enum FlagType {
    boolean_,
    string,
    number,
    json;

    public String externalName() {
        return this == boolean_ ? "boolean" : name();
    }

    public static FlagType fromExternal(String value) {
        return "boolean".equals(value) ? boolean_ : valueOf(value);
    }
}
