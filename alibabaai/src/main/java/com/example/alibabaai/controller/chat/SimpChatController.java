package com.example.alibabaai.controller.chat;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.dashscope.chat.MessageFormat;
import com.alibaba.cloud.ai.dashscope.common.DashScopeApiConstants;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.http.MediaType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

@RestController
@Api(tags = "聊天")
public class SimpChatController {
    // 默认ChatClient
    private final ChatClient chatClient;


    public SimpChatController(ChatModel chatModel) {

        // 构建默认ChatClient
        this.chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .defaultOptions(
                        DashScopeChatOptions.builder()
                                .withTopP(0.7)
                                .build()
                )
                .build();
                
    }

    // 也可以使用如下的方式注入 ChatClient 这种方式需要配置 #spring.ai.chat.client.enabled=false
    // public DashScopeChatClientController(ChatClient.Builder chatClientBuilder) {
    //
    //  	this.dashScopeChatClient = chatClientBuilder.build();
    // }

    @GetMapping("/chat")
    @ApiOperation("聊天-默认模型")
    public String chat(String input) {
        return this.chatClient.prompt()
                .user(input)
                .call()
                .content();
    }

    @GetMapping(value = "/stream/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ApiOperation("聊天-默认模型（stream）")
    @ResponseBody
    public Flux<String> streamChat(HttpServletResponse response, String input) {
        response.setCharacterEncoding("UTF-8");
        // 设置响应头，否则，stream不生效
        response.setContentType("text/event-stream");
        return this.chatClient.prompt(input).stream().content();
    }

    @PostMapping("/image/analyze/upload")
    @ApiOperation("聊天-图片分析")
    public String analyzeImageByUpload(@RequestParam(defaultValue = "请分析这张图片的内容") String prompt,
                                       @RequestParam("file") MultipartFile file) {
        try {
            // 验证文件类型
            if (!file.getContentType().startsWith("image/")) {
                return "请上传图片文件";
            }

            // 创建包含图片的用户消息
            Media media = new Media(MimeTypeUtils.parseMimeType(file.getContentType()), file.getResource());
            UserMessage message = UserMessage.builder()
                    .text(prompt)
                    .media(media)
                    .build();

            // 设置消息格式为图片
            message.getMetadata().put(DashScopeApiConstants.MESSAGE_FORMAT, MessageFormat.IMAGE);

            // 创建提示词，启用多模态模型
            Prompt chatPrompt = new Prompt(message,
                    DashScopeChatOptions.builder()
                            .withModel("qwen-vl-max-latest")  // 使用视觉模型
                            .withMultiModel(true)             // 启用多模态
                            .withVlHighResolutionImages(true) // 启用高分辨率图片处理
                            .withTemperature(0.7)
                            .build());

            // 调用模型进行图片分析
            return chatClient.prompt(chatPrompt).call().content();

        } catch (Exception e) {
            return "图片分析失败: " + e.getMessage();
        }
    }

}