package com.jinloes.grpc.ui.controller;

import com.jinloes.grpc.ui.client.UiComponentClient;
import com.jinloes.grpc.ui.proto.ComponentResponse;
import com.jinloes.grpc.ui.proto.Field;
import com.jinloes.grpc.ui.proto.StateResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/components")
@RequiredArgsConstructor
public class UiComponentController {

  private final UiComponentClient uiComponentClient;

  @GetMapping
  public List<Map<String, Object>> getComponents(@RequestParam List<String> ids) {
    return uiComponentClient.getComponents(ids).stream().map(this::toMap).toList();
  }

  @PostMapping("/state")
  public Map<String, Object> resolveState(@RequestBody StateRequest request) {
    StateResponse response =
        uiComponentClient.resolveState(request.componentId(), request.fieldValues());
    return Map.of(
        "componentId",
        response.getComponentId(),
        "fields",
        response.getFieldsList().stream().map(this::fieldToMap).toList());
  }

  public record StateRequest(String componentId, Map<String, String> fieldValues) {}

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
    return map;
  }
}
