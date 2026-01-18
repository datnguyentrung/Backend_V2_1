package com.dat.backend_v2_1.util.converter;

import com.dat.backend_v2_1.enums.Core.Weekday;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class WeekdayConverter implements AttributeConverter<Weekday, Integer> {
    @Override
    public Integer convertToDatabaseColumn(Weekday attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public Weekday convertToEntityAttribute(Integer dbData) {
        return dbData == null ? null : Weekday.fromCode(dbData);
    }
}