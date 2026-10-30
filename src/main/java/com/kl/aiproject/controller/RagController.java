package com.kl.aiproject.controller;

import com.kl.aiproject.util.DocumentParseUtil;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class RagController {

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private TokenTextSplitter tokenTextSplitter;

    @Autowired
    private DocumentParseUtil documentParseUtil;

    @Autowired
    private ChatClient ragChatClient;

    /**
     * 向量化存储资料文件
     */
    @RequestMapping("/addDocs")
    public String addDocs(){
        addChunkedDocuments("D:/AI-project/ai-project/upload/project-intro.md", "project-intro.md");
        addChunkedDocuments("D:/AI-project/ai-project/upload/project-runbook.txt", "project-runbook.txt");
        return "ok";
    }

    /**
     * 实现文件转成资源对象-分块-存储
     */
    private void addChunkedDocuments(String filePath,String label){
        List<Document> raw = documentParseUtil.parse(filePath);
        List<Document> chunks = tokenTextSplitter.apply(raw);
        vectorStore.add(chunks);
        System.out.println(label+"，向量存储添加成功（共"+chunks.size()+"块）");
    }


    /**
     * 简单向量数据库使用-查询
     */
    @RequestMapping("/query2")
    public String query2(String question){
        SearchRequest request = SearchRequest.builder()
                .query(question)
                .topK(5)
                .build();
        List<Document> result = vectorStore.similaritySearch(request);
        System.out.println(result);
        return result.toString();
    }

    /**
     * 大模型根据向量数据库内容匹配最佳回答
     */
    @RequestMapping("/ask_rag")
    public String ask_rag(String question){
        String result = ragChatClient.prompt()
                .user(question)
                .call()
                .content();
        System.out.println(result);
        return result.toString();

    }

}
