package com.jinloes.todo.config;

import com.jinloes.todo.service.TodoService;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpConfig {

    @Bean
    public ToolCallbackProvider todoTools(TodoService todoService) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(todoService)
                .build();
    }
}