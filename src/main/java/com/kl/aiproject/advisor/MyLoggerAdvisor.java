package com.kl.aiproject.advisor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import reactor.core.publisher.Flux;

public class MyLoggerAdvisor implements CallAdvisor, StreamAdvisor {
    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        System.out.println("请求前："+chatClientRequest);
        ChatClientResponse response = callAdvisorChain.nextCall(chatClientRequest);
        System.out.println("请求后"+response);
        return response;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {
        System.out.println("请求前："+chatClientRequest);
        return streamAdvisorChain.nextStream(chatClientRequest)
                .doOnNext(chatClientResponse -> System.out.println("收到流式响应"+chatClientResponse));
    }

    @Override
    public String getName() {
        return "日志advisor";
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
