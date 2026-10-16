package com.kl.aiproject.controller;

import org.springframework.ai.document.Document;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
public class MyEmbController {

    @Autowired
    private OpenAiEmbeddingModel openAiEmbeddingModel;

    @Autowired
    private VectorStore vectorStore;

    /**
     * 简单向量模型处理数据向量化
     */
    @RequestMapping("/emb")
    public String emb(){
        float[] vector = openAiEmbeddingModel.embed("Hello, world!");
        System.out.println(vector.length+" "+Arrays.toString(vector));
        return "ok";
    }


    /**
     * 简单向量数据库使用-存储
     */
    @RequestMapping("/addDoc")
    public String addDoc(){
        List<Document> doc = List.of(
                new Document("《无职转生》"),
                new Document("日本动画"),
                new Document("去旅游")
        );
        vectorStore.add(doc);
        return "ok";
    }

    /**
     * 简单向量数据库使用-查询
     */
    @RequestMapping("/query")
    public String query(){
        SearchRequest request = SearchRequest.builder()
                .query("好看的日本动画有什么？")
                .topK(2)
                .build();
        List<Document> result = vectorStore.similaritySearch(request);
        System.out.println(result);
        return "ok";
    }




}
