package com.kl.aiproject.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfiguration {
    public ChatClient chatClient(OpenAiChatModel model){
        return ChatClient.builder(model).build();
    }
}
