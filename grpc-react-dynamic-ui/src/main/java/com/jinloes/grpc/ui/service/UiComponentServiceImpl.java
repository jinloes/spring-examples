package com.jinloes.grpc.ui.service;

import com.jinloes.grpc.ui.config.UiComponentProperties;
import com.jinloes.grpc.ui.proto.ComponentRequest;
import com.jinloes.grpc.ui.proto.ComponentResponse;
import com.jinloes.grpc.ui.proto.ComponentsResponse;
import com.jinloes.grpc.ui.proto.Condition;
import com.jinloes.grpc.ui.proto.EqualsOperator;
import com.jinloes.grpc.ui.proto.Field;
import com.jinloes.grpc.ui.proto.InOperator;
import com.jinloes.grpc.ui.proto.NotEqualsOperator;
import com.jinloes.grpc.ui.proto.NotInOperator;
import com.jinloes.grpc.ui.proto.UiComponentServiceGrpc;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class UiComponentServiceImpl extends UiComponentServiceGrpc.UiComponentServiceImplBase {

    private final UiComponentProperties properties;
    private final Map<String, ComponentResponse> componentRegistry = new HashMap<>();

    @PostConstruct
    public void init() {
        if (properties.getComponents() == null) {
            return;
        }
        properties.getComponents().forEach((componentId, config) -> {
            ComponentResponse response = buildComponentResponse(componentId, config);
            componentRegistry.put(componentId, response);
            log.info("Registered component: {}", componentId);
        });
    }

    @Override
    public void getComponents(ComponentRequest request, StreamObserver<ComponentsResponse> responseObserver) {
        log.info("Received component request for IDs: {}", request.getComponentIdsList());

        ComponentsResponse.Builder responseBuilder = ComponentsResponse.newBuilder();

        for (String componentId : request.getComponentIdsList()) {
            ComponentResponse component = componentRegistry.get(componentId);
            if (component == null) {
                responseObserver.onError(Status.NOT_FOUND
                        .withDescription("Component not found: " + componentId)
                        .asRuntimeException());
                return;
            }
            responseBuilder.addComponents(component);
        }

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }

    private ComponentResponse buildComponentResponse(String componentId, UiComponentProperties.ComponentConfig config) {
        List<Field> fields = config.getFields() == null ? List.of() : config.getFields().stream()
                .map(this::buildField)
                .toList();

        return ComponentResponse.newBuilder()
                .setComponentId(componentId)
                .setType(config.getType())
                .addAllFields(fields)
                .build();
    }

    private Field buildField(UiComponentProperties.FieldConfig fieldConfig) {
        Field.Builder fieldBuilder = Field.newBuilder()
                .setName(fieldConfig.getName())
                .setLabel(fieldConfig.getLabel())
                .setType(fieldConfig.getType())
                .setRequired(fieldConfig.isRequired());

        if (fieldConfig.getValues() != null) {
            fieldBuilder.addAllValues(fieldConfig.getValues());
        }

        if (fieldConfig.getVisible() != null) {
            fieldBuilder.setVisible(buildCondition(fieldConfig.getVisible()));
        }

        return fieldBuilder.build();
    }

    private Condition buildCondition(UiComponentProperties.ConditionConfig conditionConfig) {
        String field = conditionConfig.getField();
        Condition.Builder conditionBuilder = Condition.newBuilder();

        if (conditionConfig.getEquals() != null) {
            conditionBuilder.setEquals(EqualsOperator.newBuilder().setField(field).setValue(conditionConfig.getEquals()).build());
        } else if (conditionConfig.getNotEquals() != null) {
            conditionBuilder.setNotEquals(NotEqualsOperator.newBuilder().setField(field).setValue(conditionConfig.getNotEquals()).build());
        } else if (conditionConfig.getIn() != null) {
            conditionBuilder.setIn(InOperator.newBuilder().setField(field).addAllValues(conditionConfig.getIn()).build());
        } else if (conditionConfig.getNotIn() != null) {
            conditionBuilder.setNotIn(NotInOperator.newBuilder().setField(field).addAllValues(conditionConfig.getNotIn()).build());
        }

        return conditionBuilder.build();
    }
}