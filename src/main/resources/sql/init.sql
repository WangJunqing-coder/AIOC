-- AI综合平台数据库初始化脚本
-- 创建数据库
CREATE DATABASE IF NOT EXISTS ai_platform DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ai_platform;

-- 用户表
CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码(加密)',
    email VARCHAR(100) UNIQUE COMMENT '邮箱',
    phone VARCHAR(20) UNIQUE COMMENT '手机号',
    nickname VARCHAR(50) COMMENT '昵称',
    avatar VARCHAR(500) COMMENT '头像URL',
    gender TINYINT DEFAULT 0 COMMENT '性别:0未知,1男,2女',
    birthday DATE COMMENT '生日',
    user_type TINYINT DEFAULT 0 COMMENT '用户类型:0普通用户,1VIP,2超级VIP',
    role VARCHAR(20) DEFAULT 'USER' COMMENT '角色:USER/ADMIN',
    status TINYINT DEFAULT 1 COMMENT '状态:0禁用,1启用',
    balance DECIMAL(10,2) DEFAULT 0.00 COMMENT '余额',
    total_tokens INT DEFAULT 0 COMMENT '总token数',
    used_tokens INT DEFAULT 0 COMMENT '已使用token数',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT '用户表';

-- 用户使用记录表
CREATE TABLE user_usage_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    service_type TINYINT NOT NULL COMMENT '服务类型:1聊天,2图片,3视频,4PPT',
    tokens_used INT DEFAULT 0 COMMENT '使用的tokens',
    cost DECIMAL(10,2) DEFAULT 0.00 COMMENT '费用',
    request_content TEXT COMMENT '请求内容',
    response_content TEXT COMMENT '响应内容',
    status TINYINT DEFAULT 1 COMMENT '状态:0失败,1成功',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_service_type (service_type),
    INDEX idx_create_time (create_time)
) COMMENT '用户使用记录表';

-- 聊天会话表
CREATE TABLE chat_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '会话ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    session_id VARCHAR(64) NOT NULL UNIQUE COMMENT '会话唯一标识',
    title VARCHAR(200) COMMENT '会话标题',
    context_summary TEXT COMMENT '上下文摘要',
    message_count INT DEFAULT 0 COMMENT '消息数量',
    last_message_time DATETIME COMMENT '最后消息时间',
    status TINYINT DEFAULT 1 COMMENT '状态:0已删除,1正常',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_session_id (session_id)
) COMMENT '聊天会话表';

-- 聊天消息表
CREATE TABLE chat_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '消息ID',
    session_id BIGINT NOT NULL COMMENT '会话ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    message_type TINYINT NOT NULL COMMENT '消息类型:1用户消息,2AI回复',
    content TEXT NOT NULL COMMENT '消息内容',
    token_count INT DEFAULT 0 COMMENT 'token数量',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_session_id (session_id),
    INDEX idx_user_id (user_id),
    INDEX idx_create_time (create_time)
) COMMENT '聊天消息表';

-- 图片生成记录表
CREATE TABLE image_generation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '生成ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    prompt TEXT NOT NULL COMMENT '生成提示词',
    style VARCHAR(50) COMMENT '图片风格',
    size VARCHAR(20) COMMENT '图片尺寸',
    image_url VARCHAR(500) COMMENT '图片URL',
    thumbnail_url VARCHAR(500) COMMENT '缩略图URL',
    generation_time INT COMMENT '生成耗时(秒)',
    status TINYINT DEFAULT 0 COMMENT '状态:0生成中,1成功,2失败',
    error_message TEXT COMMENT '错误信息',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) COMMENT '图片生成记录表';

-- 视频生成记录表
CREATE TABLE video_generation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '生成ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    prompt TEXT COMMENT '生成提示词',
    source_type TINYINT NOT NULL COMMENT '来源类型:1文本生成,2图片转视频',
    source_image_url VARCHAR(500) COMMENT '源图片URL',
    duration INT COMMENT '视频时长(秒)',
    style VARCHAR(50) COMMENT '视频风格',
    video_url VARCHAR(500) COMMENT '视频URL',
    thumbnail_url VARCHAR(500) COMMENT '缩略图URL',
    generation_time INT COMMENT '生成耗时(秒)',
    progress INT DEFAULT 0 COMMENT '生成进度(0-100)',
    status TINYINT DEFAULT 0 COMMENT '状态:0生成中,1成功,2失败',
    error_message TEXT COMMENT '错误信息',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) COMMENT '视频生成记录表';

