package com.smagesci.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonUtil {

    private static final ObjectMapper objectMapper = createObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(JsonUtil.class);

    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule()); // Para soportar Java 8 Date/Time API (LocalDateTime, etc.)
        // Se pueden añadir más configuraciones aquí si es necesario
        // mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

    public static String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Error serializando objeto a JSON: {}", obj, e);
            return null; // O lanzar una excepción personalizada
        }
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("Error deserializando JSON a objeto: {}. JSON: {}", clazz.getSimpleName(), json, e);
            return null; // O lanzar una excepción personalizada
        } catch (IllegalArgumentException e) {
            log.error("Invalid JSON input for class: {}. JSON: {}", clazz.getSimpleName(), json, e);
            return null;
        }
    }
}
