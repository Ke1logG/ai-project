package com.kl.aiproject.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/mcp")
public class McpController {

    @Autowired
    private ChatClient mcpChatClient;

    @Autowired
    @Qualifier("mcpToolCallbacks")
    private ToolCallbackProvider mcpToolCallbackProvider;

    public record McpToolInfo(String name, String description, String inputSchema) {
    }

    /**
     * 不经过大模型，直接列出 MCP Server 暴露的工具
     */
    @GetMapping("/tools")
    public List<McpToolInfo> tools() {
        return Arrays.stream(mcpToolCallbackProvider.getToolCallbacks())
                .map(ToolCallback::getToolDefinition)
                .map(def -> new McpToolInfo(def.name(), def.description(), def.inputSchema()))
                .toList();
    }

    /**
     * 完整链路：模型 -> MCP 工具 -> 结果 -> 模型
     */
    @GetMapping("/chat")
    public String chat(String question) {
        return mcpChatClient.prompt()
                .user(question)
                .call()
                .content();
    }
}
