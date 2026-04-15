package com.jinloes.grpc.ui.service;

import com.jinloes.grpc.ui.config.UiComponentProperties;
import com.jinloes.grpc.ui.proto.ComponentRequest;
import com.jinloes.grpc.ui.proto.ComponentResponse;
import com.jinloes.grpc.ui.proto.ComponentsResponse;
import com.jinloes.grpc.ui.proto.Field;
import com.jinloes.grpc.ui.proto.StateRequest;
import com.jinloes.grpc.ui.proto.StateResponse;
import com.jinloes.grpc.ui.proto.UiComponentServiceGrpc;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class UiComponentServiceImpl extends UiComponentServiceGrpc.UiComponentServiceImplBase {

  private final UiComponentProperties properties;
  // Pre-built protobuf responses keyed by componentId. Built once at startup so
  // request handling only does a map lookup rather than rebuilding protos each time.
  private final Map<String, ComponentResponse> componentRegistry = new HashMap<>();
  // Visibility conditions stored separately from the client-facing Field proto.
  // Keyed by componentId → fieldName → FieldConfig (which holds the ConditionConfig).
  // This keeps server-internal logic out of the proto entirely.
  private final Map<String, Map<String, UiComponentProperties.FieldConfig>> conditionRegistry =
      new HashMap<>();

  @PostConstruct
  public void init() {
    if (properties.getComponents() == null) {
      return;
    }
    properties
        .getComponents()
        .forEach(
            (componentId, config) -> {
              ComponentResponse response = buildComponentResponse(componentId, config);
              componentRegistry.put(componentId, response);

              // Index FieldConfig by field name so resolveState() can look up conditions
              // in O(1) without scanning the full field list on every request.
              Map<String, UiComponentProperties.FieldConfig> fieldConditions = new HashMap<>();
              if (config.getFields() != null) {
                config.getFields().forEach(f -> fieldConditions.put(f.getName(), f));
              }
              conditionRegistry.put(componentId, fieldConditions);

              log.info("Registered component: {}", componentId);
            });
  }

  @Override
  public void getComponents(
      ComponentRequest request, StreamObserver<ComponentsResponse> responseObserver) {
    log.info("Received component request for IDs: {}", request.getComponentIdsList());

    ComponentsResponse.Builder responseBuilder = ComponentsResponse.newBuilder();

    for (String componentId : request.getComponentIdsList()) {
      ComponentResponse component = componentRegistry.get(componentId);
      if (component == null) {
        responseObserver.onError(
            Status.NOT_FOUND
                .withDescription("Component not found: " + componentId)
                .asRuntimeException());
        return;
      }
      responseBuilder.addComponents(component);
    }

    responseObserver.onNext(responseBuilder.build());
    responseObserver.onCompleted();
  }

  @Override
  public void resolveState(StateRequest request, StreamObserver<StateResponse> responseObserver) {
    log.info("Resolving state for component: {}", request.getComponentId());

    ComponentResponse component = componentRegistry.get(request.getComponentId());
    if (component == null) {
      responseObserver.onError(
          Status.NOT_FOUND
              .withDescription("Component not found: " + request.getComponentId())
              .asRuntimeException());
      return;
    }

    Map<String, UiComponentProperties.FieldConfig> fieldConditions =
        conditionRegistry.getOrDefault(request.getComponentId(), Map.of());

    StateResponse.Builder builder =
        StateResponse.newBuilder().setComponentId(request.getComponentId());

    for (Field field : component.getFieldsList()) {
      UiComponentProperties.FieldConfig fieldConfig = fieldConditions.get(field.getName());
      if (evaluateVisibility(fieldConfig, request.getFieldValuesMap())) {
        builder.addFields(field);
      }
    }

    responseObserver.onNext(builder.build());
    responseObserver.onCompleted();
  }

  // Fields with no FieldConfig or no visibility condition are always shown. Missing field
  // values (fields the client hasn't populated yet) default to "" so conditions that compare
  // against a non-empty value will evaluate to hidden.
  private boolean evaluateVisibility(
      UiComponentProperties.FieldConfig fieldConfig, Map<String, String> fieldValues) {
    if (fieldConfig == null || fieldConfig.getVisible() == null) {
      return true;
    }
    UiComponentProperties.ConditionConfig condition = fieldConfig.getVisible();
    String actual = fieldValues.getOrDefault(condition.getField(), "");
    if (condition.getEquals() != null) {
      return actual.equals(condition.getEquals());
    }
    if (condition.getNotEquals() != null) {
      return !actual.equals(condition.getNotEquals());
    }
    if (condition.getIn() != null) {
      return condition.getIn().contains(actual);
    }
    if (condition.getNotIn() != null) {
      return !condition.getNotIn().contains(actual);
    }
    return true;
  }

  private ComponentResponse buildComponentResponse(
      String componentId, UiComponentProperties.ComponentConfig config) {
    List<Field> fields =
        config.getFields() == null
            ? List.of()
            : config.getFields().stream().map(this::buildField).toList();

    return ComponentResponse.newBuilder()
        .setComponentId(componentId)
        .setType(config.getType())
        .addAllFields(fields)
        .build();
  }

  private Field buildField(UiComponentProperties.FieldConfig fieldConfig) {
    Field.Builder fieldBuilder =
        Field.newBuilder()
            .setName(fieldConfig.getName())
            .setLabel(fieldConfig.getLabel())
            .setType(fieldConfig.getType())
            .setRequired(fieldConfig.isRequired());

    if (fieldConfig.getValues() != null) {
      fieldBuilder.addAllValues(fieldConfig.getValues());
    }

    return fieldBuilder.build();
  }
}
