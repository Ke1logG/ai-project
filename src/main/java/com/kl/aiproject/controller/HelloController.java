package com.kl.aiproject.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@RestController
public class HelloController {
    @Autowired
    private ChatClient chatClient;

    @Autowired
    private ChatClient chatClient2;

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

    /**
     * 流式输出+流式提示词API
     */
    @GetMapping(value = "ask2",produces = "text/html;charset=UTF-8")
    public Flux<String> ask2(String topic){
        return chatClient.prompt()
                .system("你是一个专业的书评助手")
                .user(u->u.text("请给我推荐3本关于{topic}的书籍,只输出书名").param("topic",topic))
                .stream()
                .content();
    }


    /**
     * 简单系统提示词测试
     */
    @GetMapping("/ask3")
    public String ask3(String question){
        return chatClient2.prompt()
                .user(question)
                .call()
                .content();
    }

    /**
     * record定义简单一次性类
     */
    public record TopicBook(String topic, List<String> books){}


    /**
     * 结构化输出，将回答映射为Java对象
     */
    @GetMapping(value = "ask4")
    public String ask4(String topic){
        TopicBook topicBook =  chatClient.prompt()
                .system("你是一个专业的书评助手")
                .user(u->u.text("请给我推荐3本关于{topic}的书籍,只输出书名").param("topic",topic))
                .call()
                .entity(TopicBook.class);
        System.out.println(topicBook);
        return topicBook+"";

    }

    /**
     * record定义简单一次性类
     */
    public record BookReview(String reviewerName, String comment){}


    /**
     * 结构化输出，将回答映射为Java集合对象
     */
    @GetMapping(value = "ask5")
    public String ask5(String topic){
        List<BookReview> bookReview =  chatClient.prompt()
                .system("你是一个专业的书评助手")
                .user(u->u.text("请给我三条关于书籍“{topic}”的评价,输出评价人和评价内容").param("topic",topic))
                .call()
                .entity(new ParameterizedTypeReference<List<BookReview>>() {
                });
        System.out.println(bookReview);
        return bookReview+"";

    }


    /**
     * 大模型记忆功能与多用户记忆隔离
     */
    @GetMapping("/ai3")
    public String ai3(String question,String Id){
        return chatClient.prompt()
                .user(question)
                .advisors(a->a.param(ChatMemory.CONVERSATION_ID,Id))
                .call()
                .content();
    }
}
