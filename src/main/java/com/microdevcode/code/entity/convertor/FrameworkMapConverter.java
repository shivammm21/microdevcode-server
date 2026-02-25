package com.microdevcode.code.entity.convertor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microdevcode.code.entity.Framework;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.HashMap;

@Converter
public class FrameworkMapConverter implements
        AttributeConverter<HashMap<Framework, HashMap<String, String>>, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(HashMap<Framework, HashMap<String, String>> map) {
        try {
            // Convert enum keys to string before saving
            HashMap<String, HashMap<String, String>> stringMap = new HashMap<>();
            map.forEach((k, v) -> stringMap.put(k.getValue(), v));
            return objectMapper.writeValueAsString(stringMap);
        } catch (Exception e) {
            return "{}";
        }
    }

    @Override
    public HashMap<Framework, HashMap<String, String>> convertToEntityAttribute(String json) {
        try {
            // Convert string keys back to enum when reading
            HashMap<String, HashMap<String, String>> stringMap = objectMapper.readValue(
                    json, new TypeReference<HashMap<String, HashMap<String, String>>>() {});

            HashMap<Framework, HashMap<String, String>> enumMap = new HashMap<>();
            stringMap.forEach((k, v) -> enumMap.put(Framework.fromValue(k), v));
            return enumMap;
        } catch (Exception e) {
            return new HashMap<>();
        }
    }
}