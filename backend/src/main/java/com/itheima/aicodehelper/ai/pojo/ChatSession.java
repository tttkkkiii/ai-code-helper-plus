package com.itheima.aicodehelper.ai.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 会话（对应表 chat_session）
 * <p>
 * 一行 = 一次完整对话的"目录页"。
 * <p>
 * 加了 {@code @JsonInclude(NON_NULL)}：新建会话时时间和内存对象里没有值，
 * 直接不输出这两个字段，避免返回 {@code "createdAt": null} 这种看起来像出错的响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatSession {

    /** 主键，bigserial */
    private Long id;

    /** 会话标题，默认「新会话」，首次提问后自动截取生成 */
    private String title;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 最后活跃时间，每次有消息时更新，列表按它倒序 */
    private LocalDateTime updatedAt;
}
