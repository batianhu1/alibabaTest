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
package com.example.alibabaai.service.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class BrowserMcpService {
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    public BrowserMcpService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
    }
    
    /**
     * Execute chrome navigate operation
     */
    public Mono<String> chromeNavigate(String url, Boolean newWindow, Integer width, Integer height) {
        return Mono.fromCallable(() -> {
            try {
                // 根据操作系统类型选择合适的命令（按照记忆中的跨平台浏览器调用模式）
                String os = System.getProperty("os.name").toLowerCase();
                ProcessBuilder pb;
                
                if (os.contains("win")) {
                    // Windows使用rundll32或cmd start
                    pb = new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url);
                    // 备用方案： pb = new ProcessBuilder("cmd", "/c", "start", "\"\"", url);
                } else if (os.contains("mac")) {
                    // macOS使用open
                    pb = new ProcessBuilder("open", url);
                } else {
                    // Linux使用xdg-open
                    pb = new ProcessBuilder("xdg-open", url);
                }
                
                // 设置进程属性
                pb.redirectErrorStream(true);
                Process process = pb.start();
                
                // 等待一小段时间确保进程启动
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                return String.format("Successfully opened browser and navigated to: %s", url);
            } catch (Exception e) {
                throw new RuntimeException("Failed to open browser: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * Execute chrome get web content operation
     */
    public Mono<String> chromeGetWebContent(String url, Boolean textContent, Boolean htmlContent, String selector) {
        return Mono.fromCallable(() -> {
            // 获取网页内容
            try {
                if (url == null || url.trim().isEmpty()) {
                    return "No URL provided for content retrieval";
                }
                
                // 使用WebClient获取网页内容
                String content = webClient.get()
                        .uri(url.trim())
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(java.time.Duration.ofSeconds(10)) // 添加超时设置
                        .block();
                
                if (content == null || content.isEmpty()) {
                    return "No content retrieved from " + url;
                }
                
                if (Boolean.TRUE.equals(textContent)) {
                    // 简单提取文本内容（移除HTML标签）
                    String textOnly = content.replaceAll("<[^>]*>", " ")
                                            .replaceAll("\\s+", " ")
                                            .trim();
                    int maxLength = Math.min(textOnly.length(), 500);
                    return "Text content from " + url + ": " + textOnly.substring(0, maxLength) + 
                           (textOnly.length() > maxLength ? "..." : "");
                } else {
                    // 返回HTML内容
                    int maxLength = Math.min(content.length(), 1000);
                    return "HTML content from " + url + ": " + content.substring(0, maxLength) + 
                           (content.length() > maxLength ? "..." : "");
                }
            } catch (Exception e) {
                return "Failed to retrieve content from " + url + ": " + e.getMessage();
            }
        });
    }
    /**
     * Parse JSON arguments for tool execution
     */
    public Map<String, Object> parseArguments(String jsonArgs) throws IOException {
        JsonNode node = objectMapper.readTree(jsonArgs);
        Map<String, Object> result = new HashMap<>();
        
        node.fields().forEachRemaining(entry -> {
            result.put(entry.getKey(), entry.getValue().asText());
        });
        
        return result;
    }
}