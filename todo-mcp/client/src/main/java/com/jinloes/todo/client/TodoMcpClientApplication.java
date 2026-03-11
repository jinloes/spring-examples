package com.jinloes.todo.client;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class TodoMcpClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(TodoMcpClientApplication.class, args);
    }

    @Bean
    CommandLineRunner run(TodoAssistant assistant) {
        return args -> {
            System.out.println(assistant.chat("Create a todo to buy groceries and another to walk the dog. Then list all todos."));
        };
    }
}