package com.kl.aiproject.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Map;

@RestController
public class HelloController {
    @Autowired
    private ChatClient chatClient;

    /**
     * 阻塞式同步输出
     */
    @GetMapping("/ai")
    public String ai(String question){
        return chatClient.prompt()
                .user(question)
                .call()
                .content();
    }

    /**
     * 流式输出
     */
    @GetMapping(value = "/ai2",produces = "text/html;charset=UTF-8")
    public Flux<String> ai2(String question){
        return chatClient.prompt()
                .user(question)
                .stream()
                .content();
    }

    /**
     * 流式输出+提示词模板
     */
    @GetMapping(value = "ask",produces = "text/html;charset=UTF-8")
    public Flux<String> ask(String topic){
        PromptTemplate promptTemplate = new PromptTemplate("介绍下{topic}");
        Prompt prompt = promptTemplate.create(Map.of("topic",topic));
        return chatClient.prompt(prompt)
                .stream()
                .content();
    }
}
