package com.microdevcode.code.entity.convertor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.HashMap;

@Converter
public class HashMapConverter implements AttributeConverter<HashMap<String, HashMap<String, String>>, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(HashMap<String, HashMap<String, String>> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }

    @Override
    public HashMap<String, HashMap<String, String>> convertToEntityAttribute(String json) {
        try {
            return objectMapper.readValue(json,
                    new TypeReference<HashMap<String, HashMap<String, String>>>() {});
        } catch (Exception e) {
            return new HashMap<>();
        }
    }
}