package com.kmu.edu.back_service.config;

import cn.dev33.satoken.stp.StpInterface;
import com.kmu.edu.back_service.entity.SysUser;
import com.kmu.edu.back_service.mapper.SysUserMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

/**
 * 将数据库中的角色信息映射给 Sa-Token，使 @SaCheckRole 注解生效。
 */
@Component
public class SaTokenRolePermissionImpl implements StpInterface {

    private final SysUserMapper userMapper;

    public SaTokenRolePermissionImpl(SysUserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return Collections.emptyList();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        if (loginId == null) {
            return Collections.emptyList();
        }
        Long uid;
        try {
            uid = Long.parseLong(String.valueOf(loginId));
        } catch (NumberFormatException ex) {
            return Collections.emptyList();
        }
        SysUser user = userMapper.selectById(uid);
        if (user == null || !StringUtils.hasText(user.getRole())) {
            return Collections.emptyList();
        }
        return List.of(user.getRole().trim().toUpperCase());
    }
}
