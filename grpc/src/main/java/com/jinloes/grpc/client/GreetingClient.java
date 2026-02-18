package com.jinloes.grpc.client;

import com.jinloes.grpc.proto.GreetingServiceGrpc;
import com.jinloes.grpc.proto.HelloRequest;
import com.jinloes.grpc.proto.HelloResponse;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class GreetingClient {

    @GrpcClient("grpc-server")
    private GreetingServiceGrpc.GreetingServiceBlockingStub blockingStub;

    @GrpcClient("grpc-server")
    private GreetingServiceGrpc.GreetingServiceStub asyncStub;

    public String sayHello(String name) {
        log.info("Sending unary request for: {}", name);
        HelloRequest request = HelloRequest.newBuilder()
                .setName(name)
                .build();

        HelloResponse response = blockingStub.sayHello(request);
        return response.getMessage();
    }

    public void sayHelloServerStreaming(String name) throws InterruptedException {
        log.info("Sending server streaming request for: {}", name);
        HelloRequest request = HelloRequest.newBuilder()
                .setName(name)
                .build();

        CountDownLatch latch = new CountDownLatch(1);

        asyncStub.sayHelloServerStreaming(request, new StreamObserver<>() {
            @Override
            public void onNext(HelloResponse response) {
                log.info("Received server streaming response: {}", response.getMessage());
            }

            @Override
            public void onError(Throwable t) {
                log.error("Error in server streaming", t);
                latch.countDown();
            }

            @Override
            public void onCompleted() {
                log.info("Server streaming completed");
                latch.countDown();
            }
        });

        latch.await(10, TimeUnit.SECONDS);
    }

    public void sayHelloClientStreaming(String... names) throws InterruptedException {
        log.info("Sending client streaming request");
        CountDownLatch latch = new CountDownLatch(1);

        StreamObserver<HelloRequest> requestObserver = asyncStub.sayHelloClientStreaming(
                new StreamObserver<>() {
                    @Override
                    public void onNext(HelloResponse response) {
                        log.info("Received client streaming response: {}", response.getMessage());
                    }

                    @Override
                    public void onError(Throwable t) {
                        log.error("Error in client streaming", t);
                        latch.countDown();
                    }

                    @Override
                    public void onCompleted() {
                        log.info("Client streaming completed");
                        latch.countDown();
                    }
                }
        );

        for (String name : names) {
            HelloRequest request = HelloRequest.newBuilder()
                    .setName(name)
                    .build();
            requestObserver.onNext(request);
            Thread.sleep(100);
        }

        requestObserver.onCompleted();
        latch.await(10, TimeUnit.SECONDS);
    }

    public void sayHelloBidirectional(String... names) throws InterruptedException {
        log.info("Sending bidirectional streaming request");
        CountDownLatch latch = new CountDownLatch(1);

        StreamObserver<HelloRequest> requestObserver = asyncStub.sayHelloBidirectional(
                new StreamObserver<>() {
                    @Override
                    public void onNext(HelloResponse response) {
                        log.info("Received bidirectional response: {}", response.getMessage());
                    }

                    @Override
                    public void onError(Throwable t) {
                        log.error("Error in bidirectional streaming", t);
                        latch.countDown();
                    }

                    @Override
                    public void onCompleted() {
                        log.info("Bidirectional streaming completed");
                        latch.countDown();
                    }
                }
        );

        for (String name : names) {
            HelloRequest request = HelloRequest.newBuilder()
                    .setName(name)
                    .build();
            requestObserver.onNext(request);
            Thread.sleep(100);
        }

        requestObserver.onCompleted();
        latch.await(10, TimeUnit.SECONDS);
    }
}