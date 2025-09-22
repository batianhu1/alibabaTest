package com.example.alibabaai.controller.mcp;

import io.swagger.annotations.Api;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.AsyncMcpToolCallback;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/mcp")
@Api(tags = "MCP浏览器查询")
public class McpController {

    private final ChatClient chatClient;

    public McpController(ChatClient.Builder builder, AsyncMcpToolCallback mcpToolCallback) {
        this.chatClient = builder.defaultToolCallbacks(mcpToolCallback).build();
    }


    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> stream(@RequestParam("message") String message) {
        /**
         * messages:
         * 请帮我用 start-notification-stream 工具，每隔 1 秒推送 5 次消息，调用人叫 hy
         */
        return chatClient.prompt()
                .user(message)
                .stream()
                .content()
                .map(token -> ServerSentEvent.builder(token).build());
    }
}
