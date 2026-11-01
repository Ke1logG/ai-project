package com.kl.aiproject.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MyMutimodalityController {

    @Autowired
    private ChatClient chatClient;


    /**
     * 测试多模态模型的图像识别能力
     */
    @RequestMapping("/analyzeImage")
    public String analyzeImage(){
        Resource image = new FileSystemResource("D:/AI-project/ai-project/upload/JLM.png");
        String result = chatClient.prompt()
                .user(u -> u.text("请简要描述这张图片的内容")
                        .media(MediaType.IMAGE_PNG, image))
                .call()
                .content();
        System.out.println(result);
        return result;
    }
}