-- PPT生成记录表
CREATE TABLE ppt_generation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '生成ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    title VARCHAR(200) NOT NULL COMMENT 'PPT标题',
    prompt TEXT NOT NULL COMMENT '生成提示词',
    template_id VARCHAR(50) COMMENT '模板ID',
    slide_count INT COMMENT '幻灯片数量',
    ppt_url VARCHAR(500) COMMENT 'PPT文件URL',
    pdf_url VARCHAR(500) COMMENT 'PDF文件URL',
    thumbnail_url VARCHAR(500) COMMENT '缩略图URL',
    generation_time INT COMMENT '生成耗时(秒)',
    status TINYINT DEFAULT 0 COMMENT '状态:0生成中,1成功,2失败',
    error_message TEXT COMMENT '错误信息',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) COMMENT 'PPT生成记录表';

-- 订单表
CREATE TABLE orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '订单ID',
    order_no VARCHAR(64) NOT NULL UNIQUE COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    product_type TINYINT NOT NULL COMMENT '产品类型:1tokens,2VIP会员,3超级VIP',
    product_name VARCHAR(100) NOT NULL COMMENT '产品名称',
    amount DECIMAL(10,2) NOT NULL COMMENT '订单金额',
    payment_method TINYINT COMMENT '支付方式:1微信,2支付宝,3余额',
    payment_status TINYINT DEFAULT 0 COMMENT '支付状态:0待支付,1已支付,2已取消,3已退款',
    payment_time DATETIME COMMENT '支付时间',
    transaction_id VARCHAR(100) COMMENT '第三方交易ID',
    tokens_amount INT COMMENT 'token数量',
    vip_days INT COMMENT 'VIP天数',
    expire_time DATETIME COMMENT '过期时间',
    remark TEXT COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_order_no (order_no),
    INDEX idx_payment_status (payment_status),
    INDEX idx_create_time (create_time)
) COMMENT '订单表';

-- 系统配置表
CREATE TABLE sys_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '配置ID',
    config_key VARCHAR(100) NOT NULL UNIQUE COMMENT '配置键',
    config_value TEXT COMMENT '配置值',
    config_desc VARCHAR(200) COMMENT '配置描述',
    status TINYINT DEFAULT 1 COMMENT '状态:0禁用,1启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT '系统配置表';

-- PPT模板表
CREATE TABLE ppt_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '模板ID',
    template_name VARCHAR(100) NOT NULL COMMENT '模板名称',
    template_desc TEXT COMMENT '模板描述',
    template_url VARCHAR(500) NOT NULL COMMENT '模板文件URL',
    thumbnail_url VARCHAR(500) COMMENT '缩略图URL',
    category VARCHAR(50) COMMENT '分类',
    sort_order INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '状态:0禁用,1启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT 'PPT模板表';

-- 插入默认数据
INSERT INTO sys_config (config_key, config_value, config_desc) VALUES
('default_tokens', '1000', '新用户默认token数量'),
('chat_token_cost', '1', '聊天每次消耗token数'),
('image_token_cost', '10', '图片生成每次消耗token数'),
('video_token_cost', '50', '视频生成每次消耗token数'),
('ppt_token_cost', '20', 'PPT生成每次消耗token数'),
('vip_price_30', '29.9', 'VIP 30天价格'),
('vip_price_365', '299', 'VIP 365天价格'),
('super_vip_price_30', '59.9', '超级VIP 30天价格'),
('super_vip_price_365', '599', '超级VIP 365天价格');

-- 插入默认管理员用户
INSERT INTO sys_user (username, password, email, nickname, user_type, role, status, balance, total_tokens) VALUES
('admin', '$2a$10$7JB720yubVSOfvVWaSM9wu.kW.TvL1zLKYgJ8tT0m9LU0oF0xX7da', 'admin@ai-platform.com', '管理员', 2, 'ADMIN', 1, 10000.00, 100000);