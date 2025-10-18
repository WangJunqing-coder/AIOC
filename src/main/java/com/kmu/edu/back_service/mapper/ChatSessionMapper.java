package com.kmu.edu.back_service.mapper;

import com.kmu.edu.back_service.entity.ChatSession;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 聊天会话Mapper接口
 */
@Mapper
public interface ChatSessionMapper {
    
    /**
     * 插入会话
     */
    @Insert("INSERT INTO chat_session (user_id, session_id, title, context_summary, message_count, last_message_time, status) " +
            "VALUES (#{userId}, #{sessionId}, #{title}, #{contextSummary}, #{messageCount}, #{lastMessageTime}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ChatSession session);
    
    /**
     * 根据会话ID查询
     */
    @Select("SELECT * FROM chat_session WHERE session_id = #{sessionId} AND status = 1")
    ChatSession selectBySessionId(String sessionId);
    
    /**
     * 根据用户ID查询会话列表
     */
    @Select("SELECT * FROM chat_session WHERE user_id = #{userId} AND status = 1 " +
            "ORDER BY last_message_time DESC LIMIT #{offset}, #{size}")
    List<ChatSession> selectByUserId(@Param("userId") Long userId, 
                                    @Param("offset") Integer offset, 
                                    @Param("size") Integer size);
    
    /**
     * 更新会话信息
     */
    @Update("UPDATE chat_session SET title = #{title}, context_summary = #{contextSummary}, " +
            "message_count = #{messageCount}, last_message_time = #{lastMessageTime}, " +
            "update_time = CURRENT_TIMESTAMP WHERE id = #{id}")
    int update(ChatSession session);
    
    /**
     * 增加消息数量
     */
    @Update("UPDATE chat_session SET message_count = message_count + 1, " +
            "last_message_time = CURRENT_TIMESTAMP, update_time = CURRENT_TIMESTAMP " +
            "WHERE session_id = #{sessionId}")
    int incrementMessageCount(String sessionId);
    
    /**
     * 删除会话(软删除)
     */
    @Update("UPDATE chat_session SET status = 0, update_time = CURRENT_TIMESTAMP WHERE session_id = #{sessionId}")
    int deleteBySessionId(String sessionId);
    
    /**
     * 查询用户会话总数
     */
    @Select("SELECT COUNT(*) FROM chat_session WHERE user_id = #{userId} AND status = 1")
    Long countByUserId(Long userId);
}