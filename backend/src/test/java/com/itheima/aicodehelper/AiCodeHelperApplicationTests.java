package com.itheima.aicodehelper;

import com.itheima.aicodehelper.ai.AiCodeHelper;
import com.itheima.aicodehelper.ai.AiCodeHelperService;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.service.Result;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AiCodeHelperApplicationTests {


    @Resource
    private AiCodeHelper aiCodeHelper;
    @Autowired
    private AiCodeHelperService aiCodeHelperService;

    @Test
    void chat() {
        aiCodeHelper.chat("你好，我是一个程序员");
    }
    @Test
    void chatWithMassage() {
        String result  = aiCodeHelperService.chat(1,"你好");
        System.out.println(result);
    }
    @Test
    void chatWithRag() {
        Result<String> result  = aiCodeHelperService.chatWithRag(1,"java有哪些常见面试题");
        System.out.println(result.sources());
        System.out.println(result.content());
    }
}
