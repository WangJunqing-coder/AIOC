package com.kmu.edu.back_service.mapper;

import com.kmu.edu.back_service.entity.SysConfig;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 系统配置 Mapper
 */
@Mapper
public interface SysConfigMapper {

    @Select("SELECT * FROM sys_config WHERE id = #{id}")
    SysConfig selectById(@Param("id") Long id);

    @Select("SELECT * FROM sys_config WHERE config_key = #{key}")
    SysConfig selectByKey(@Param("key") String key);

    @Select("SELECT * FROM sys_config ORDER BY create_time DESC LIMIT #{offset}, #{limit}")
    List<SysConfig> selectPage(@Param("offset") Integer offset, @Param("limit") Integer limit);

    @Select("SELECT COUNT(*) FROM sys_config")
    Long countAll();

    @Insert("INSERT INTO sys_config (config_key, config_value, config_desc, status) VALUES (#{configKey}, #{configValue}, #{configDesc}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SysConfig config);

    @Update("UPDATE sys_config SET config_value = #{configValue}, config_desc = #{configDesc}, status = #{status}, update_time = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updateById(SysConfig config);

    @Delete("DELETE FROM sys_config WHERE id = #{id}")
    int deleteById(@Param("id") Long id);
}
