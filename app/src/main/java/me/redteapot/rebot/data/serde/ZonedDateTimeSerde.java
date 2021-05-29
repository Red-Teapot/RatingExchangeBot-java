package me.redteapot.rebot.data.serde;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;


public class ZonedDateTimeSerde extends SimpleModule {
    @Override
    public void setupModule(SetupContext context) {
        addSerializer(ZonedDateTime.class, new JsonSerializer<>() {
            @Override
            public void serialize(ZonedDateTime value, JsonGenerator gen, SerializerProvider provider) throws IOException {
                gen.writeString(value.toEpochSecond() + "#" + value.getNano() + "#" + value.getZone().getId());
            }
        });

        addDeserializer(ZonedDateTime.class, new JsonDeserializer<>() {
            @Override
            public ZonedDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                String[] tokens = p.getText().split("#");
                long epochSecond = Long.parseLong(tokens[0]);
                long nano = Long.parseLong(tokens[1]);
                String zone = tokens[2];
                return ZonedDateTime.ofInstant(
                    Instant.ofEpochSecond(epochSecond, nano),
                    ZoneId.of(zone));
            }
        });

        super.setupModule(context);
    }
}
