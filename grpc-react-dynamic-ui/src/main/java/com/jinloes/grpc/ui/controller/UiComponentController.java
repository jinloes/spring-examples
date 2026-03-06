package com.jinloes.grpc.ui.controller;

import com.jinloes.grpc.ui.client.UiComponentClient;
import com.jinloes.grpc.ui.proto.ComponentResponse;
import com.jinloes.grpc.ui.proto.Condition;
import com.jinloes.grpc.ui.proto.Field;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/components")
@RequiredArgsConstructor
public class UiComponentController {

    private final UiComponentClient uiComponentClient;

    @GetMapping
    public List<Map<String, Object>> getComponents(@RequestParam List<String> ids) {
        return uiComponentClient.getComponents(ids).stream()
                .map(this::toMap)
                .toList();
    }

    private Map<String, Object> toMap(ComponentResponse response) {
        Map<String, Object> result = new HashMap<>();
        result.put("componentId", response.getComponentId());
        result.put("type", response.getType());
        result.put("fields", response.getFieldsList().stream().map(this::fieldToMap).toList());
        return result;
    }

    private Map<String, Object> fieldToMap(Field field) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", field.getName());
        map.put("label", field.getLabel());
        map.put("type", field.getType());
        map.put("values", field.getValuesList());
        map.put("required", field.getRequired());
        if (field.hasVisible()) {
            map.put("visible", conditionToMap(field.getVisible()));
        }
        return map;
    }

    private Map<String, Object> conditionToMap(Condition condition) {
        Map<String, Object> map = new HashMap<>();
        switch (condition.getOperatorCase()) {
            case EQUALS -> {
                map.put("field", condition.getEquals().getField());
                map.put("equals", condition.getEquals().getValue());
            }
            case NOTEQUALS -> {
                map.put("field", condition.getNotEquals().getField());
                map.put("notEquals", condition.getNotEquals().getValue());
            }
            case IN -> {
                map.put("field", condition.getIn().getField());
                map.put("in", condition.getIn().getValuesList());
            }
            case NOTIN -> {
                map.put("field", condition.getNotIn().getField());
                map.put("notIn", condition.getNotIn().getValuesList());
            }
            default -> { }
        }
        return map;
    }
}