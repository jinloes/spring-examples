package com.jinloes.grpc.service;

import com.jinloes.grpc.proto.GreetingServiceGrpc;
import com.jinloes.grpc.proto.HelloRequest;
import com.jinloes.grpc.proto.HelloResponse;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

@Slf4j
@GrpcService
public class GreetingServiceImpl extends GreetingServiceGrpc.GreetingServiceImplBase {

    @Override
    public void sayHello(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
        log.info("Received greeting request for: {}", request.getName());

        HelloResponse response = HelloResponse.newBuilder()
                .setMessage("Hello, " + request.getName() + "!")
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void sayHelloServerStreaming(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
        log.info("Received server streaming request for: {}", request.getName());

        for (int i = 1; i <= 5; i++) {
            HelloResponse response = HelloResponse.newBuilder()
                    .setMessage("Hello " + i + ", " + request.getName() + "!")
                    .build();
            responseObserver.onNext(response);

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                responseObserver.onError(e);
                return;
            }
        }
        responseObserver.onCompleted();
    }

    @Override
    public StreamObserver<HelloRequest> sayHelloClientStreaming(StreamObserver<HelloResponse> responseObserver) {
        return new StreamObserver<>() {
            private final StringBuilder names = new StringBuilder();
            private int count = 0;

            @Override
            public void onNext(HelloRequest request) {
                log.info("Received name: {}", request.getName());
                if (count > 0) {
                    names.append(", ");
                }
                names.append(request.getName());
                count++;
            }

            @Override
            public void onError(Throwable t) {
                log.error("Error in client streaming", t);
            }

            @Override
            public void onCompleted() {
                HelloResponse response = HelloResponse.newBuilder()
                        .setMessage("Hello to all " + count + " people: " + names)
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        };
    }

    @Override
    public StreamObserver<HelloRequest> sayHelloBidirectional(StreamObserver<HelloResponse> responseObserver) {
        return new StreamObserver<>() {
            @Override
            public void onNext(HelloRequest request) {
                log.info("Received bidirectional greeting for: {}", request.getName());

                HelloResponse response = HelloResponse.newBuilder()
                        .setMessage("Hello back to you, " + request.getName() + "!")
                        .build();
                responseObserver.onNext(response);
            }

            @Override
            public void onError(Throwable t) {
                log.error("Error in bidirectional streaming", t);
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }
        };
    }
}