package com.kl.aiproject.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {
    @Autowired
    private ChatClient chatClient;


    @GetMapping("/ai")
    public String ai(String question){
        return chatClient.prompt()
                .user(question)
                .call()
                .content();
    }
}
