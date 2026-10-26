package com.itheima.aicodehelper.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itheima.aicodehelper.ai.AiCodeHelperService;
import com.itheima.aicodehelper.ai.service.ChatHistoryService;
import jakarta.annotation.Resource;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Map;

@RestController
@RequestMapping("/ai")
public class Aicontroller {

    @Resource
    private AiCodeHelperService aiCodeHelperService;

    @Resource
    private ChatHistoryService chatHistoryService;

    /** Spring Boot 自动配置好的 Bean，直接注入即可 */
    @Resource
    private ObjectMapper objectMapper;

    @GetMapping("/chat")
    public Flux<ServerSentEvent<String>> chat(@RequestParam long memoryId,
                                              @RequestParam String message) {

        // 用户消息先落库（同时也负责首轮自动生成会话标题）
        chatHistoryService.saveUserMessage(memoryId, message);

        // 累积完整回答，流结束后一次性落库
        StringBuilder answer = new StringBuilder();

        return aiCodeHelperService.chatStream(memoryId, message)
                .doOnNext(answer::append)
                .doOnComplete(() -> chatHistoryService.saveAiMessage(memoryId, answer.toString()))
                .map(chunk -> {
                    String data;
                    try {
                        // 包成 {"c":"原文"}，避免 SSE 规范吃掉文本开头的空格
                        data = objectMapper.writeValueAsString(Map.of("c", chunk));
                    } catch (Exception e) {
                        data = "{}";
                    }
                    return ServerSentEvent.<String>builder(data).build();
                });
    }
}
