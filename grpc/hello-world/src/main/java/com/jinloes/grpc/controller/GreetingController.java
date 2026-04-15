package com.jinloes.grpc.controller;

import com.jinloes.grpc.client.GreetingClient;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/greeting")
@RequiredArgsConstructor
public class GreetingController {

    private final GreetingClient greetingClient;

    @GetMapping("/{name}")
    public String greet(@PathVariable String name) {
        return greetingClient.sayHello(name);
    }

    @GetMapping("/stream/server/{name}")
    public String greetServerStreaming(@PathVariable String name) throws InterruptedException {
        greetingClient.sayHelloServerStreaming(name);
        return "Check logs for server streaming responses";
    }

    @GetMapping("/stream/client")
    public String greetClientStreaming() throws InterruptedException {
        greetingClient.sayHelloClientStreaming("Alice", "Bob", "Charlie");
        return "Check logs for client streaming response";
    }

    @GetMapping("/stream/bidirectional")
    public String greetBidirectional() throws InterruptedException {
        greetingClient.sayHelloBidirectional("David", "Emma", "Frank");
        return "Check logs for bidirectional streaming responses";
    }
}