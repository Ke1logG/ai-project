package com.kl.aiproject.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

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
}
