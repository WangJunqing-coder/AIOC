package com.kmu.edu.back_service.mapper;

import com.kmu.edu.back_service.entity.SysUser;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 用户Mapper接口
 */
@Mapper
public interface SysUserMapper {
    
    /**
     * 根据ID查询用户
     */
    @Select("SELECT * FROM sys_user WHERE id = #{id}")
    SysUser selectById(Long id);
    
    /**
     * 根据用户名查询用户
     */
    @Select("SELECT * FROM sys_user WHERE username = #{username}")
    SysUser selectByUsername(String username);
    
    /**
     * 根据邮箱查询用户
     */
    @Select("SELECT * FROM sys_user WHERE email = #{email}")
    SysUser selectByEmail(String email);
    
    /**
     * 根据手机号查询用户
     */
    @Select("SELECT * FROM sys_user WHERE phone = #{phone}")
    SysUser selectByPhone(String phone);
    
    /**
     * 根据账号查询用户(用户名/邮箱/手机号)
     */
    @Select("SELECT * FROM sys_user WHERE username = #{account} OR email = #{account} OR phone = #{account}")
    SysUser selectByAccount(String account);
    
    /**
     * 插入用户
     */
    @Insert("INSERT INTO sys_user (username, password, email, phone, nickname, avatar, gender, " +
            "user_type, role, status, balance, total_tokens, used_tokens) VALUES " +
            "(#{username}, #{password}, #{email}, #{phone}, #{nickname}, #{avatar}, #{gender}, " +
            "#{userType}, #{role}, #{status}, #{balance}, #{totalTokens}, #{usedTokens})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SysUser user);
    
    /**
     * 更新用户信息
     */
    @Update("UPDATE sys_user SET email = #{email}, phone = #{phone}, nickname = #{nickname}, " +
            "avatar = #{avatar}, gender = #{gender}, birthday = #{birthday}, " +
            "update_time = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updateUserInfo(SysUser user);
    
    /**
     * 更新用户密码
     */
    @Update("UPDATE sys_user SET password = #{password}, update_time = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updatePassword(@Param("id") Long id, @Param("password") String password);
    
    /**
     * 更新用户状态
     */
    @Update("UPDATE sys_user SET status = #{status}, update_time = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
    
    /**
     * 更新用户类型
     */
    @Update("UPDATE sys_user SET user_type = #{userType}, update_time = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updateUserType(@Param("id") Long id, @Param("userType") Integer userType);

        /**
         * 更新用户角色
         */
        @Update("UPDATE sys_user SET role = #{role}, update_time = CURRENT_TIMESTAMP WHERE id = #{id}")
        int updateRole(@Param("id") Long id, @Param("role") String role);
    
    /**
     * 更新用户余额
     */
    @Update("UPDATE sys_user SET balance = balance + #{amount}, update_time = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updateBalance(@Param("id") Long id, @Param("amount") Double amount);
    
    /**
     * 更新用户token
     */
    @Update("UPDATE sys_user SET total_tokens = total_tokens + #{tokens}, update_time = CURRENT_TIMESTAMP WHERE id = #{id}")
    int addTokens(@Param("id") Long id, @Param("tokens") Integer tokens);
    
    /**
     * 使用token
     */
    @Update("UPDATE sys_user SET used_tokens = used_tokens + #{tokens}, update_time = CURRENT_TIMESTAMP WHERE id = #{id}")
    int useTokens(@Param("id") Long id, @Param("tokens") Integer tokens);
    
    /**
     * 分页查询用户列表
     */
    @Select("<script>" +
            "SELECT * FROM sys_user WHERE 1=1 " +
            "<if test='username != null and username != \"\"'>" +
            "AND username LIKE CONCAT('%', #{username}, '%') " +
            "</if>" +
            "<if test='email != null and email != \"\"'>" +
            "AND email LIKE CONCAT('%', #{email}, '%') " +
            "</if>" +
            "<if test='role != null and role != \"\"'>" +
            "AND role = #{role} " +
            "</if>" +
            "<if test='status != null'>" +
            "AND status = #{status} " +
            "</if>" +
            "ORDER BY create_time DESC " +
            "LIMIT #{offset}, #{size}" +
            "</script>")
    List<SysUser> selectPage(@Param("username") String username, 
                            @Param("email") String email,
                            @Param("role") String role,
                            @Param("status") Integer status,
                            @Param("offset") Integer offset, 
                            @Param("size") Integer size);
    
    /**
     * 查询用户总数
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM sys_user WHERE 1=1 " +
            "<if test='username != null and username != \"\"'>" +
            "AND username LIKE CONCAT('%', #{username}, '%') " +
            "</if>" +
            "<if test='email != null and email != \"\"'>" +
            "AND email LIKE CONCAT('%', #{email}, '%') " +
            "</if>" +
            "<if test='role != null and role != \"\"'>" +
            "AND role = #{role} " +
            "</if>" +
            "<if test='status != null'>" +
            "AND status = #{status} " +
            "</if>" +
            "</script>")
    Long selectCount(@Param("username") String username, 
                    @Param("email") String email,
                    @Param("role") String role,
                    @Param("status") Integer status);
}