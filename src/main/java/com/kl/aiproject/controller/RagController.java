package com.kl.aiproject.controller;

import com.kl.aiproject.util.DocumentParseUtil;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
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

    @RequestMapping("/addDocs")
    public String addDocs(){
        addChunkedDocuments("D:/AI-project/ai-project/upload/project-intro.md", "project-intro.md");
        addChunkedDocuments("D:/AI-project/ai-project/upload/project-runbook.txt", "project-runbook.txt");
        return "ok";
    }

    private void addChunkedDocuments(String filePath,String label){
        List<Document> raw = documentParseUtil.parse(filePath);
        List<Document> chunks = tokenTextSplitter.apply(raw);
        vectorStore.add(chunks);
        System.out.println(label+"，向量存储添加成功（共"+chunks.size()+"块）");
    }
}
