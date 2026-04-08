package com.jinloes.grpc.ui.config;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

@Slf4j
@Component
public class UiComponentProperties implements InitializingBean {

  @Autowired private ResourcePatternResolver resourcePatternResolver;

  private final Map<String, ComponentConfig> components = new LinkedHashMap<>();

  public Map<String, ComponentConfig> getComponents() {
    return components;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void afterPropertiesSet() throws IOException {
    Yaml yaml = new Yaml();
    // Discovers all component YAML files at startup. The filename (without .yml)
    // becomes the componentId used to look up the component at runtime.
    Resource[] resources = resourcePatternResolver.getResources("classpath:components/*.yml");
    for (Resource resource : resources) {
      String componentId = resource.getFilename().replace(".yml", "");
      Map<String, Object> doc = yaml.load(resource.getInputStream());
      components.put(componentId, parseComponentConfig(doc));
      log.info("Loaded component: {}", componentId);
    }
  }

  @SuppressWarnings("unchecked")
  private ComponentConfig parseComponentConfig(Map<String, Object> data) {
    ComponentConfig config = new ComponentConfig();
    config.setType((String) data.get("type"));
    List<Map<String, Object>> fieldsList = (List<Map<String, Object>>) data.get("fields");
    if (fieldsList != null) {
      config.setFields(fieldsList.stream().map(this::parseFieldConfig).toList());
    }
    return config;
  }

  @SuppressWarnings("unchecked")
  private FieldConfig parseFieldConfig(Map<String, Object> data) {
    FieldConfig field = new FieldConfig();
    field.setName((String) data.get("name"));
    field.setLabel((String) data.get("label"));
    field.setType((String) data.get("type"));
    field.setRequired(Boolean.TRUE.equals(data.get("required")));
    field.setValues((List<String>) data.get("values"));
    Map<String, Object> visible = (Map<String, Object>) data.get("visible");
    if (visible != null) {
      field.setVisible(parseConditionConfig(visible));
    }
    return field;
  }

  @SuppressWarnings("unchecked")
  private ConditionConfig parseConditionConfig(Map<String, Object> data) {
    ConditionConfig condition = new ConditionConfig();
    condition.setField((String) data.get("field"));
    condition.setEquals((String) data.get("equals"));
    condition.setNotEquals((String) data.get("notEquals"));
    condition.setIn((List<String>) data.get("in"));
    condition.setNotIn((List<String>) data.get("notIn"));
    return condition;
  }

  @Data
  public static class ComponentConfig {
    private String type;
    private List<FieldConfig> fields;
  }

  @Data
  public static class FieldConfig {
    private String name;
    private String label;
    private String type;
    private List<String> values;
    private boolean required;
    private ConditionConfig visible;
  }

  @Data
  public static class ConditionConfig {
    private String field;
    private String equals;
    private String notEquals;
    private List<String> in;
    private List<String> notIn;
  }
}
