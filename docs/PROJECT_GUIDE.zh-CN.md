# AIOC 项目文档

> 本文档面向希望快速了解、部署与二次开发 AIOC 平台的研发、运维与产品同学。内容涵盖系统架构、目录结构、部署步骤、核心业务流程、数据模型以及常见问题。

## 目录
- [1. 项目概览](#1-项目概览)
- [2. 架构综述](#2-架构综述)
- [3. 代码仓库与目录结构](#3-代码仓库与目录结构)
- [4. 环境准备](#4-环境准备)
- [5. 快速启动](#5-快速启动)
- [6. 配置说明](#6-配置说明)
- [7. 核心模块与业务流程](#7-核心模块与业务流程)
- [8. 数据存储设计](#8-数据存储设计)
- [9. 前端应用说明](#9-前端应用说明)
- [10. 构建与部署](#10-构建与部署)
- [11. 日志、监控与告警建议](#11-日志监控与告警建议)
- [12. 测试与质量保障](#12-测试与质量保障)
- [13. 常见问题 FAQ](#13-常见问题-faq)
- [14. 版本规划与建议](#14-版本规划与建议)

## 1. 项目概览

### 1.1 项目定位
AIOC（AI Omnichannel Companion）是一个聚合多模态 AI 能力（对话、图像、视频、PPT）并内置会员计费体系的全栈平台，包含 Spring Boot 3 后端与 Vue 3 前端。项目目标是提供一站式的 AI 创作与内容辅助服务，同时兼顾可运营性（订单、会员、配额管理）。

### 1.2 核心功能
- **AI 对话**：基于 SiliconFlow（如 Qwen 系列）的大模型对话，支持多会话、上下文记忆、流式推送（SSE）、深度思考模式以及图片消息。
- **图像生成**：调用 SiliconFlow 图像模型生成高质量图片，支持尺寸、数量等参数配置，并记录生成历史。
- **视频生成**：支持文本转视频（T2V）与图像转视频（I2V），包含任务轮询、进度更新与异常处理。
- **AI PPT**：根据提示词生成 PPT，并支持模板管理（MinIO 存储）以及导出。
- **用户体系**：登录注册、角色（USER/ADMIN）、会员等级、余额、配额（token）统计。
- **订单与计费**：支持虚拟商品（token、VIP、超级 VIP）订单创建、支付状态管理及配额结算。
- **统一鉴权**：通过 Sa-Token + Redis 管理登录态，前端自动附带 Bearer Token。
- **运营支撑**：后台管理页面、系统配置（如 token 消耗、套餐价格）等。

### 1.3 技术栈概览
- **前端**：Vue 3、Vite 7、Element Plus、Pinia、Vue Router、Axios、ECharts。
- **后端**：Spring Boot 3.4.10、Spring WebMVC、Spring WebFlux（WebClient）、MyBatis + PageHelper、Redis、Sa-Token、LangChain4j、Knife4j、MinIO、Apache POI、Lombok、Hutool。
- **数据与基础设施**：MySQL 8、Redis 6、MinIO（可选）、SiliconFlow API。

### 1.4 系统边界
- **非目标**：本版本未内置第三方支付对接（支付状态模拟/留扩展点），未提供自动化部署脚本。
- **外部依赖**：MySQL、Redis、分页插件 PageHelper、SiliconFlow API、MinIO（若需对象存储）。

## 2. 架构综述

### 2.1 总体架构
```
┌──────────────────────────────────────┐
│               客户端层              │
│  Vue 3 单页应用 (Vite, Pinia, SSE)  │
└───────────────▲─────────────────────┘
                │ HTTPS/REST + SSE
┌───────────────┴─────────────────────┐
│               服务层                │
│ Spring Boot API (WebMVC/WebFlux)    │
│  ├─ Sa-Token 鉴权                  │
│  ├─ LangChain4j + SiliconFlow 接入 │
│  ├─ MinIO 客户端                   │
│  └─ 业务服务/事务                  │
└───────────────▲─────────────────────┘
                │ JDBC/Redis SDK
┌───────────────┴─────────────────────┐
│           数据与基础设施层          │
│ MySQL │ Redis │ MinIO │ SiliconFlow │
└──────────────────────────────────────┘
```

### 2.2 模块划分
- **common**：统一返回体、枚举、常量。
- **config**：应用配置（CORS、Redis、Knife4j、Sa-Token、WebClient、MinIO、异步线程池等）。
- **dto**：请求/响应对象，含聊天、图像、视频、订单、用户等多模块定义。
- **entity + mapper**：数据库实体与 MyBatis 映射。
- **service**：领域服务，包括 Chat、Image、Video、PPT、Order、User、MinioStorage、SiliconAI 等，并提供 `impl` 实现。
- **utils**：工具类（如 Markdown 渲染、文件处理、Snowflake ID 等）。
- **front/src**：分模块接口封装（`api/modules`）、页面、状态管理、主题样式、工具。

### 2.3 第三方依赖说明
| 依赖 | 场景 | 备注 |
| --- | --- | --- |
| LangChain4j | 大模型接入封装 | 支持会话记忆、流式输出等 |
| SiliconFlow | 模型推理 | Qwen3、Wan2.2 等模型，可替换为兼容 OpenAI API 的模型 |
| MinIO | 对象存储 | 可替换为 S3 兼容服务；前端提供代理避免跨域 |
| Sa-Token | 鉴权 | Token + 会话管理；Redis 存储状态 |
| Knife4j/SpringDoc | API 文档 | 访问 `http://host:8080/swagger-ui.html` |

## 3. 代码仓库与目录结构

```
AIOC/
├─ back_service/                # Spring Boot 后端
│  ├─ pom.xml
│  ├─ src/main/java/com/kmu/edu/back_service
│  │  ├─ common/
│  │  ├─ config/
│  │  ├─ dto/
│  │  ├─ entity/
│  │  ├─ mapper/
│  │  ├─ service/ (及 impl、storage)
│  │  └─ utils/
│  ├─ src/main/resources
│  │  ├─ application.yml
│  │  └─ sql/init.sql
│  └─ src/test/java/.../BackServiceApplicationTests.java
├─ front/                       # Vue 3 前端
│  ├─ package.json
│  ├─ vite.config.js
│  ├─ public/
│  └─ src/
│     ├─ api/
│     ├─ pages/
│     ├─ layouts/
│     ├─ router/
│     ├─ stores/
│     ├─ styles/
│     └─ utils/
└─ docs/
   └─ PROJECT_GUIDE.zh-CN.md     # 当前文档
```

## 4. 环境准备

| 组件 | 版本建议 | 说明 |
| --- | --- | --- |
| 操作系统 | Windows 10+/Linux/macOS | 部署环境不限 |
| JDK | 20 或 21 | 与 `pom.xml` 中 `java.version` 保持一致 |
| Maven | ≥ 3.8 | 构建后端 |
| Node.js | 20.x 或 22.x | 构建前端（`package.json` `engines` 限定） |
| npm | 最新稳定 | 可替换为 pnpm/yarn（需自行调整脚本） |
| MySQL | ≥ 8.0 | 默认库名 `ai_platform` |
| Redis | ≥ 6.0 | 默认无密码，0 号库 |
| MinIO | 可选 | 若需启用对象存储/文件下载 |
| SiliconFlow API Key | 必需 | 用于调用 AI 模型，建议以环境变量注入 |

> **安全提示：** 请不要将真实的 API Key、数据库密码写入 `application.yml` 并提交到版本库。推荐使用环境变量或 `application-local.yml` 覆盖。

## 5. 快速启动

### 5.1 初始化数据
```bat
REM 1. 创建数据库
mysql -h127.0.0.1 -P3306 -uroot -p -e "CREATE DATABASE ai_platform DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

REM 2. 导入初始化脚本（根据实际路径调整）
mysql -h127.0.0.1 -P3306 -uroot -p ai_platform < back_service\src\main\resources\sql\init.sql
```
脚本会创建核心表（用户、会话、生成记录、订单、配置、模板）并插入默认管理员账号（密码为 BCrypt Hash，首次登录后请重置）。

### 5.2 配置敏感信息
- 在 `back_service/src/main/resources/` 新增 `application-local.yml` 或通过环境变量覆盖以下关键项：
  - `spring.datasource.url/username/password`
  - `spring.data.redis.*`
  - `ai.silicon.api-key`（强烈建议环境变量 `AI_SILICON_API_KEY`）
  - `storage.minio.*`（若启用对象存储）
  - `file.upload.path`（上传目录，Windows 建议 `D:/uploads/` 或 `C:\\uploads\\`）
- 本地运行时可使用 Spring Profile `--spring.profiles.active=local`。

### 5.3 启动后端
```bat
cd back_service
mvn clean package -DskipTests
java -jar target\back_service-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```
或直接运行：
```bat
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### 5.4 启动前端
```bat
cd front
npm install
npm run dev
```
默认访问地址 `http://localhost:5173/`，通过代理将 `/api` 请求转发到 `http://localhost:8080`。

## 6. 配置说明

### 6.1 后端 `application.yml`
| 配置段 | 说明 | 备注 |
| --- | --- | --- |
| `server.port` | HTTP 端口，默认 8080 | 可视需求修改 |
| `spring.datasource.*` | MySQL JDBC 连接 | 生产环境需强制覆盖账号密码 |
| `spring.data.redis.*` | Redis 地址与连接池 | 若有密码需配置 `password` |
| `spring.servlet.multipart` | 上传大小限制 | 默认 100MB |
| `mybatis.*` | Mapper 扫描、驼峰映射、日志输出 | 开发环境默认开启 STDOUT 日志 |
| `sa-token.*` | Token 名称、前缀、有效期 | Token 放在 Header `Authorization: Bearer xx` |
| `ai.silicon.*` | 模型、超时、令牌上限 | 请改为自己的 API Key，避免泄漏 |
| `file.upload.*` | 上传文件路径与访问 URL 前缀 | 需与 Nginx/静态服务器保持一致 |
| `storage.minio.*` | MinIO Endpoint、Bucket 等 | 可选；与 `MinioConfig`/`MinioStorageService` 对应 |
| `springdoc.*` & `knife4j.*` | 在线 API 文档配置 | 访问 `/swagger-ui.html` 或 `/doc.html` |
| `logging.*` | 日志级别、格式 | 生产环境建议调整为 INFO |
| `user.*` | 用户默认头像、配额限制 | 用于前端展示与配额计算 |

> 提示：代码仓库中的示例 `api-key` 为占位符，请立即替换并避免提交真实密钥。

### 6.2 前端环境变量
- 开发环境：在 `front/.env.development` 中设置 `VITE_API_BASE=http://localhost:8080`。
- 生产环境：在 `front/.env.production` 中设置接口域名，如 `https://api.example.com`。
- 其余自定义变量可通过 `import.meta.env.VITE_*` 使用。

## 7. 核心模块与业务流程

### 7.1 鉴权与用户体系
- **数据表**：`sys_user`
- **主要组件**：
  - `SaTokenConfig`、`SaToken` 拦截器负责鉴权。
  - `SysUserService`/`SysUserServiceImpl` 提供用户注册、登录、信息查询。
  - Token 生成后存储于 Redis，同时返回给前端。
  - 前端 `stores/user.js` 管理 Token、用户信息与路由守卫。
- **流程**：
  1. 用户注册（校验唯一性、写库），注册后自动登录。
  2. 用户登录 -> 返回 Token -> 前端保存到 `localStorage`。
  3. 前端路由守卫校验 Token，必要时拉取用户信息。
  4. 注销时清理 Redis 会话及本地存储。

### 7.2 聊天服务
- **数据表**：`chat_session`、`chat_message`、`user_usage_record`
- **主要类**：
  - `ChatService`/`ChatServiceImpl`：会话创建、消息记录、SSE 流式输出。
  - `SiliconAIService`：封装 LangChain4j，处理会话上下文、深度思考模式、推理文本。
  - `PptTemplateDataInitializer`：初始化模板（用于 PPT，但与聊天配额共享同一 usage 逻辑）。
- **流程**：
  1. 前端进入聊天页请求会话列表。
  2. 用户发送消息，前端先插入本地消息，再开启 SSE 通道 `/api/chat/stream`。
  3. 后端使用 WebClient 调用 SiliconFlow，流式向前端发送 `delta/reasoning/done` 事件。
  4. 消息落库，更新会话摘要与 token 使用记录。
  5. 支持图片消息追加：`ChatService.appendImageMessage`。

### 7.3 图像生成
- **数据表**：`image_generation`
- **主要类**：`ImageGenerationService`/`ImageGenerationServiceImpl`, `MinioStorageService`
- **流程**：
  1. 前端提交提示词、尺寸、数量。
  2. 后端调用 SiliconFlow 图像模型，生成结果写入 MinIO 并记录数据库。
  3. 前端轮询或直接获取生成记录列表。
  4. 失败时写入错误信息，供用户查看。

### 7.4 视频生成
- **数据表**：`video_generation`
- **主要类**：`VideoGenerationService`/`VideoGenerationServiceImpl`
- **流程**：
  1. 任务提交后立即返回任务编号。
  2. 后端异步轮询 SiliconFlow 任务状态，更新进度和链接。
  3. 前端通过接口查询任务详情，展示状态。

### 7.5 AI PPT
- **数据表**：`ppt_generation`、`ppt_template`
- **主要类**：`PptGenerationService`/`PptGenerationServiceImpl`, `MinioStorageService`
- **功能**：根据主题、模板生成 PPTX 文件（Apache POI），可同步生成 PDF/缩略图。
- **流程**：收集提示词 -> 调用 AI 生成内容 -> 组合模板 -> 上传至 MinIO -> 返回下载地址。

### 7.6 订单与会员
- **数据表**：`orders`, `sys_config`
- **主要类**：`OrderService`/`OrderServiceImpl`
- **说明**：
  - 订单类型包括 token、VIP、超级 VIP；消费规则写在 `sys_config`。
  - 订单生成后可扩展接入第三方支付，目前以内置状态流转为主。
  - 支持余额、token 统计、过期时间管理。

### 7.7 配额与使用记录
- **数据表**：`user_usage_record`
- **逻辑**：每次调用 AI 能力会记录消耗的 tokens、费用与请求/响应内容，便于审计与限流。

## 8. 数据存储设计

> 以下表结构与初始脚本 `back_service/src/main/resources/sql/init.sql` 完全一致，如需变更请同步修改脚本并更新文档。

### 8.1 `sys_user` — 用户表

| 列名 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| `id` | BIGINT | PK, AUTO_INCREMENT | 用户 ID |
| `username` | VARCHAR(50) | NOT NULL, UNIQUE | 用户名 |
| `password` | VARCHAR(100) | NOT NULL | 加密后的密码 |
| `email` | VARCHAR(100) | UNIQUE | 邮箱 |
| `phone` | VARCHAR(20) | UNIQUE | 手机号 |
| `nickname` | VARCHAR(50) |  | 昵称 |
| `avatar` | VARCHAR(500) |  | 头像 URL |
| `gender` | TINYINT | DEFAULT 0 | 性别：0 未知，1 男，2 女 |
| `birthday` | DATE |  | 生日 |
| `user_type` | TINYINT | DEFAULT 0 | 用户类型：0 普通，1 VIP，2 超级 VIP |
| `role` | VARCHAR(20) | DEFAULT 'USER' | 角色：USER/ADMIN |
| `status` | TINYINT | DEFAULT 1 | 状态：0 禁用，1 启用 |
| `balance` | DECIMAL(10,2) | DEFAULT 0.00 | 余额 |
| `total_tokens` | INT | DEFAULT 0 | 总 token 数 |
| `used_tokens` | INT | DEFAULT 0 | 已使用 token 数 |
| `create_time` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| `update_time` | DATETIME | DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | 更新时间 |

### 8.2 `user_usage_record` — 用户使用记录表

| 列名 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| `id` | BIGINT | PK, AUTO_INCREMENT | 记录 ID |
| `user_id` | BIGINT | NOT NULL, INDEX | 用户 ID |
| `service_type` | TINYINT | NOT NULL, INDEX | 服务类型：1 聊天，2 图片，3 视频，4 PPT |
| `tokens_used` | INT | DEFAULT 0 | 消耗的 token 数 |
| `cost` | DECIMAL(10,2) | DEFAULT 0.00 | 费用 |
| `request_content` | TEXT |  | 请求内容 |
| `response_content` | TEXT |  | 响应内容 |
| `status` | TINYINT | DEFAULT 1 | 状态：0 失败，1 成功 |
| `create_time` | DATETIME | DEFAULT CURRENT_TIMESTAMP, INDEX | 创建时间 |

### 8.3 `chat_session` — 聊天会话表

| 列名 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| `id` | BIGINT | PK, AUTO_INCREMENT | 会话 ID |
| `user_id` | BIGINT | NOT NULL, INDEX | 用户 ID |
| `session_id` | VARCHAR(64) | NOT NULL, UNIQUE, INDEX | 会话唯一标识 |
| `title` | VARCHAR(200) |  | 会话标题 |
| `context_summary` | TEXT |  | 上下文摘要 |
| `message_count` | INT | DEFAULT 0 | 消息数量 |
| `last_message_time` | DATETIME |  | 最后消息时间 |
| `status` | TINYINT | DEFAULT 1 | 状态：0 已删除，1 正常 |
| `create_time` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| `update_time` | DATETIME | DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | 更新时间 |

### 8.4 `chat_message` — 聊天消息表

| 列名 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| `id` | BIGINT | PK, AUTO_INCREMENT | 消息 ID |
| `session_id` | BIGINT | NOT NULL, INDEX | 所属会话 ID |
| `user_id` | BIGINT | NOT NULL, INDEX | 用户 ID |
| `message_type` | TINYINT | NOT NULL | 消息类型：1 用户，2 AI |
| `content` | TEXT | NOT NULL | 消息内容 |
| `token_count` | INT | DEFAULT 0 | token 数量 |
| `create_time` | DATETIME | DEFAULT CURRENT_TIMESTAMP, INDEX | 创建时间 |

### 8.5 `image_generation` — 图片生成记录表

| 列名 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| `id` | BIGINT | PK, AUTO_INCREMENT | 生成 ID |
| `user_id` | BIGINT | NOT NULL, INDEX | 用户 ID |
| `prompt` | TEXT | NOT NULL | 提示词 |
| `style` | VARCHAR(50) |  | 图片风格 |
| `size` | VARCHAR(20) |  | 图片尺寸 |
| `image_url` | VARCHAR(500) |  | 图片 URL |
| `thumbnail_url` | VARCHAR(500) |  | 缩略图 URL |
| `generation_time` | INT |  | 生成耗时（秒） |
| `status` | TINYINT | DEFAULT 0, INDEX | 状态：0 生成中，1 成功，2 失败 |
| `error_message` | TEXT |  | 错误信息 |
| `create_time` | DATETIME | DEFAULT CURRENT_TIMESTAMP, INDEX | 创建时间 |

### 8.6 `video_generation` — 视频生成记录表

| 列名 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| `id` | BIGINT | PK, AUTO_INCREMENT | 生成 ID |
| `user_id` | BIGINT | NOT NULL, INDEX | 用户 ID |
| `prompt` | TEXT |  | 提示词 |
| `source_type` | TINYINT | NOT NULL | 来源类型：1 文本生成，2 图片转视频 |
| `source_image_url` | VARCHAR(500) |  | 源图片 URL |
| `duration` | INT |  | 视频时长（秒） |
| `style` | VARCHAR(50) |  | 视频风格 |
| `video_url` | VARCHAR(500) |  | 视频 URL |
| `thumbnail_url` | VARCHAR(500) |  | 缩略图 URL |
| `generation_time` | INT |  | 生成耗时（秒） |
| `progress` | INT | DEFAULT 0 | 进度（0-100） |
| `status` | TINYINT | DEFAULT 0, INDEX | 状态：0 生成中，1 成功，2 失败 |
| `error_message` | TEXT |  | 错误信息 |
| `create_time` | DATETIME | DEFAULT CURRENT_TIMESTAMP, INDEX | 创建时间 |

### 8.7 `ppt_generation` — PPT 生成记录表

| 列名 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| `id` | BIGINT | PK, AUTO_INCREMENT | 生成 ID |
| `user_id` | BIGINT | NOT NULL, INDEX | 用户 ID |
| `title` | VARCHAR(200) | NOT NULL | PPT 标题 |
| `prompt` | TEXT | NOT NULL | 生成提示词 |
| `template_id` | VARCHAR(50) |  | 模板 ID |
| `slide_count` | INT |  | 幻灯片数量 |
| `ppt_url` | VARCHAR(500) |  | PPT 文件 URL |
| `pdf_url` | VARCHAR(500) |  | PDF 文件 URL |
| `thumbnail_url` | VARCHAR(500) |  | 缩略图 URL |
| `generation_time` | INT |  | 生成耗时（秒） |
| `status` | TINYINT | DEFAULT 0, INDEX | 状态：0 生成中，1 成功，2 失败 |
| `error_message` | TEXT |  | 错误信息 |
| `create_time` | DATETIME | DEFAULT CURRENT_TIMESTAMP, INDEX | 创建时间 |

### 8.8 `orders` — 订单表

| 列名 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| `id` | BIGINT | PK, AUTO_INCREMENT | 订单 ID |
| `order_no` | VARCHAR(64) | NOT NULL, UNIQUE, INDEX | 订单号 |
| `user_id` | BIGINT | NOT NULL, INDEX | 用户 ID |
| `product_type` | TINYINT | NOT NULL | 产品类型：1 tokens，2 VIP，3 超级 VIP |
| `product_name` | VARCHAR(100) | NOT NULL | 产品名称 |
| `amount` | DECIMAL(10,2) | NOT NULL | 订单金额 |
| `payment_method` | TINYINT |  | 支付方式：1 微信，2 支付宝，3 余额 |
| `payment_status` | TINYINT | DEFAULT 0, INDEX | 支付状态：0 待支付，1 已支付，2 已取消，3 已退款 |
| `payment_time` | DATETIME |  | 支付时间 |
| `transaction_id` | VARCHAR(100) |  | 第三方交易 ID |
| `tokens_amount` | INT |  | Token 数量 |
| `vip_days` | INT |  | VIP 天数 |
| `expire_time` | DATETIME |  | 过期时间 |
| `remark` | TEXT |  | 备注 |
| `create_time` | DATETIME | DEFAULT CURRENT_TIMESTAMP, INDEX | 创建时间 |
| `update_time` | DATETIME | DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | 更新时间 |

### 8.9 `sys_config` — 系统配置表

| 列名 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| `id` | BIGINT | PK, AUTO_INCREMENT | 配置 ID |
| `config_key` | VARCHAR(100) | NOT NULL, UNIQUE | 配置键 |
| `config_value` | TEXT |  | 配置值 |
| `config_desc` | VARCHAR(200) |  | 配置描述 |
| `status` | TINYINT | DEFAULT 1 | 状态：0 禁用，1 启用 |
| `create_time` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| `update_time` | DATETIME | DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | 更新时间 |

### 8.10 `ppt_template` — PPT 模板表

| 列名 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| `id` | BIGINT | PK, AUTO_INCREMENT | 模板 ID |
| `template_name` | VARCHAR(100) | NOT NULL | 模板名称 |
| `template_desc` | TEXT |  | 模板描述 |
| `template_url` | VARCHAR(500) | NOT NULL | 模板文件 URL |
| `thumbnail_url` | VARCHAR(500) |  | 缩略图 URL |
| `category` | VARCHAR(50) |  | 分类 |
| `sort_order` | INT | DEFAULT 0 | 排序 |
| `status` | TINYINT | DEFAULT 1 | 状态：0 禁用，1 启用 |
| `create_time` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| `update_time` | DATETIME | DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | 更新时间 |

初始化脚本还包含默认 `sys_config` 配置项以及管理员账号，部署时请根据需求调整。

## 9. 前端应用说明

- **路由结构**（`front/src/router/index.js`）：
  - 公共页：欢迎页、登录、注册。
  - 受保护页：聊天、图像、视频、PPT、订单、个人中心（需登录）。
  - 管理页：`/admin`，需管理员角色。
- **状态管理**（`front/src/stores/user.js`）：
  - 保存 Token、用户信息、余额、会员类型。
  - 提供 `login`、`register`、`fetchInfo`、`logout` 等动作。
- **API 模块**（`front/src/api/modules`）：
  - `chat`, `image`, `video`, `ppt`, `order`, `user`, `auth`, `file`, `admin`, `code`, `test` 等模块化封装。
  - `http.js` 统一设置 Axios 拦截器，自动附加 Token、处理响应格式。
- **UI 交互**：
  - Element Plus 组件库，配合自定义主题（`front/src/styles`）。
  - 支持暗色模式、移动端视窗适配（如聊天页的 `--app-dvh`）。
  - SSE 流式对话实时渲染 Markdown（`front/src/utils/markdown.js`）。

## 10. 构建与部署

### 10.1 后端部署建议
1. 编译打包：`mvn clean package -DskipTests`
2. 将 `back_service-0.0.1-SNAPSHOT.jar` 上传至服务器。
3. 配置环境变量或外部化配置文件（`application-prod.yml`）。
4. 使用进程守护（systemd、pm2、supervisor）运行：
   ```bash
   java -Xms512m -Xmx1024m -jar back_service-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
   ```
5. 配置反向代理（如 Nginx）转发 `/api` 请求，并处理静态文件目录。

### 10.2 前端部署建议
1. 构建产物：`npm run build`（输出至 `dist/`）。
2. 上传 `dist/` 到静态服务器（Nginx/OSS/CDN）。
3. 配置 Nginx：
   ```nginx
   server {
       listen 80;
       server_name ai.example.com;

       root /var/www/aioc/dist;
       index index.html;

       location / {
           try_files $uri $uri/ /index.html;
       }

       location /api/ {
           proxy_pass http://127.0.0.1:8080/;
           proxy_set_header Host $host;
           proxy_set_header X-Real-IP $remote_addr;
           proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
       }
   }
   ```
4. 若启用 SSL，记得同步更新前端 `VITE_API_BASE`。

### 10.3 MinIO/对象存储
- 若使用 MinIO：
  - 创建 Bucket（如 `test`），开启相应读写策略。
  - 在后端配置 `storage.minio.*`，前端通过 `/api/proxy` 中转资源以规避跨域/鉴权问题。
- 若替换为其他 S3 兼容服务，确认 SDK 兼容性并调整配置。

## 11. 日志监控与告警建议
- **应用日志**：默认输出到控制台，生产环境可通过 Logback 配置文件输出到文件或集中日志系统。
- **重要事件**：
  - 登录失败、订单状态变更、AI 调用异常、超过配额的请求。
- **监控建议**：
  - 接入 Prometheus + Grafana 监控 JVM 指标（可启用 `spring-boot-starter-actuator`）。
  - Redis/MySQL 监控连接数与慢查询。
  - SiliconFlow API 调用失败率。
- **告警**：
  - Token 使用异常（突然激增）。
  - 队列积压（视频生成长时间未完成）。

## 12. 测试与质量保障
- **后端单元测试**：
  - 模板测试类 `BackServiceApplicationTests` 可扩展。
  - 推荐使用 `@SpringBootTest` + MockMVC + Testcontainers（MySQL/Redis）。
- **接口联调**：
  - 访问 `http://localhost:8080/swagger-ui.html` 或 Knife4j 界面 `/doc.html`。
  - 建议使用 Postman / Hoppscotch 保存接口集合。
- **前端测试**：
  - 当前未提供自动化测试脚本，可引入 Vitest + Vue Test Utils。
- **流程验证**：
  - 登录/注册 -> 聊天 -> 图像/视频/PPT 生成 -> 订单创建 -> 配额扣减。

## 13. 常见问题 FAQ
- **API 返回 401**：确认前端是否携带 Token；Token 过期后需重新登录。
- **CORS 错误**：确保前后端端口一致或开启代理；后端 `CorsConfig` 已允许常见方法。
- **上传路径不可写**：检查 `file.upload.path` 目录权限，Linux 需 `chmod`。
- **视频生成长时间无结果**：调整 `ai.silicon.video-timeout-seconds` 与 `video-poll-interval-millis`；检查外部服务状态。
- **MinIO 访问异常**：确认 Bucket、AccessKey/Secret、Endpoint；可先在本地关闭 MinIO 集成。
- **数据库密码泄漏风险**：通过环境变量或独立配置文件覆盖敏感信息；勿提交到仓库。
- **默认管理员密码未知**：`init.sql` 中为 BCrypt Hash，无法直接还原；请通过 SQL 手工修改为新 Hash（可使用 Spring Security `BCryptPasswordEncoder` 生成）。

## 14. 版本规划与建议
- **短期**：
  - 优化错误处理与前端提示。
  - 增加接口限流与 IP 黑名单策略。
  - 补充单元测试、集成测试。
- **中期**：
  - 引入消息队列处理视频/PPT 长耗时任务。
  - 支持第三方支付（微信/支付宝）回调。
  - 多租户/组织管理。
- **长期**：
  - 微服务拆分（审慎评估复杂度）。
  - 增加更多模型接入（如语音、3D）。
  - 完善监控与 APM。

---
如需更多帮助或协作开发建议，欢迎在仓库提 Issue 或联系项目维护者。