package com.kmu.edu.back_service.constant;

/**
 * 常量定义
 */
public class Constants {
    
    /**
     * 用户类型
     */
    public static class UserType {
        public static final int NORMAL = 0;     // 普通用户
        public static final int VIP = 1;        // VIP用户
        public static final int SUPER_VIP = 2;  // 超级VIP用户
    }

    /**
     * 角色
     */
    public static class Role {
        public static final String USER = "USER";
        public static final String ADMIN = "ADMIN";
    }
    
    /**
     * 用户状态
     */
    public static class UserStatus {
        public static final int DISABLED = 0;   // 禁用
        public static final int ENABLED = 1;    // 启用
    }
    
    /**
     * 性别
     */
    public static class Gender {
        public static final int UNKNOWN = 0;    // 未知
        public static final int MALE = 1;       // 男
        public static final int FEMALE = 2;     // 女
    }
    
    /**
     * 服务类型
     */
    public static class ServiceType {
        public static final int CHAT = 1;       // 聊天
        public static final int IMAGE = 2;      // 图片生成
        public static final int VIDEO = 3;      // 视频生成
        public static final int PPT = 4;        // PPT生成
    }
    
    /**
     * 消息类型
     */
    public static class MessageType {
        public static final int USER = 1;       // 用户消息
        public static final int AI = 2;         // AI回复
    }
    
    /**
     * 生成状态
     */
    public static class GenerationStatus {
        public static final int GENERATING = 0; // 生成中
        public static final int SUCCESS = 1;    // 成功
        public static final int FAILED = 2;     // 失败
    }
    
    /**
     * 视频来源类型
     */
    public static class VideoSourceType {
        public static final int TEXT = 1;       // 文本生成
        public static final int IMAGE = 2;      // 图片转视频
    }
    
    /**
     * 产品类型
     */
    public static class ProductType {
        public static final int TOKENS = 1;     // Token
        public static final int VIP = 2;        // VIP会员
        public static final int SUPER_VIP = 3;  // 超级VIP
    }
    
    /**
     * 支付方式
     */
    public static class PaymentMethod {
        public static final int WECHAT = 1;     // 微信支付
        public static final int ALIPAY = 2;     // 支付宝
        public static final int BALANCE = 3;    // 余额支付
    }
    
    /**
     * 支付状态
     */
    public static class PaymentStatus {
        public static final int PENDING = 0;    // 待支付
        public static final int PAID = 1;       // 已支付
        public static final int CANCELLED = 2;  // 已取消
        public static final int REFUNDED = 3;   // 已退款
    }
    
    /**
     * 缓存Key前缀
     */
    public static class CacheKey {
        public static final String USER_TOKEN = "user:token:";
        public static final String USER_INFO = "user:info:";
        // 旧：usage:daily:{userId}:{yyyyMMdd}（总次数）
        // 新增按服务细分的每日计数与聊天token计数
        public static final String DAILY_USAGE = "usage:daily:"; // 兼容：总次数
        public static final String DAILY_USAGE_CHAT_TOKENS = "usage:daily:chatTokens:"; // + userId + : + day
        public static final String DAILY_USAGE_IMAGE = "usage:daily:image:"; // 次数
        public static final String DAILY_USAGE_VIDEO = "usage:daily:video:"; // 次数
        public static final String DAILY_USAGE_PPT = "usage:daily:ppt:";   // 次数
        public static final String CHAT_SESSION = "chat:session:";
        public static final String GENERATION_PROGRESS = "generation:progress:";
        // 网站访问统计
        public static final String ANALYTICS_PV_DAILY = "analytics:pv:"; // + yyyyMMdd
        public static final String ANALYTICS_UV_DAILY = "analytics:uv:"; // + yyyyMMdd (HyperLogLog)
    }
    
    /**
     * 默认值
     */
    public static class DefaultValue {
        public static final String DEFAULT_AVATAR = "http://localhost:8080/files/avatar/default.png";
        public static final int DEFAULT_TOKENS = 1000;
        // 新配额规则（按日）
        // 普通用户：聊天5000（按固定每次1 token计数，相当于最多5000次），图片/视频/PPT各5次
        public static final int NORMAL_CHAT_TOKENS_DAILY = 5000;
        public static final int NORMAL_IMAGE_DAILY = 5;
        public static final int NORMAL_VIDEO_DAILY = 5;
        public static final int NORMAL_PPT_DAILY = 5;
        // VIP：聊天不限量，图片/视频/PPT各50次
        public static final int VIP_IMAGE_DAILY = 50;
        public static final int VIP_VIDEO_DAILY = 50;
        public static final int VIP_PPT_DAILY = 50;
        // 超级VIP：全部不限量（使用-1代表不限制）
        public static final int UNLIMITED = -1;
    }
    
    /**
     * Token消耗
     */
    public static class TokenCost {
        public static final int CHAT = 1;       // 聊天每次消耗
        public static final int IMAGE = 10;     // 图片生成每次消耗
        public static final int VIDEO = 50;     // 视频生成每次消耗
        public static final int PPT = 20;       // PPT生成每次消耗
    }
}