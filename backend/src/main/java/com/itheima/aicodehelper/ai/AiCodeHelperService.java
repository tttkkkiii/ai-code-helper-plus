package com.itheima.aicodehelper.ai;

import com.itheima.aicodehelper.ai.guardrail.SafeInputGuardrail;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.guardrail.InputGuardrails;
import reactor.core.publisher.Flux;

import java.util.List;

@InputGuardrails(SafeInputGuardrail.class)
public interface AiCodeHelperService {
    @SystemMessage(fromResource = "system-prompt.txt")
    String chat(@MemoryId long memoryId, @UserMessage String userMessage);

    @SystemMessage(fromResource = "system-prompt.txt")
    Report chatForReport(@MemoryId long memoryId, @UserMessage String userMessage);


    record Report(String name, List<String> suggestionList) {}


    @SystemMessage(fromResource = "system-prompt.txt")
    Result<String> chatWithRag(@MemoryId long memoryId, @UserMessage String userMessage);

    @SystemMessage(fromResource = "system-prompt.txt")
    Flux<String> chatStream(@MemoryId long memoryId, @UserMessage String userMessage);
}
