package com.itheima.aicodehelper.ai.listener;

import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 大模型调用监听器：直接打印完整的 ChatRequest / ChatResponse。
 * <p>
 * 声明成 Spring Bean 后，DashScope starter 会自动把它注册到 qwenChatModel 上。
 */
@Slf4j
@Component
public class ChatModelLoggingListener implements ChatModelListener {

    @Override
    public void onRequest(ChatModelRequestContext requestContext) {
        log.info("模型请求: {}", requestContext.chatRequest());
    }

    @Override
    public void onResponse(ChatModelResponseContext responseContext) {
        log.info("模型响应: {}", responseContext.chatResponse());
    }

    @Override
    public void onError(ChatModelErrorContext errorContext) {
        log.error("模型调用失败", errorContext.error());
    }
}
