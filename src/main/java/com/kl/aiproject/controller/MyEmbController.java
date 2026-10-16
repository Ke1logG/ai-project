package com.kl.aiproject.controller;

import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

@RestController
public class MyEmbController {

    @Autowired
    private OpenAiEmbeddingModel openAiEmbeddingModel;

    /**
     * 简单向量模型处理数据向量化
     */
    @RequestMapping("/em")
    public String emb(){
        float[] vector = openAiEmbeddingModel.embed("Hello, world!");
        System.out.println(vector.length+" "+Arrays.toString(vector));
        return "ok";
    }

}
