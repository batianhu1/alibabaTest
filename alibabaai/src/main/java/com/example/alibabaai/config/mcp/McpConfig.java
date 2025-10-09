/*
 * Copyright 2025-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @author brianxiadong
 */
package com.example.alibabaai.config.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.WebClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.AsyncMcpToolCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP (Model Context Protocol) 配置类
 * 
 * 本配置类负责：
 * 1. 配置MCP客户端和传输层
 * 2. 定义可用的MCP工具（Tool）
 * 3. 设置工具回调处理
 * 4. 集成浏览器自动化功能
 */
@Configuration
@ConditionalOnProperty(name = "spring.ai.mcp.client.toolcallback.enabled", havingValue = "true", matchIfMissing = false)
public class McpConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(McpConfig.class);
    @Value("${spring.ai.mcp.client.streamable.connections.server1.url}")
    private String mcpServerUrl;
    
    @Value("${mcp.node-path:node}")
    private String nodePath;

    /**
     * MCP传输层配置 - 使用可流式HTTP传输
     * 
     * @param objectMapper JSON序列化器
     * @return WebClientStreamableHttpTransport实例
     */
    @Bean
    public WebClientStreamableHttpTransport mcpTransport(ObjectMapper objectMapper) {
        logger.info("Initializing MCP Transport with server URL: {}", mcpServerUrl);
        return WebClientStreamableHttpTransport.builder(WebClient.builder())
                .endpoint(mcpServerUrl)
                .resumableStreams(true)
                .objectMapper(objectMapper)
                .openConnectionOnStartup(true)
                .build();
    }

    /**
     * MCP异步客户端配置
     * 
     * @param transport 传输层实例
     * @return McpAsyncClient实例
     */
    @Bean
    public McpAsyncClient mcpAsyncClient(WebClientStreamableHttpTransport transport) {
        logger.info("Creating MCP Async Client");
        return McpClient.async(transport).build();
    }

    // ==================== MCP工具定义 ====================
    
    /**
     * 通知流工具 - 发送可配置的通知流
     * 
     * 对应Python MCP SDK:
     * examples/servers/simple-streamablehttp-stateless/mcp_simple_streamablehttp_stateless/server.py
     */
    @Bean
    public McpSchema.Tool startNotificationTool() {
        String inputSchema = """
                    {
                      "type": "object",
                      "required": ["interval", "count", "caller"],
                      "properties": {
                        "interval": { "type": "number", "description": "Interval between notifications in seconds" },
                        "count": { "type": "number", "description": "Number of notifications to send" },
                        "caller": { "type": "string", "description": "Identifier of the caller to include in notifications" }
                      }
                    }
                """;
        logger.debug("Registering start-notification-stream tool");
        return McpSchema.Tool.builder()
                .name("start-notification-stream")
                .description("Sends a stream of notifications with configurable count and interval")
                .inputSchema(inputSchema)
                .build();
    }

    /**
     * Chrome浏览器导航工具 - 打开指定URL
     * 
     * 支持跨平台浏览器操作（Windows/macOS/Linux）
     */
    @Bean
    public McpSchema.Tool chromeNavigateTool() {
        String inputSchema = """
                    {
                      "type": "object",
                      "required": ["url"],
                      "properties": {
                        "url": { "type": "string", "description": "URL to navigate to" },
                        "newWindow": { "type": "boolean", "description": "Create a new window" },
                        "width": { "type": "number", "description": "Viewport width in pixels" },
                        "height": { "type": "number", "description": "Viewport height in pixels" }
                      }
                    }
                """;
        logger.debug("Registering chrome_navigate tool");
        return McpSchema.Tool.builder()
                .name("chrome_navigate")
                .description("Navigate to a URL or refresh the current tab in Chrome browser")
                .inputSchema(inputSchema)
                .build();
    }

    /**
     * Chrome网页内容获取工具 - 抓取网页内容
     * 
     * 支持获取文本内容、HTML内容以及特定元素选择
     */
    @Bean
    public McpSchema.Tool chromeGetWebContentTool() {
        String inputSchema = """
                    {
                      "type": "object",
                      "properties": {
                        "url": { "type": "string", "description": "URL to fetch content from" },
                        "textContent": { "type": "boolean", "description": "Get visible text content" },
                        "htmlContent": { "type": "boolean", "description": "Get HTML content" },
                        "selector": { "type": "string", "description": "CSS selector to get specific element" }
                      }
                    }
                """;
        logger.debug("Registering chrome_get_web_content tool");
        return McpSchema.Tool.builder()
                .name("chrome_get_web_content")
                .description("Fetch content from a web page")
                .inputSchema(inputSchema)
                .build();
    }
    
    // ==================== 工具集合与回调配置 ====================

    /**
     * MCP工具列表 - 所有可用工具的集合
     * 
     * @param startNotificationTool 通知流工具
     * @param chromeNavigateTool Chrome导航工具
     * @param chromeGetWebContentTool Chrome内容获取工具
     * @return 工具列表
     */
    @Bean
    public List<McpSchema.Tool> mcpTools(
            McpSchema.Tool startNotificationTool,
            McpSchema.Tool chromeNavigateTool,
            McpSchema.Tool chromeGetWebContentTool
    ) {
        List<McpSchema.Tool> tools = List.of(
            startNotificationTool, 
            chromeNavigateTool, 
            chromeGetWebContentTool
        );
        logger.info("Registered {} MCP tools: {}", tools.size(), 
                   tools.stream().map(McpSchema.Tool::name).toList());
        return tools;
    }
    
    /**
     * MCP工具回调处理器 - 支持混合模式（外部MCP服务 + 本地工具）
     * 
     * 根据记忆中的MCP服务连接配置，Chrome MCP服务应该通过stdio方式启动
     * 但我们同时也有本地实现的浏览器工具在BrowserMcpController中
     * 
     * @param mcpAsyncClient MCP异步客户端
     * @param startNotificationTool 通知流工具（外部MCP服务）
     * @return AsyncMcpToolCallback实例
     */
    @Bean
    public AsyncMcpToolCallback mcpToolCallback(
            McpAsyncClient mcpAsyncClient,
            McpSchema.Tool startNotificationTool
    ) {
        logger.info("Creating MCP Tool Callback for external MCP service");
        logger.info("External MCP tool: {}", startNotificationTool.name());
        logger.warn("Note: chrome_navigate and chrome_get_web_content tools are handled locally by BrowserMcpController");
        
        // 仅注册外部MCP服务的工具（通知流）
        // Chrome相关工具由本地BrowserMcpController处理
        return new AsyncMcpToolCallback(mcpAsyncClient, startNotificationTool);
    }
    
    /**
     * 本地工具支持配置 - 为BrowserMcpController提供工具定义
     * 
     * 这些工具定义将被BrowserMcpController使用，实现本地浏览器操作
     * 不依赖外部MCP服务连接
     */
    @Bean
    public Map<String, McpSchema.Tool> localToolDefinitions(
            McpSchema.Tool chromeNavigateTool,
            McpSchema.Tool chromeGetWebContentTool
    ) {
        Map<String, McpSchema.Tool> localTools = new HashMap<>();
        localTools.put("chrome_navigate", chromeNavigateTool);
        localTools.put("chrome_get_web_content", chromeGetWebContentTool);
        
        logger.info("Configured local tool definitions: {}", localTools.keySet());
        return localTools;
    }
    
    // ==================== 基础配置 ====================
    
    /**
     * JSON序列化器配置
     * 
     * @return ObjectMapper实例
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
