package me.redteapot.rebot.data.converters;

import discord4j.common.util.Snowflake;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter(autoApply = true)
public class SnowflakeConverter implements AttributeConverter<Snowflake, Long> {
    @Override
    public Long convertToDatabaseColumn(Snowflake attribute) {
        return attribute.asLong();
    }

    @Override
    public Snowflake convertToEntityAttribute(Long dbData) {
        return Snowflake.of(dbData);
    }
}
