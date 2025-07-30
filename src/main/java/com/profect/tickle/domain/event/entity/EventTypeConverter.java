package com.profect.tickle.domain.event.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class EventTypeConverter implements AttributeConverter<EventType, Integer> {

    @Override
    public Integer convertToDatabaseColumn(EventType attribute) {
        return attribute != null ? attribute.getCode() : null;
    }

    @Override
    public EventType convertToEntityAttribute(Integer dbData) {
        if (dbData == null) {
            return null;
        }
        return EventType.fromCode(dbData);
    }
}