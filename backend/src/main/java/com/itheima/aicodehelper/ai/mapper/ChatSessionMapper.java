package com.itheima.aicodehelper.ai.mapper;

import com.itheima.aicodehelper.ai.pojo.ChatSession;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 会话表 chat_session 的数据访问层。
 */
@Mapper
public interface ChatSessionMapper {

    /** 新建会话，回填自增主键 */
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    @Insert("INSERT INTO chat_session (title) VALUES (#{title})")
    int insert(ChatSession session);

    /** 会话列表：按最后活跃时间倒序 */
    @Select("SELECT id, title, created_at, updated_at FROM chat_session ORDER BY updated_at DESC")
    List<ChatSession> selectAll();

    @Select("SELECT id, title, created_at, updated_at FROM chat_session WHERE id = #{id}")
    ChatSession selectById(@Param("id") Long id);

    /** 只有标题还是默认值时才改名，避免覆盖用户以后手动改过的标题 */
    @Update("UPDATE chat_session SET title = #{title} WHERE id = #{id} AND title = '新会话'")
    int updateTitleIfDefault(@Param("id") Long id, @Param("title") String title);

    /** 刷新最后活跃时间 */
    @Update("UPDATE chat_session SET updated_at = NOW() WHERE id = #{id}")
    int touch(@Param("id") Long id);

    @Delete("DELETE FROM chat_session WHERE id = #{id}")
    int deleteById(@Param("id") Long id);
}
