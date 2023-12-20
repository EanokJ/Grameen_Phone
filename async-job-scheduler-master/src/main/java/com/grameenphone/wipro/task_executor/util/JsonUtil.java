package com.grameenphone.wipro.task_executor.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class JsonUtil {
    public static ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES).disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).setDateFormat(new SimpleDateFormat("dd/MM/yyyy HH:mm:ss")).setTimeZone(TimeZone.getDefault());
    }

    public static <T> T fromJson(String source, Class<T> target) throws IOException {
        return mapper.readValue(source, target);
    }

    public static <T> T fromJson(String source, TypeReference<T> target) throws IOException {
        return mapper.readValue(source, target);
    }

    public static <T> String toJson(T source) throws JsonProcessingException {
        return mapper.writeValueAsString(source);
    }
}