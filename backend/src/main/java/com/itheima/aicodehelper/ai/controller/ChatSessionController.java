package com.itheima.aicodehelper.ai.controller;

import com.itheima.aicodehelper.ai.pojo.ChatSession;
import com.itheima.aicodehelper.ai.pojo.MessageRecord;
import com.itheima.aicodehelper.ai.service.ChatHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 会话历史接口。
 * <p>
 * 加上 server.servlet.context-path=/api 之后，实际地址为：
 * <ul>
 *   <li>POST   /api/chat/session</li>
 *   <li>GET    /api/chat/sessions</li>
 *   <li>GET    /api/chat/session/{id}</li>
 *   <li>DELETE /api/chat/session/{id}</li>
 * </ul>
 */
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatSessionController {

    private final ChatHistoryService chatHistoryService;

    /** 新建会话 */
    @PostMapping("/session")
    public ChatSession createSession() {
        return chatHistoryService.createSession();
    }

    /** 会话列表（侧边栏用） */
    @GetMapping("/sessions")
    public List<ChatSession> listSessions() {
        return chatHistoryService.listSessions();
    }

    /** 某个会话的历史消息 */
    @GetMapping("/session/{id}")
    public List<MessageRecord> listMessages(@PathVariable Long id) {
        return chatHistoryService.listMessages(id);
    }

    /** 删除会话 */
    @DeleteMapping("/session/{id}")
    public Map<String, Object> deleteSession(@PathVariable Long id) {
        chatHistoryService.deleteSession(id);
        return Map.of("id", id, "deleted", true);
    }
}
