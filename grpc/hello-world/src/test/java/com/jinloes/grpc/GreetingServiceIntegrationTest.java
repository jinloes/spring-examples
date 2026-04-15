package com.jinloes.grpc;

import com.jinloes.grpc.proto.GreetingServiceGrpc;
import com.jinloes.grpc.proto.HelloRequest;
import com.jinloes.grpc.proto.HelloResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class GreetingServiceIntegrationTest {

    @GrpcClient("inProcess")
    private GreetingServiceGrpc.GreetingServiceBlockingStub blockingStub;

    @GrpcClient("inProcess")
    private GreetingServiceGrpc.GreetingServiceStub asyncStub;

    @Test
    void testSayHello() {
        HelloRequest request = HelloRequest.newBuilder()
                .setName("World")
                .build();

        HelloResponse response = blockingStub.sayHello(request);

        assertThat(response.getMessage()).isEqualTo("Hello, World!");
    }

    @Test
    void testSayHelloServerStreaming() throws InterruptedException {
        HelloRequest request = HelloRequest.newBuilder()
                .setName("Stream")
                .build();

        CountDownLatch latch = new CountDownLatch(1);
        List<String> responses = new ArrayList<>();

        asyncStub.sayHelloServerStreaming(request, new StreamObserver<>() {
            @Override
            public void onNext(HelloResponse response) {
                responses.add(response.getMessage());
            }

            @Override
            public void onError(Throwable t) {
                latch.countDown();
            }

            @Override
            public void onCompleted() {
                latch.countDown();
            }
        });

        latch.await(10, TimeUnit.SECONDS);
        assertThat(responses).hasSize(5);
        assertThat(responses.get(0)).isEqualTo("Hello 1, Stream!");
        assertThat(responses.get(4)).isEqualTo("Hello 5, Stream!");
    }

    @Test
    void testSayHelloClientStreaming() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        List<String> responses = new ArrayList<>();

        StreamObserver<HelloRequest> requestObserver = asyncStub.sayHelloClientStreaming(
                new StreamObserver<>() {
                    @Override
                    public void onNext(HelloResponse response) {
                        responses.add(response.getMessage());
                    }

                    @Override
                    public void onError(Throwable t) {
                        latch.countDown();
                    }

                    @Override
                    public void onCompleted() {
                        latch.countDown();
                    }
                }
        );

        String[] names = {"Alice", "Bob", "Charlie"};
        for (String name : names) {
            HelloRequest request = HelloRequest.newBuilder()
                    .setName(name)
                    .build();
            requestObserver.onNext(request);
        }
        requestObserver.onCompleted();

        latch.await(10, TimeUnit.SECONDS);
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0)).contains("Hello to all 3 people: Alice, Bob, Charlie");
    }

    @Test
    void testSayHelloBidirectional() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        List<String> responses = new ArrayList<>();

        StreamObserver<HelloRequest> requestObserver = asyncStub.sayHelloBidirectional(
                new StreamObserver<>() {
                    @Override
                    public void onNext(HelloResponse response) {
                        responses.add(response.getMessage());
                    }

                    @Override
                    public void onError(Throwable t) {
                        latch.countDown();
                    }

                    @Override
                    public void onCompleted() {
                        latch.countDown();
                    }
                }
        );

        String[] names = {"David", "Emma"};
        for (String name : names) {
            HelloRequest request = HelloRequest.newBuilder()
                    .setName(name)
                    .build();
            requestObserver.onNext(request);
            Thread.sleep(100);
        }
        requestObserver.onCompleted();

        latch.await(10, TimeUnit.SECONDS);
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0)).isEqualTo("Hello back to you, David!");
        assertThat(responses.get(1)).isEqualTo("Hello back to you, Emma!");
    }
}
