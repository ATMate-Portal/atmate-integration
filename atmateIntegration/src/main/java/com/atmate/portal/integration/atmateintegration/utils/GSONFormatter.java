package com.atmate.portal.integration.atmateintegration.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class GSONFormatter {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public List<Map<String, String>> formatIUCJSON(String jsonInput) throws JsonProcessingException {
        JsonNode rootNode = objectMapper.readTree(jsonInput);
        JsonNode headers = rootNode.get("headers");
        JsonNode rows = rootNode.get("rows");

        List<Map<String, String>> resultList = new ArrayList<>();

        for (JsonNode row : rows) {
            Map<String, String> transformedMap = new HashMap<>();
            for (int i = 0; i < headers.size(); i++) {
                transformedMap.put(headers.get(i).asText(), row.get(i).asText());
            }
            resultList.add(transformedMap);
        }

        return resultList;
    }
}