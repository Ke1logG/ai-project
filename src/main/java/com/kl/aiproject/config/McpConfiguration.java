package com.kl.aiproject.config;

import io.modelcontextprotocol.client.McpClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.mcp.McpToolFilter;
import org.springframework.ai.mcp.customizer.McpClientCustomizer;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Set;

@Configuration
public class McpConfiguration {

    /**
     * MCP 专用客户端：只挂 MCP 工具，不影响现有三个 ChatClient
     */
    @Bean
    public ChatClient mcpChatClient(OpenAiChatModel model,
            @Qualifier("mcpToolCallbacks") ToolCallbackProvider mcpToolCallbacks) {
        return ChatClient.builder(model)
                .defaultSystem("""
                        你是文件系统助手。需要查看目录或读取文件时，必须调用提供的 MCP 工具，
                        不要编造文件内容；回答时说明你调用了哪些工具。
                        """)
                .defaultTools(mcpToolCallbacks)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }

    /**
     * SDK 初始化超时默认 20s，Spring AI 没有配置项；首次 npx 冷启动可能超时，调到 120s
     */
    @Bean
    public McpClientCustomizer<McpClient.SyncSpec> mcpInitializationTimeout() {
        return (name, spec) -> spec.initializationTimeout(Duration.ofSeconds(120));
    }

    /**
     * 只把只读工具暴露给模型；filesystem server 默认还会暴露 write_file/edit_file 等
     */
    @Bean
    public McpToolFilter readOnlyMcpToolFilter() {
        Set<String> readOnlyTools = Set.of(
                "list_allowed_directories",
                "list_directory",
                "list_directory_with_sizes",
                "read_text_file",
                "read_multiple_files",
                "get_file_info",
                "search_files",
                "directory_tree");
        return (connection, tool) -> readOnlyTools.contains(tool.name());
    }
}
