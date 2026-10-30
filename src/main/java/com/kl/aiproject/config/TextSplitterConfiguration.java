package com.kl.aiproject.config;

import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TextSplitterConfiguration {

    /**
     * 令牌文本切分器
     */
    @Bean
    public TokenTextSplitter tokenTextSplitter(){
        return TokenTextSplitter.builder()
                .withChunkSize(500)
                .withMaxNumChunks(1000)
                .build();

    }
}
