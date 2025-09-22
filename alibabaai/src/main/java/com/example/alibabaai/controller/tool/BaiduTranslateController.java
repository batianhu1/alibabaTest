package com.example.alibabaai.controller.tool;

import io.swagger.annotations.ApiOperation;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/translate")
public class BaiduTranslateController {

    private final ChatClient dashScopeChatClient;


    public BaiduTranslateController(ChatClient chatClient) {

        this.dashScopeChatClient = chatClient;
    }

    /**
     * No Tool
     */
    @GetMapping("/chat")
    @ApiOperation("聊天-翻译")
    public String simpleChat(@RequestParam(value = "query", defaultValue = "帮我把以下内容翻译成英文：你好，世界。") String query) {

        return dashScopeChatClient.prompt(query).call().content();
    }

    /**
     * Function as Tools - Function Name
     */
    @GetMapping("/chat-tool-function-callback")
    @ApiOperation("聊天-翻译（工具调用）")
    public String chatTranslateFunction(@RequestParam(value = "query", defaultValue = "帮我把以下内容翻译成英文：你好，世界。") String query) {

        return dashScopeChatClient.prompt(query)
                .toolNames("baiduTranslate")
                .call()
                .content();
    }

}
