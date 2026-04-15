package com.jinloes.todo.client;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Service;

@Service
public class TodoAssistant {

    private final ChatClient chatClient;

    public TodoAssistant(ChatClient.Builder builder, ToolCallbackProvider toolCallbackProvider) {
        this.chatClient = builder
                .defaultToolCallbacks(toolCallbackProvider)
                .build();
    }

    public String chat(String message) {
        return chatClient.prompt()
                .user(message)
                .call()
                .content();
    }
}