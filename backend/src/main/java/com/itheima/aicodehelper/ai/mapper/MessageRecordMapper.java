package com.itheima.aicodehelper.ai.mapper;

import com.itheima.aicodehelper.ai.pojo.MessageRecord;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 消息表 chat_message 的数据访问层。
 * <p>
 * 这张表是 append-only 的"情景记忆"事实来源，只有插入和删除，没有更新。
 */
@Mapper
public interface MessageRecordMapper {

    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    @Insert("""
            INSERT INTO chat_message (session_id, role, content)
            VALUES (#{sessionId}, #{role}, #{content})
            """)
    int insert(MessageRecord record);

    /**
     * 取最近 limit 条，但按时间正序返回。
     * <p>
     * 子查询先倒序取 N 条（拿到"最近"的），外层再正序（保证回放给模型时顺序正确）。
     */
    @Select("""
            SELECT * FROM (
                SELECT id, session_id, role, content, created_at
                FROM chat_message
                WHERE session_id = #{sessionId}
                ORDER BY id DESC
                LIMIT #{limit}
            ) t ORDER BY t.id ASC
            """)
    List<MessageRecord> selectRecent(@Param("sessionId") Long sessionId,
                                     @Param("limit") int limit);

    /** 某会话的全部消息，按时间正序（前端加载历史用） */
    @Select("""
            SELECT id, session_id, role, content, created_at
            FROM chat_message
            WHERE session_id = #{sessionId}
            ORDER BY id ASC
            """)
    List<MessageRecord> selectBySessionId(@Param("sessionId") Long sessionId);

    @Delete("DELETE FROM chat_message WHERE session_id = #{sessionId}")
    int deleteBySessionId(@Param("sessionId") Long sessionId);
}
