/*
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
package com.example.alibabaai.controller.mcp;

import com.example.alibabaai.service.mcp.BrowserMcpService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/mcp")
public class BrowserMcpController {
    
    private static final Logger logger = LoggerFactory.getLogger(BrowserMcpController.class);
    
    private final BrowserMcpService browserMcpService;
    private final ObjectMapper objectMapper;
    
    public BrowserMcpController(BrowserMcpService browserMcpService, ObjectMapper objectMapper) {
        this.browserMcpService = browserMcpService;
        this.objectMapper = objectMapper;
    }
    
    /**
     * List available tools
     */
    @GetMapping("/tools/list")
    public Mono<Map<String, Object>> listTools() {
        return Mono.just(Map.of(
            "tools", List.of(
                Map.of(
                    "name", "chrome_navigate",
                    "description", "Navigate to a URL or refresh the current tab in Chrome browser",
                    "inputSchema", Map.of(
                        "type", "object",
                        "required", List.of("url"),
                        "properties", Map.of(
                            "url", Map.of("type", "string", "description", "URL to navigate to"),
                            "newWindow", Map.of("type", "boolean", "description", "Create a new window"),
                            "width", Map.of("type", "number", "description", "Viewport width in pixels"),
                            "height", Map.of("type", "number", "description", "Viewport height in pixels")
                        )
                    )
                ),
                Map.of(
                    "name", "chrome_get_web_content",
                    "description", "Fetch content from a web page",
                    "inputSchema", Map.of(
                        "type", "object",
                        "properties", Map.of(
                            "url", Map.of("type", "string", "description", "URL to fetch content from"),
                            "textContent", Map.of("type", "boolean", "description", "Get visible text content"),
                            "htmlContent", Map.of("type", "boolean", "description", "Get HTML content"),
                            "selector", Map.of("type", "string", "description", "CSS selector to get specific element")
                        )
                    )
                )
            )
        ));
    }
    
    /**
     * Execute tool call
     */
    @PostMapping("/tools/call")
    public Mono<Map<String, Object>> callTool(@RequestBody Map<String, Object> request) {
        try {
            String name = (String) request.get("name");
            Map<String, Object> arguments = (Map<String, Object>) request.get("arguments");
            
            switch (name) {
                case "chrome_navigate":
                    return handleChromeNavigate(arguments);
                case "chrome_get_web_content":
                    return handleChromeGetWebContent(arguments);
                default:
                    return Mono.just(Map.of(
                        "isError", true,
                        "content", List.of(Map.of(
                            "type", "text",
                            "text", "Unknown tool: " + name
                        ))
                    ));
            }
        } catch (Exception e) {
            return Mono.just(Map.of(
                "isError", true,
                "content", List.of(Map.of(
                    "type", "text",
                    "text", "Error executing tool: " + e.getMessage()
                ))
            ));
        }
    }
    
    private Mono<Map<String, Object>> handleChromeNavigate(Map<String, Object> arguments) {
        String url = (String) arguments.get("url");
        Boolean newWindow = (Boolean) arguments.get("newWindow");
        Integer width = (Integer) arguments.get("width");
        Integer height = (Integer) arguments.get("height");
        
        return browserMcpService.chromeNavigate(url, newWindow, width, height)
            .map(result -> Map.of(
                "content", List.of(Map.of(
                    "type", "text",
                    "text", "Browser opened successfully! Navigated to: " + url
                ))
            ));
    }
    
    private Mono<Map<String, Object>> handleChromeGetWebContent(Map<String, Object> arguments) {
        String url = (String) arguments.get("url");
        Boolean textContent = (Boolean) arguments.get("textContent");
        Boolean htmlContent = (Boolean) arguments.get("htmlContent");
        String selector = (String) arguments.get("selector");
        
        return browserMcpService.chromeGetWebContent(url, textContent, htmlContent, selector)
            .map(result -> Map.of(
                "content", List.of(Map.of(
                    "type", "text",
                    "text", result
                ))
            ));
    }
}