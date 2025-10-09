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

import com.example.alibabaai.service.mcp.McpEnvironmentDiagnosticService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/mcp/diagnosis")
public class McpDiagnosticController {

    private final McpEnvironmentDiagnosticService diagnosticService;

    public McpDiagnosticController(McpEnvironmentDiagnosticService diagnosticService) {
        this.diagnosticService = diagnosticService;
    }

    /**
     * 执行MCP环境完整诊断
     */
    @GetMapping("/full")
    public Mono<Map<String, Object>> performFullDiagnosis() {
        return diagnosticService.performFullDiagnosis();
    }

    /**
     * 健康检查端点
     */
    @GetMapping("/health")
    public Mono<Map<String, Object>> healthCheck() {
        return diagnosticService.performFullDiagnosis()
                .map(result -> {
                    Boolean healthy = (Boolean) result.get("overallHealthy");
                    return Map.of(
                        "status", healthy ? "UP" : "DOWN",
                        "healthy", healthy,
                        "message", healthy ? "All systems operational" : "Some components need attention"
                    );
                });
    }
}