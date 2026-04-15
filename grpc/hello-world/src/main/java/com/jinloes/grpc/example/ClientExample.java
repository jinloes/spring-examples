package com.jinloes.grpc.example;

import com.jinloes.grpc.client.GreetingClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Example demonstrating how to use the gRPC client.
 * Enable this by running with profile: --spring.profiles.active=client-demo
 */
@Slf4j
@Component
@Profile("client-demo")
@RequiredArgsConstructor
public class ClientExample implements CommandLineRunner {

    private final GreetingClient greetingClient;

    @Override
    public void run(String... args) throws Exception {
        log.info("=== Starting gRPC Client Demo ===");

        // Unary call
        log.info("\n--- Unary Call ---");
        String response = greetingClient.sayHello("World");
        log.info("Response: {}", response);

        // Server streaming
        log.info("\n--- Server Streaming ---");
        greetingClient.sayHelloServerStreaming("Stream");

        // Client streaming
        log.info("\n--- Client Streaming ---");
        greetingClient.sayHelloClientStreaming("Alice", "Bob", "Charlie");

        // Bidirectional streaming
        log.info("\n--- Bidirectional Streaming ---");
        greetingClient.sayHelloBidirectional("David", "Emma");

        log.info("\n=== gRPC Client Demo Completed ===");
    }
}