package com.itheima.aicodehelper.ai.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 消息（对应表 chat_message）
 * <p>
 * append-only 的消息流水，是"情景记忆"的唯一事实来源。
 * <p>
 * ⚠️ 注意：LangChain4j 里也有一个 {@code dev.langchain4j.data.message.ChatMessage}（接口），
 * 两者同名但不同包。需要同时使用的地方请用全限定名，或者把这个类改名为 ChatMessagePO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageRecord {

    /** 主键，bigserial */
    private Long id;

    /** 所属会话 id，外键指向 chat_session.id */
    private Long sessionId;

    /** 角色：USER / AI */
    private String role;

    /** 消息正文 */
    private String content;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 角色常量，避免各处硬编码字符串 */
    public static final String ROLE_USER = "USER";
    public static final String ROLE_AI = "AI";
}
