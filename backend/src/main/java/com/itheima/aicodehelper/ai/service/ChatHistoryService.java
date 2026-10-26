package com.itheima.aicodehelper.ai.service;

import com.itheima.aicodehelper.ai.mapper.ChatSessionMapper;
import com.itheima.aicodehelper.ai.mapper.MessageRecordMapper;
import com.itheima.aicodehelper.ai.pojo.ChatSession;
import com.itheima.aicodehelper.ai.pojo.MessageRecord;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 会话与消息的业务层。
 * <p>
 * 职责边界：
 * <ul>
 *   <li>写库：{@link #saveUserMessage} / {@link #saveAiMessage}（append-only，完整保留历史）</li>
 *   <li>回放：{@link #loadChatMemory} 把最近 N 条装进 LangChain4j 的记忆窗口</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class ChatHistoryService {

    private static final String DEFAULT_TITLE = "新会话";

    private final ChatSessionMapper chatSessionMapper;
    private final MessageRecordMapper messageRecordMapper;

    @Value("${chat-memory.max-messages}")
    private int maxMessages;

    @Value("${chat-memory.title-max-length}")
    private int titleMaxLength;

    // ========== 会话 ==========

    /** 新建一个空会话；只返回 id 和 title，时间字段前端用不到（列表接口会给） */
    public ChatSession createSession() {
        ChatSession session = ChatSession.builder().title(DEFAULT_TITLE).build();
        chatSessionMapper.insert(session);
        return session;
    }

    /** 会话列表（按最后活跃时间倒序） */
    public List<ChatSession> listSessions() {
        return chatSessionMapper.selectAll();
    }

    /** 某个会话的全部消息（正序） */
    public List<MessageRecord> listMessages(Long sessionId) {
        return messageRecordMapper.selectBySessionId(sessionId);
    }

    /** 删除会话及其消息 */
    @Transactional
    public void deleteSession(Long sessionId) {
        messageRecordMapper.deleteBySessionId(sessionId);
        chatSessionMapper.deleteById(sessionId);
    }

    // ========== 消息落库 ==========

    /** 保存用户消息；如果这是首轮问答，顺手把默认标题改成问题摘要 */
    @Transactional
    public void saveUserMessage(Long sessionId, String content) {
        messageRecordMapper.insert(MessageRecord.builder()
                .sessionId(sessionId)
                .role(MessageRecord.ROLE_USER)
                .content(content)
                .build());

        chatSessionMapper.updateTitleIfDefault(sessionId, truncateTitle(content));
        chatSessionMapper.touch(sessionId);
    }

    /** 保存 AI 回复 */
    @Transactional
    public void saveAiMessage(Long sessionId, String content) {
        messageRecordMapper.insert(MessageRecord.builder()
                .sessionId(sessionId)
                .role(MessageRecord.ROLE_AI)
                .content(content)
                .build());

        chatSessionMapper.touch(sessionId);
    }

    // ========== 记忆回放 ==========

    /**
     * 按 sessionId 构造一条记忆：先从数据库回放最近 N 条，再交给 LangChain4j 管理窗口。
     * <p>
     * 该方法会被 AiServices 的 chatMemoryProvider 调用；
     * 同一个 sessionId 只调用一次（ChatMemoryService 内部会缓存返回值）。
     */
    public ChatMemory loadChatMemory(Object memoryId) {
        long sessionId = ((Number) memoryId).longValue();
        ChatMemory memory = MessageWindowChatMemory.withMaxMessages(maxMessages);
        messageRecordMapper.selectRecent(sessionId, maxMessages)
                .forEach(record -> memory.add(toLangChainMessage(record)));
        return memory;
    }

    /** 把数据库里的消息还原成 LangChain4j 的消息对象 */
    private ChatMessage toLangChainMessage(MessageRecord record) {
        return MessageRecord.ROLE_USER.equals(record.getRole())
                ? UserMessage.from(record.getContent())
                : AiMessage.from(record.getContent());
    }

    /** 截取标题，超长则截断（中文按字符算，够用） */
    private String truncateTitle(String text) {
        if (text == null || text.isBlank()) {
            return DEFAULT_TITLE;
        }
        String trimmed = text.strip();
        return trimmed.length() <= titleMaxLength
                ? trimmed
                : trimmed.substring(0, titleMaxLength);
    }
}
