package com.jinloes.grpc.ui.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.jinloes.grpc.ui.config.UiComponentProperties;
import com.jinloes.grpc.ui.config.UiComponentProperties.ComponentConfig;
import com.jinloes.grpc.ui.config.UiComponentProperties.ConditionConfig;
import com.jinloes.grpc.ui.config.UiComponentProperties.FieldConfig;
import com.jinloes.grpc.ui.proto.StateRequest;
import com.jinloes.grpc.ui.proto.UiComponentServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class UiComponentServiceImplTest {

  private Server server;
  private ManagedChannel channel;
  private UiComponentServiceGrpc.UiComponentServiceBlockingStub stub;

  @BeforeEach
  void setUp() throws IOException {
    UiComponentProperties props = mock(UiComponentProperties.class);
    when(props.getComponents()).thenReturn(buildTestComponents());

    UiComponentServiceImpl service = new UiComponentServiceImpl(props);
    service.init();

    String serverName = InProcessServerBuilder.generateName();
    server =
        InProcessServerBuilder.forName(serverName)
            .directExecutor()
            .addService(service)
            .build()
            .start();
    channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
    stub = UiComponentServiceGrpc.newBlockingStub(channel);
  }

  @AfterEach
  void tearDown() throws InterruptedException {
    channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
  }

  @Nested
  class NoCondition {

    @Test
    void fieldWithNoCondition_alwaysVisible() {
      assertThat(visibleFields("trigger", "anything")).contains("trigger");
    }

    @Test
    void emptyFieldValues_unconditionalFieldStillVisible() {
      StateRequest request = StateRequest.newBuilder().setComponentId("test").build();
      Set<String> fields = fieldNames(request);
      assertThat(fields).contains("trigger");
    }
  }

  @Nested
  class EqualsOperator {

    @Test
    void matchingValue_fieldVisible() {
      assertThat(visibleFields("trigger", "show")).contains("equalsField");
    }

    @Test
    void nonMatchingValue_fieldHidden() {
      assertThat(visibleFields("trigger", "other")).doesNotContain("equalsField");
    }
  }

  @Nested
  class NotEqualsOperator {

    @Test
    void nonMatchingValue_fieldVisible() {
      assertThat(visibleFields("trigger", "other")).contains("notEqualsField");
    }

    @Test
    void matchingValue_fieldHidden() {
      assertThat(visibleFields("trigger", "hide")).doesNotContain("notEqualsField");
    }
  }

  @Nested
  class InOperator {

    @Test
    void valueInList_fieldVisible() {
      assertThat(visibleFields("trigger", "b")).contains("inField");
    }

    @Test
    void valueNotInList_fieldHidden() {
      assertThat(visibleFields("trigger", "z")).doesNotContain("inField");
    }
  }

  @Nested
  class NotInOperator {

    @Test
    void valueNotInList_fieldVisible() {
      assertThat(visibleFields("trigger", "z")).contains("notInField");
    }

    @Test
    void valueInList_fieldHidden() {
      assertThat(visibleFields("trigger", "x")).doesNotContain("notInField");
    }
  }

  private Set<String> visibleFields(String field, String value) {
    return fieldNames(
        StateRequest.newBuilder().setComponentId("test").putFieldValues(field, value).build());
  }

  private Set<String> fieldNames(StateRequest request) {
    return stub.resolveState(request).getFieldsList().stream()
        .map(f -> f.getName())
        .collect(Collectors.toSet());
  }

  private Map<String, ComponentConfig> buildTestComponents() {
    ComponentConfig config = new ComponentConfig();
    config.setType("form");
    config.setFields(
        List.of(
            field("trigger", null),
            field("equalsField", equalsCondition("trigger", "show")),
            field("notEqualsField", notEqualsCondition("trigger", "hide")),
            field("inField", inCondition("trigger", List.of("a", "b", "c"))),
            field("notInField", notInCondition("trigger", List.of("x", "y")))));
    return Map.of("test", config);
  }

  private FieldConfig field(String name, ConditionConfig condition) {
    FieldConfig f = new FieldConfig();
    f.setName(name);
    f.setLabel(name);
    f.setType("text");
    f.setVisible(condition);
    return f;
  }

  private ConditionConfig equalsCondition(String field, String value) {
    ConditionConfig c = new ConditionConfig();
    c.setField(field);
    c.setEquals(value);
    return c;
  }

  private ConditionConfig notEqualsCondition(String field, String value) {
    ConditionConfig c = new ConditionConfig();
    c.setField(field);
    c.setNotEquals(value);
    return c;
  }

  private ConditionConfig inCondition(String field, List<String> values) {
    ConditionConfig c = new ConditionConfig();
    c.setField(field);
    c.setIn(values);
    return c;
  }

  private ConditionConfig notInCondition(String field, List<String> values) {
    ConditionConfig c = new ConditionConfig();
    c.setField(field);
    c.setNotIn(values);
    return c;
  }
}
