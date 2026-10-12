package com.kl.aiproject.config;

import com.kl.aiproject.advisor.MyLoggerAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfiguration {
    @Bean 
    public ChatClient chatClient(OpenAiChatModel model,ChatMemory chatMemory){
        return ChatClient.builder(model)
                .defaultAdvisors(new SimpleLoggerAdvisor(), MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    @Bean
    public ChatClient chatClient2(OpenAiChatModel model){
        String systemPrompt = "你是一名资深Java开发工程师，只能回答Java相关问题，要求回答简洁易懂，且内容不能太长";
        return ChatClient.builder(model)
                .defaultSystem(systemPrompt)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }

    @Bean
    public ChatMemory chatMemory(){
        return MessageWindowChatMemory.builder()
                .maxMessages(10)
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .build();
    }
}
