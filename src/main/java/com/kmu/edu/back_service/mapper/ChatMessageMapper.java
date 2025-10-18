package com.kmu.edu.back_service.mapper;

import com.kmu.edu.back_service.entity.ChatMessage;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 聊天消息Mapper接口
 */
@Mapper
public interface ChatMessageMapper {
    
    /**
     * 插入消息
     */
    @Insert("INSERT INTO chat_message (session_id, user_id, message_type, content, token_count) " +
            "VALUES (#{sessionId}, #{userId}, #{messageType}, #{content}, #{tokenCount})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ChatMessage message);
    
    /**
     * 根据会话ID查询消息列表
     */
    @Select("SELECT * FROM chat_message WHERE session_id = #{sessionId} " +
            "ORDER BY create_time ASC LIMIT #{offset}, #{size}")
    List<ChatMessage> selectBySessionId(@Param("sessionId") Long sessionId, 
                                       @Param("offset") Integer offset, 
                                       @Param("size") Integer size);
    
    /**
     * 根据会话ID查询最近的消息（用于上下文）
     */
    @Select("SELECT * FROM chat_message WHERE session_id = #{sessionId} " +
            "ORDER BY create_time DESC LIMIT #{limit}")
    List<ChatMessage> selectRecentBySessionId(@Param("sessionId") Long sessionId, 
                                             @Param("limit") Integer limit);
    
    /**
     * 根据会话ID查询消息总数
     */
    @Select("SELECT COUNT(*) FROM chat_message WHERE session_id = #{sessionId}")
    Long countBySessionId(Long sessionId);
    
    /**
     * 根据会话ID删除消息
     */
    @Delete("DELETE FROM chat_message WHERE session_id = #{sessionId}")
    int deleteBySessionId(Long sessionId);
    
    /**
     * 根据用户ID统计今日消息数量
     */
    @Select("SELECT COUNT(*) FROM chat_message WHERE user_id = #{userId} " +
            "AND message_type = 1 AND DATE(create_time) = CURDATE()")
    Long countTodayMessagesByUserId(Long userId);
}