package com.kmu.edu.back_service.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.kmu.edu.back_service.common.ResultCode;
import com.kmu.edu.back_service.constant.Constants;
import com.kmu.edu.back_service.dto.request.UserLoginRequest;
import com.kmu.edu.back_service.dto.request.UserRegisterRequest;
import com.kmu.edu.back_service.dto.response.UserInfoResponse;
import com.kmu.edu.back_service.entity.SysUser;
import com.kmu.edu.back_service.exception.BusinessException;
import com.kmu.edu.back_service.mapper.SysUserMapper;
import com.kmu.edu.back_service.service.SysUserService;
import com.kmu.edu.back_service.utils.CommonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * 用户服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl implements SysUserService {
    
    private final SysUserMapper userMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    
    @Override
    @Transactional
    public void register(UserRegisterRequest request) {
        // 校验密码一致性
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "两次输入的密码不一致");
        }
        
        // 检查用户名是否存在
        if (userMapper.selectByUsername(request.getUsername()) != null) {
            throw new BusinessException(ResultCode.USERNAME_EXIST);
        }
        
        // 检查邮箱是否存在
        if (userMapper.selectByEmail(request.getEmail()) != null) {
            throw new BusinessException(ResultCode.EMAIL_EXIST);
        }
        
        // 检查手机号是否存在
        if (CommonUtils.isNotEmpty(request.getPhone()) && userMapper.selectByPhone(request.getPhone()) != null) {
            throw new BusinessException(ResultCode.PHONE_EXIST);
        }
        
        // 创建用户
        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(CommonUtils.encryptPassword(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setNickname(CommonUtils.isNotEmpty(request.getNickname()) ? request.getNickname() : request.getUsername());
        user.setAvatar(Constants.DefaultValue.DEFAULT_AVATAR);
        user.setGender(Constants.Gender.UNKNOWN);
        user.setUserType(Constants.UserType.NORMAL);
    user.setRole(Constants.Role.USER);
        user.setStatus(Constants.UserStatus.ENABLED);
        user.setBalance(BigDecimal.ZERO);
        user.setTotalTokens(Constants.DefaultValue.DEFAULT_TOKENS);
        user.setUsedTokens(0);
        
        int result = userMapper.insert(user);
        if (result <= 0) {
            throw new BusinessException("用户注册失败");
        }
        
        log.info("用户注册成功：{}", user.getUsername());
    }
    
    @Override
    public String login(UserLoginRequest request) {
        // 根据账号查询用户
        SysUser user = userMapper.selectByAccount(request.getAccount());
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        
        // 检查用户状态
        if (user.getStatus() == Constants.UserStatus.DISABLED) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }
        
        // 验证密码
        if (!CommonUtils.checkPassword(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }
        
        // 登录成功，生成token
        StpUtil.login(user.getId());
        String token = StpUtil.getTokenValue();
        
        // 缓存用户信息
        String userCacheKey = Constants.CacheKey.USER_INFO + user.getId();
        redisTemplate.opsForValue().set(userCacheKey, user, 24, TimeUnit.HOURS);
        
        log.info("用户登录成功：{}", user.getUsername());
        return token;
    }
    
    @Override
    public void logout() {
        Long userId = StpUtil.getLoginIdAsLong();
        
        // 清除用户缓存
        String userCacheKey = Constants.CacheKey.USER_INFO + userId;
        redisTemplate.delete(userCacheKey);
        
        // 登出
        StpUtil.logout();
        
        log.info("用户登出成功：{}", userId);
    }
    
    @Override
    public UserInfoResponse getCurrentUserInfo() {
        Long userId = StpUtil.getLoginIdAsLong();
        
        // 先从缓存获取
        String userCacheKey = Constants.CacheKey.USER_INFO + userId;
        Object cached = redisTemplate.opsForValue().get(userCacheKey);
        SysUser user = null;
        if (cached instanceof SysUser) {
            user = (SysUser) cached;
        } else if (cached != null) {
            // 兼容历史缓存中存的是 LinkedHashMap 的情况：直接回库并刷新缓存
            user = userMapper.selectById(userId);
            if (user != null) {
                redisTemplate.opsForValue().set(userCacheKey, user, 24, TimeUnit.HOURS);
            }
        }
        
        if (user == null) {
            // 缓存不存在，从数据库查询
            user = userMapper.selectById(userId);
            if (user == null) {
                throw new BusinessException(ResultCode.USER_NOT_FOUND);
            }
            // 更新缓存
            redisTemplate.opsForValue().set(userCacheKey, user, 24, TimeUnit.HOURS);
        }
        
        // 转换为响应DTO
        UserInfoResponse response = new UserInfoResponse();
        BeanUtils.copyProperties(user, response);
        
        // 设置用户类型名称
        switch (user.getUserType()) {
            case Constants.UserType.NORMAL:
                response.setUserTypeName("普通用户");
                break;
            case Constants.UserType.VIP:
                response.setUserTypeName("VIP用户");
                break;
            case Constants.UserType.SUPER_VIP:
                response.setUserTypeName("超级VIP用户");
                break;
            default:
                response.setUserTypeName("未知");
        }
        
        // 计算剩余token
        response.setRemainingTokens(user.getTotalTokens() - user.getUsedTokens());
        
        // 获取今日使用次数
        response.setTodayUsage(getTodayUsage(userId));
        
        // 设置今日使用限制（统一展示用：生成类的日限；聊天为 5000 token 不在此处体现）
        switch (user.getUserType()) {
            case Constants.UserType.NORMAL:
                response.setDailyLimit(Constants.DefaultValue.NORMAL_IMAGE_DAILY);
                break;
            case Constants.UserType.VIP:
                response.setDailyLimit(Constants.DefaultValue.VIP_IMAGE_DAILY);
                break;
            case Constants.UserType.SUPER_VIP:
                response.setDailyLimit(Constants.DefaultValue.UNLIMITED); // -1 表示不限
                break;
            default:
                response.setDailyLimit(0);
        }
        
        return response;
    }
    
    @Override
    public SysUser getUserById(Long id) {
        return userMapper.selectById(id);
    }
    
    @Override
    public SysUser getUserByUsername(String username) {
        return userMapper.selectByUsername(username);
    }
    
    @Override
    @Transactional
    public void updateUserInfo(SysUser user) {
        Long userId = StpUtil.getLoginIdAsLong();
        user.setId(userId);
        
        int result = userMapper.updateUserInfo(user);
        if (result <= 0) {
            throw new BusinessException("更新用户信息失败");
        }
        
        // 清除缓存
        String userCacheKey = Constants.CacheKey.USER_INFO + userId;
        redisTemplate.delete(userCacheKey);
        
        log.info("用户信息更新成功：{}", userId);
    }
    
    @Override
    @Transactional
    public void changePassword(String oldPassword, String newPassword) {
        Long userId = StpUtil.getLoginIdAsLong();
        SysUser user = userMapper.selectById(userId);
        
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        
        // 验证原密码
        if (!CommonUtils.checkPassword(oldPassword, user.getPassword())) {
            throw new BusinessException(ResultCode.OLD_PASSWORD_ERROR);
        }
        
        // 更新密码
        String encryptedPassword = CommonUtils.encryptPassword(newPassword);
        int result = userMapper.updatePassword(userId, encryptedPassword);
        if (result <= 0) {
            throw new BusinessException("密码修改失败");
        }
        
        log.info("用户密码修改成功：{}", userId);
    }
    
    @Override
    @Transactional
    public void resetPassword(String email, String newPassword) {
        SysUser user = userMapper.selectByEmail(email);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        
        // 更新密码
        String encryptedPassword = CommonUtils.encryptPassword(newPassword);
        int result = userMapper.updatePassword(user.getId(), encryptedPassword);
        if (result <= 0) {
            throw new BusinessException("密码重置失败");
        }
        
        log.info("用户密码重置成功：{}", email);
    }
    
    @Override
    @Transactional
    public void recharge(Long userId, Double amount) {
        int result = userMapper.updateBalance(userId, amount);
        if (result <= 0) {
            throw new BusinessException("充值失败");
        }
        
        // 清除缓存
        String userCacheKey = Constants.CacheKey.USER_INFO + userId;
        redisTemplate.delete(userCacheKey);
        
        log.info("用户充值成功：用户ID={}, 金额={}", userId, amount);
    }
    
    @Override
    @Transactional
    public void addTokens(Long userId, Integer tokens) {
        int result = userMapper.addTokens(userId, tokens);
        if (result <= 0) {
            throw new BusinessException("Token增加失败");
        }
        
        // 清除缓存
        String userCacheKey = Constants.CacheKey.USER_INFO + userId;
        redisTemplate.delete(userCacheKey);
        
        log.info("用户Token增加成功：用户ID={}, 数量={}", userId, tokens);
    }
    
    @Override
    @Transactional
    public boolean useTokens(Long userId, Integer tokens) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            return false;
        }
        
        // 检查token余额
        int remainingTokens = user.getTotalTokens() - user.getUsedTokens();
        if (remainingTokens < tokens) {
            throw new BusinessException(ResultCode.INSUFFICIENT_TOKENS);
        }
        
        int result = userMapper.useTokens(userId, tokens);
        if (result <= 0) {
            return false;
        }
        
        // 清除缓存
        String userCacheKey = Constants.CacheKey.USER_INFO + userId;
        redisTemplate.delete(userCacheKey);
        
        log.info("用户Token使用成功：用户ID={}, 数量={}", userId, tokens);
        return true;
    }
    
    @Override
    public Integer getTodayUsage(Long userId) {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String usageCacheKey = Constants.CacheKey.DAILY_USAGE + userId + ":" + today;
        Object usage = redisTemplate.opsForValue().get(usageCacheKey);
        return usage != null ? (Integer) usage : 0;
    }
    
    @Override
    public boolean checkDailyLimit(Long userId, Integer serviceType) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            return false;
        }

        // 超级 VIP 全部不限量
        if (user.getUserType() == Constants.UserType.SUPER_VIP) {
            incrementLegacyTotalCount(userId, 1);
            return true;
        }

        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String key;
        int limit; // -1 表示不限
        int inc = 1;

        switch (serviceType) {
            case Constants.ServiceType.CHAT:
                // 聊天：普通用户每日 5000 token，上层每次固定消耗 1 token => 最多 5000 次；VIP 不限
                if (user.getUserType() == Constants.UserType.VIP) {
                    incrementLegacyTotalCount(userId, 1);
                    return true;
                }
                key = Constants.CacheKey.DAILY_USAGE_CHAT_TOKENS + userId + ":" + today;
                limit = Constants.DefaultValue.NORMAL_CHAT_TOKENS_DAILY;
                inc = Constants.TokenCost.CHAT; // 当前为 1
                break;
            case Constants.ServiceType.IMAGE:
                key = Constants.CacheKey.DAILY_USAGE_IMAGE + userId + ":" + today;
                if (user.getUserType() == Constants.UserType.VIP) {
                    limit = Constants.DefaultValue.VIP_IMAGE_DAILY;
                } else { // NORMAL
                    limit = Constants.DefaultValue.NORMAL_IMAGE_DAILY;
                }
                break;
            case Constants.ServiceType.VIDEO:
                key = Constants.CacheKey.DAILY_USAGE_VIDEO + userId + ":" + today;
                if (user.getUserType() == Constants.UserType.VIP) {
                    limit = Constants.DefaultValue.VIP_VIDEO_DAILY;
                } else {
                    limit = Constants.DefaultValue.NORMAL_VIDEO_DAILY;
                }
                break;
            case Constants.ServiceType.PPT:
                key = Constants.CacheKey.DAILY_USAGE_PPT + userId + ":" + today;
                if (user.getUserType() == Constants.UserType.VIP) {
                    limit = Constants.DefaultValue.VIP_PPT_DAILY;
                } else {
                    limit = Constants.DefaultValue.NORMAL_PPT_DAILY;
                }
                break;
            default:
                // 未知服务按一次计入总次数（保守）
                incrementLegacyTotalCount(userId, 1);
                return true;
        }

        if (limit != Constants.DefaultValue.UNLIMITED) {
            Object val = redisTemplate.opsForValue().get(key);
            int current = val instanceof Integer ? (Integer) val : (val == null ? 0 : Integer.parseInt(String.valueOf(val)));
            if (current + inc > limit) {
                throw new BusinessException(ResultCode.DAILY_LIMIT_EXCEEDED);
            }
            redisTemplate.opsForValue().increment(key, inc);
            redisTemplate.expire(key, secondsUntilEndOfDay(), TimeUnit.SECONDS);
        }

        // 兼容：总次数 +1（便于原有前端展示）
        incrementLegacyTotalCount(userId, 1);
        return true;
    }

    private void incrementLegacyTotalCount(Long userId, int delta) {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String usageCacheKey = Constants.CacheKey.DAILY_USAGE + userId + ":" + today;
        redisTemplate.opsForValue().increment(usageCacheKey, delta);
        redisTemplate.expire(usageCacheKey, secondsUntilEndOfDay(), TimeUnit.SECONDS);
    }

    private long secondsUntilEndOfDay() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime end = now.toLocalDate().plusDays(1).atStartOfDay();
        return java.time.Duration.between(now, end).getSeconds();
    }
    
    @Override
    @Transactional
    public void upgradeUserType(Long userId, Integer userType, Integer days) {
        int result = userMapper.updateUserType(userId, userType);
        if (result <= 0) {
            throw new BusinessException("用户类型升级失败");
        }
        
        // 清除缓存
        String userCacheKey = Constants.CacheKey.USER_INFO + userId;
        redisTemplate.delete(userCacheKey);
        
        log.info("用户类型升级成功：用户ID={}, 类型={}, 天数={}", userId, userType, days);
    }
}