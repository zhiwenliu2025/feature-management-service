package com.fms.domain.converter;

import com.fms.domain.enums.FlagType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class FlagTypeConverter implements AttributeConverter<FlagType, String> {

    @Override
    public String convertToDatabaseColumn(FlagType attribute) {
        return attribute == null ? null : attribute.externalName();
    }

    @Override
    public FlagType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : FlagType.fromExternal(dbData);
    }
}
