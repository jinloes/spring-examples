package com.jinloes.grpc.ui.client;

import com.jinloes.grpc.ui.proto.ComponentRequest;
import com.jinloes.grpc.ui.proto.ComponentResponse;
import com.jinloes.grpc.ui.proto.ComponentsResponse;
import com.jinloes.grpc.ui.proto.StateRequest;
import com.jinloes.grpc.ui.proto.StateResponse;
import com.jinloes.grpc.ui.proto.UiComponentServiceGrpc;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UiComponentClient {

  @GrpcClient("grpc-server")
  private UiComponentServiceGrpc.UiComponentServiceBlockingStub stub;

  public List<ComponentResponse> getComponents(List<String> componentIds) {
    log.info("Requesting components: {}", componentIds);

    ComponentRequest request =
        ComponentRequest.newBuilder().addAllComponentIds(componentIds).build();

    ComponentsResponse response = stub.getComponents(request);
    return response.getComponentsList();
  }

  public StateResponse resolveState(String componentId, Map<String, String> fieldValues) {
    log.info("Resolving state for component: {}", componentId);
    StateRequest request =
        StateRequest.newBuilder()
            .setComponentId(componentId)
            .putAllFieldValues(fieldValues)
            .build();
    return stub.resolveState(request);
  }
}
