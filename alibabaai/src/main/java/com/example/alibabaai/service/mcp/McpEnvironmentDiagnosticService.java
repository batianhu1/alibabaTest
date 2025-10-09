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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * MCP环境诊断服务 - 检查Node.js环境、MCP脚本路径、Chrome浏览器状态等关键依赖
 */
@Service
public class McpEnvironmentDiagnosticService {

    @Value("${mcp.node-path:#{null}}")
    private String customNodePath;

    /**
     * 执行完整的环境诊断
     */
    public Mono<Map<String, Object>> performFullDiagnosis() {
        return Mono.fromCallable(() -> {
            Map<String, Object> diagnosticResult = new HashMap<>();
            
            try {
                // 1. 检查Node.js环境
                Map<String, Object> nodeCheck = checkNodeEnvironment();
                diagnosticResult.put("nodeJs", nodeCheck);
                
                // 2. 检查Chrome浏览器
                Map<String, Object> chromeCheck = checkChromeBrowser();
                diagnosticResult.put("chrome", chromeCheck);
                
                // 3. 检查MCP服务器配置  
                Map<String, Object> mcpCheck = checkMcpServerConfig();
                diagnosticResult.put("mcpServer", mcpCheck);
                
                // 4. 检查Java环境
                Map<String, Object> javaCheck = checkJavaEnvironment();
                diagnosticResult.put("java", javaCheck);
                
                // 5. 总体健康状态
                boolean overallHealthy = isOverallHealthy(nodeCheck, chromeCheck, mcpCheck, javaCheck);
                diagnosticResult.put("overallHealthy", overallHealthy);
                
                return diagnosticResult;
            } catch (Exception e) {
                diagnosticResult.put("error", "Diagnostic failed: " + e.getMessage());
                diagnosticResult.put("overallHealthy", false);
                return diagnosticResult;
            }
        });
    }

    /**
     * 检查Node.js环境（支持自动检测和手动配置）
     */
    private Map<String, Object> checkNodeEnvironment() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String nodePath = determineNodePath();
            result.put("configuredPath", nodePath);
            
            if (nodePath != null) {
                // 检查Node.js版本
                ProcessBuilder pb = new ProcessBuilder(nodePath, "--version");
                pb.redirectErrorStream(true);
                Process process = pb.start();
                
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String version = reader.readLine();
                int exitCode = process.waitFor();
                
                result.put("available", exitCode == 0);
                result.put("version", version);
                result.put("path", nodePath);
            } else {
                result.put("available", false);
                result.put("error", "Node.js not found in PATH or configured path");
            }
            
        } catch (Exception e) {
            result.put("available", false);
            result.put("error", "Failed to check Node.js: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 确定Node.js路径（优先尝试系统PATH，然后使用配置路径）
     */
    private String determineNodePath() {
        // 1. 尝试自定义配置路径
        if (customNodePath != null && !customNodePath.trim().isEmpty()) {
            Path customPath = Paths.get(customNodePath.trim());
            if (Files.exists(customPath) && Files.isExecutable(customPath)) {
                return customNodePath.trim();
            }
        }
        
        // 2. 尝试系统PATH中的node命令
        String[] nodeCommands = {"node", "nodejs"};
        for (String cmd : nodeCommands) {
            try {
                ProcessBuilder pb = new ProcessBuilder(cmd, "--version");
                Process process = pb.start();
                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    return cmd; // 在PATH中找到
                }
            } catch (Exception ignored) {
                // 继续尝试下一个命令
            }
        }
        
        return null; // 未找到Node.js
    }

    /**
     * 检查Chrome浏览器状态
     */
    private Map<String, Object> checkChromeBrowser() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String os = System.getProperty("os.name").toLowerCase();
            String[] chromeCommands;
            
            if (os.contains("win")) {
                chromeCommands = new String[]{
                    "chrome", "google-chrome", 
                    "\"C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe\"",
                    "\"C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe\""
                };
            } else if (os.contains("mac")) {
                chromeCommands = new String[]{
                    "google-chrome", "chrome",
                    "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
                };
            } else {
                chromeCommands = new String[]{"google-chrome", "chrome", "chromium-browser"};
            }
            
            for (String cmd : chromeCommands) {
                try {
                    ProcessBuilder pb = new ProcessBuilder(cmd, "--version");
                    pb.redirectErrorStream(true);
                    Process process = pb.start();
                    
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String version = reader.readLine();
                    int exitCode = process.waitFor();
                    
                    if (exitCode == 0 && version != null) {
                        result.put("available", true);
                        result.put("version", version);
                        result.put("path", cmd);
                        return result;
                    }
                } catch (Exception ignored) {
                    // 继续尝试下一个命令
                }
            }
            
            result.put("available", false);
            result.put("error", "Chrome browser not found");
            
        } catch (Exception e) {
            result.put("available", false);
            result.put("error", "Failed to check Chrome: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 检查MCP服务器配置
     */
    private Map<String, Object> checkMcpServerConfig() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 这里可以添加MCP服务器连接检查逻辑
            result.put("configured", true);
            result.put("serverUrl", "http://127.0.0.1:12306/mcp/");
            // TODO: 添加实际的连接测试
            
        } catch (Exception e) {
            result.put("configured", false);
            result.put("error", "MCP server check failed: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 检查Java环境
     */
    private Map<String, Object> checkJavaEnvironment() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String javaVersion = System.getProperty("java.version");
            String javaHome = System.getProperty("java.home");
            
            result.put("available", true);
            result.put("version", javaVersion);
            result.put("javaHome", javaHome);
            
        } catch (Exception e) {
            result.put("available", false);
            result.put("error", "Failed to check Java: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 判断总体健康状态
     */
    private boolean isOverallHealthy(Map<String, Object>... checks) {
        for (Map<String, Object> check : checks) {
            Boolean available = (Boolean) check.get("available");
            if (available == null || !available) {
                return false;
            }
        }
        return true;
    }
}