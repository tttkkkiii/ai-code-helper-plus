package com.itheima.aicodehelper.ai;


import com.itheima.aicodehelper.ai.service.ChatHistoryService;
import com.itheima.aicodehelper.ai.tools.InterviewQuestionTool;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiCodeHelperServiceFactory {

    @Resource
    private ChatModel qwenChatModel;
    @Resource
    private ContentRetriever contentRetriever;
    @Resource
    private StreamingChatModel qwenStreamingChatModel;
    @Resource
    private ChatHistoryService chatHistoryService;

    @Bean
    public AiCodeHelperService createAiCodeHelperService() {
        // 会话记忆：按 sessionId 从数据库回放最近 N 条，之后由 LangChain4j 在内存里管窗口
        return AiServices.builder(AiCodeHelperService.class)
                .chatModel(qwenChatModel)
                .streamingChatModel(qwenStreamingChatModel)
                .contentRetriever(contentRetriever)
                .chatMemoryProvider(chatHistoryService::loadChatMemory)
                .tools(new InterviewQuestionTool())
                .build();
    }
}
