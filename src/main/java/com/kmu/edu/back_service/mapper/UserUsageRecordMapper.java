package com.kmu.edu.back_service.mapper;

import com.kmu.edu.back_service.entity.UserUsageRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserUsageRecordMapper {

    @Insert("INSERT INTO user_usage_record (user_id, service_type, tokens_used, cost, request_content, response_content, status) " +
            "VALUES (#{userId}, #{serviceType}, #{tokensUsed}, #{cost}, #{requestContent}, #{responseContent}, #{status})")
    int insert(UserUsageRecord record);

    @org.apache.ibatis.annotations.Select("SELECT * FROM user_usage_record WHERE user_id = #{userId} ORDER BY create_time DESC LIMIT #{offset}, #{size}")
    java.util.List<UserUsageRecord> selectByUserId(@org.apache.ibatis.annotations.Param("userId") Long userId,
                                                  @org.apache.ibatis.annotations.Param("offset") Integer offset,
                                                  @org.apache.ibatis.annotations.Param("size") Integer size);

    @org.apache.ibatis.annotations.Select("SELECT COUNT(*) FROM user_usage_record WHERE user_id = #{userId}")
    Long countByUserId(Long userId);
}
