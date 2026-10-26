package com.itheima.aicodehelper.ai.guardrail;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.InputGuardrailResult;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 输入护轨：拦截包含敏感词的用户输入。
 * <p>
 * 命中敏感词时返回 failure，LangChain4j 会中止本次调用并把失败信息作为结果返回，不会真正请求大模型。
 */
public class SafeInputGuardrail implements InputGuardrail {

    /**
     * 敏感词规则：每个词配一条按「单词边界」编译好的正则。
     * <p>
     * 为什么不用 {@code text.contains("kill")}：{@code "skill"} 里第 2～5 个字符正好是 kill，
     * 会被一起拦掉——对编程助手来说很致命（"Java skills" 直接被拒）。
     * {@code \bkill\b} 要求两侧不是单词字符：skill 不匹配，kill the process 能匹配。
     * <p>
     * 另外 CJK 字符不算单词字符，所以「把这个 kill 掉」这种中英混排也能正确命中。
     */
    private static final List<Rule> SENSITIVE_RULES = List.of(
            new Rule("kill", Pattern.compile("\\bkill\\b", Pattern.CASE_INSENSITIVE))
    );

    @Override
    public InputGuardrailResult validate(UserMessage userMessage) {
        // 只有纯文本消息才做敏感词检查，多模态消息直接放行
        if (!userMessage.hasSingleText()) {
            return success();
        }

        String text = userMessage.singleText();
        for (Rule rule : SENSITIVE_RULES) {
            if (rule.matches(text)) {
                return failure("输入包含敏感词：" + rule.word() + "，请换一种表达方式。");
            }
        }
        return success();
    }

    /** 一个敏感词 + 它的匹配规则；单独拿 word 是为了报错信息里给用户看原词而不是正则 */
    private record Rule(String word, Pattern pattern) {

        boolean matches(String text) {
            return pattern.matcher(text).find();
        }
    }
}
