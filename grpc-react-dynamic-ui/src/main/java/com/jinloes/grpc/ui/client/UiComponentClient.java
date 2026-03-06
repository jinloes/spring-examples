package com.jinloes.grpc.ui.client;

import com.jinloes.grpc.ui.proto.ComponentRequest;
import com.jinloes.grpc.ui.proto.ComponentResponse;
import com.jinloes.grpc.ui.proto.ComponentsResponse;
import com.jinloes.grpc.ui.proto.UiComponentServiceGrpc;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class UiComponentClient {

    @GrpcClient("grpc-server")
    private UiComponentServiceGrpc.UiComponentServiceBlockingStub stub;

    public List<ComponentResponse> getComponents(List<String> componentIds) {
        log.info("Requesting components: {}", componentIds);

        ComponentRequest request = ComponentRequest.newBuilder()
                .addAllComponentIds(componentIds)
                .build();

        ComponentsResponse response = stub.getComponents(request);
        return response.getComponentsList();
    }
}