package me.redteapot.rebot.data.serde;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;
import java.time.Duration;

public class DurationSerde extends SimpleModule {
    @Override
    public void setupModule(SetupContext context) {
        addSerializer(Duration.class, new JsonSerializer<>() {
            @Override
            public void serialize(Duration value, JsonGenerator gen, SerializerProvider provider) throws IOException {
                gen.writeString(value.toSeconds() + "#" + value.getNano());
            }
        });

        addDeserializer(Duration.class, new JsonDeserializer<>() {
            @Override
            public Duration deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                String[] tokens = p.getText().split("#");
                long seconds = Long.parseLong(tokens[0]);
                int nanos = Integer.parseInt(tokens[1]);
                return Duration.ofSeconds(seconds, nanos);
            }
        });

        super.setupModule(context);
    }
}
