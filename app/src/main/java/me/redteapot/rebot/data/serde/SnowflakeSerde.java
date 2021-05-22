package me.redteapot.rebot.data.serde;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import discord4j.common.util.Snowflake;

import java.io.IOException;

public class SnowflakeSerde extends SimpleModule {
    @Override
    public void setupModule(SetupContext context) {
        addSerializer(Snowflake.class, new JsonSerializer<>() {
            @Override
            public void serialize(Snowflake value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                gen.writeNumber(value.asLong());
            }
        });

        addDeserializer(Snowflake.class, new JsonDeserializer<>() {
            @Override
            public Snowflake deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                return Snowflake.of(p.getLongValue());
            }
        });

        super.setupModule(context);
    }
}
