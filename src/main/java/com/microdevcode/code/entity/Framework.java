package com.microdevcode.code.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.IOException;

@JsonSerialize(using = Framework.FrameworkSerializer.class)
@JsonDeserialize(using = Framework.FrameworkDeserializer.class)
public enum Framework {

    EXPRESS_JS("EXPRESS_JS"),
    FAST_API("FAST_API"),
    FLASK("FLASK"),
    DJANGO_REST("DJANGO_REST"),
    SPRING_BOOT("SPRING_BOOT"),
    GIN("GIN"),
    FIBER("FIBER"),
    ACTIX_WEB("ACTIX_WEB"),
    LARAVEL("LARAVEL");

    private final String value;

    Framework(String value) { this.value = value; }

    public String getValue() { return value; }

    @JsonCreator
    public static Framework fromValue(String value) {
        for (Framework f : values()) {
            if (f.value.equalsIgnoreCase(value)) return f;
        }
        throw new IllegalArgumentException("Unknown framework: " + value);
    }

    // ✅ Serializer — Enum → String
    public static class FrameworkSerializer extends JsonSerializer<Framework> {
        @Override
        public void serialize(Framework f, JsonGenerator gen,
                              SerializerProvider provider) throws IOException {
            gen.writeString(f.getValue());
        }
    }

    // ✅ Deserializer — String → Enum
    public static class FrameworkDeserializer extends JsonDeserializer<Framework> {
        @Override
        public Framework deserialize(JsonParser p,
                                     DeserializationContext ctx) throws IOException {
            return Framework.fromValue(p.getValueAsString());
        }
    }
}