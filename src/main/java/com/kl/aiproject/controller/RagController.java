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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * RAG 相关接口：文档入库、原始向量检索，以及两种问答方式
 * （Advisor 托管 vs 手动编排）。
 */
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

    @Autowired
    private ChatClient chatClient;

    /**
     * 把 upload 下的两份语料解析、切块后写入内存向量库。
     * 文件路径是硬编码的；重复调用会重复入库，不会覆盖旧数据。
     */
    @RequestMapping("/addDocs")
    public String addDocs(){
        addChunkedDocuments("D:/AI-project/ai-project/upload/project-intro.md", "project-intro.md");
        addChunkedDocuments("D:/AI-project/ai-project/upload/project-runbook.txt", "project-runbook.txt");
        return "ok";
    }

    /**
     * 单个文件的入库流程：解析成 Document → 按 token 切块 → 调用向量模型写入。
     *
     * @param filePath 文件绝对路径
     * @param label    日志里显示的文件名
     */
    private void addChunkedDocuments(String filePath,String label){
        List<Document> raw = documentParseUtil.parse(filePath);
        List<Document> chunks = tokenTextSplitter.apply(raw);
        vectorStore.add(chunks);
        System.out.println(label+"，向量存储添加成功（共"+chunks.size()+"块）");
    }


    /**
     * 只做向量检索，不经过大模型：返回 topK=5 的 Document 字符串。
     * 适合调试检索结果和相似度分数。
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
     * 框架托管的 RAG：ragChatClient 上的 QuestionAnswerAdvisor 自动完成
     * 检索、上下文拼接和提示词增强，这里只取模型回答，因此没有来源信息。
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

    /**
     * 手动编排的 RAG：自己检索、给上下文编号，再要求模型用 [n] 标注来源，
     * 最后把回答和命中的片段一起返回。
     */
    @RequestMapping("/ask_rag_sources")
    public RagSourceAnswer askRagSources(String question){
        List<Document> docs = vectorStore.similaritySearch(SearchRequest.builder()
                .query(question)
                .topK(5)
                .build());

        String context = buildNumberedContext(docs);
        String answer = chatClient.prompt()
                .system("""
                        你是严谨的文档问答助手。只能根据用户提供的编号上下文回答，
                        不要使用上下文之外的知识；每个结论后用 [1][2] 标注来源；
                        上下文里没有答案时，直接说明无法回答。
                        """)
                .user("上下文：\n" + context + "\n问题：" + question)
                .call()
                .content();

        return new RagSourceAnswer(answer, toSources(docs));
    }

    /**
     * 把检索结果拼成带编号的上下文，例如：
     * [1] 来源：project-runbook.txt，第 3 块
     * 正文……
     * 来源优先取 source（文件名）；旧数据或自定义 Reader 没有 source 时退回 title。
     */
    private String buildNumberedContext(List<Document> docs){
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < docs.size(); i++) {
            Document doc = docs.get(i);
            Map<String, Object> metadata = doc.getMetadata();
            Object source = metadata.getOrDefault("source",
                    metadata.getOrDefault("title", "unknown"));
            sb.append("[").append(i + 1).append("] 来源：").append(source);
            Object chunkIndex = metadata.get("chunk_index");
            if (chunkIndex != null) {
                sb.append("，第 ").append(chunkIndex).append(" 块");
            }
            sb.append("\n").append(doc.getText()).append("\n\n");
        }
        return sb.toString();
    }

    /**
     * 把 Document 映射成前端可读的来源信息。
     * index 从 1 开始，与回答里的 [n] 对应；预览只保留前 120 个字符。
     */
    private List<SourceRef> toSources(List<Document> docs){
        List<SourceRef> sources = new ArrayList<>();
        for (int i = 0; i < docs.size(); i++) {
            Document doc = docs.get(i);
            Map<String, Object> metadata = doc.getMetadata();
            sources.add(new SourceRef(
                    i + 1,
                    metadata.get("source") instanceof String s ? s : null,
                    metadata.get("title") instanceof String t ? t : null,
                    metadata.get("chunk_index") instanceof Integer n ? n : null,
                    doc.getScore(),
                    preview(doc.getText(), 120)));
        }
        return sources;
    }

    /**
     * 把换行和连续空白压成单个空格，再按长度截断，避免响应过大。
     */
    private String preview(String text, int max){
        if (text == null) {
            return "";
        }
        String flat = text.replaceAll("\\s+", " ").trim();
        return flat.length() <= max ? flat : flat.substring(0, max) + "…";
    }

    /**
     * 带来源的 RAG 响应：answer 是模型回答，sources 是命中的片段。
     */
    public record RagSourceAnswer(String answer, List<SourceRef> sources) {
    }

    /**
     * 单个来源片段：index 对应回答中的 [n]，score 是向量相似度。
     */
    public record SourceRef(int index, String source, String title,
                            Integer chunkIndex, Double score, String preview) {
    }

}
