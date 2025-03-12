package com.atmate.portal.integration.atmateintegration.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

public class GSONFormatter {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String formatIUCJSON(String jsonInput) throws JsonProcessingException {

        JsonNode rootNode = objectMapper.readTree(jsonInput);
        JsonNode headers = rootNode.get("headers");
        JsonNode rows = rootNode.get("rows").get(0);

        Map<String, String> transformedMap = new HashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            transformedMap.put(headers.get(i).asText(), rows.get(i).asText());
        }

        return objectMapper.writeValueAsString(transformedMap);
    }

}
