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

import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.AsyncMcpToolCallback;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * ChatClient配置类 - 集成MCP工具回调
 * 
 * 本配置类负责：
 * 1. 配置支持MCP工具调用的ChatClient
 * 2. 集成工具回调处理器
 * 3. 为AI模型提供工具调用能力
 */
@Configuration
@ConditionalOnProperty(name = "spring.ai.mcp.client.toolcallback.enabled", havingValue = "true", matchIfMissing = false)
public class ChatClientConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatClientConfig.class);
    /**
     * 支持MCP工具调用的ChatClient
     * 
     * @param builder ChatClient构建器
     * @param mcpToolCallback MCP工具回调处理器
     * @return 配置好的ChatClient
     */
    @Bean
    public ChatClient mcpEnabledChatClient(
            ChatClient.Builder builder, 
            AsyncMcpToolCallback mcpToolCallback
    ) {
        logger.info("Creating MCP-enabled ChatClient with tool callback support");
        return builder.defaultToolCallbacks(mcpToolCallback).build();
    }
}
