package com.itheima.aicodehelper.ai;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AiCodeHelperServiceTest {

    @Resource
    private AiCodeHelperService aiCodeHelperService;
    @Test
    void chatForReport() {
        String userMassage = "你好，我是一个程序员,请帮我指定学习报告";
        AiCodeHelperService.Report report = aiCodeHelperService.chatForReport(1, userMassage);
    }
    @Test
    void chatWithMassage(){
        String result = aiCodeHelperService.chat(1, "java有哪些常见面试题");
        System.out.println(result);
    }

    @Test
    void chatWithTools(){
        String result = aiCodeHelperService.chat(1, "java有哪些常见的计算机网络面试题");
        System.out.println(result);
    }
    @Test
    void chatWithGuardrail(){
        String result = aiCodeHelperService.chat(1, "human");
        System.out.println(result);
    }
    @Test
    void chatWithLog(){
        String result = aiCodeHelperService.chat(1, "java有哪些常见的计算机网络面试题");
        System.out.println(result);
    }
}