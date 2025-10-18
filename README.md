# AI综合平台后端服务

## 项目概述

本项目是一个基于Spring Boot 3的AI综合服务平台，集成了AI聊天、图片生成、视频生成和AI PPT制作功能。

## 技术栈

- **后端框架**: Spring Boot 3.4.10
- **数据库**: MySQL 8.0
- **缓存**: Redis
- **ORM框架**: MyBatis
- **认证授权**: Sa-Token
- **AI集成**: 硅基流动(支持Qwen系列模型)
- **API文档**: Swagger/OpenAPI 3
- **工具库**: Hutool, Lombok

## 功能模块

### 1. 用户模块

- 用户注册、登录、密码管理
- 个人信息管理
- 会员体系（普通用户、VIP、超级VIP）
- 余额和Token管理

### 2. AI聊天模块

- 多轮对话功能
- 会话管理
- 对话历史记录
- 上下文保持

### 3. 图片生成模块

- 文本描述生成图片
- 多种风格选择
- 图片尺寸自定义
- 生成历史管理

### 4. 其他模块（待扩展）

- 视频生成模块
- AI PPT生成模块
- 订单支付模块

## 快速开始

### 1. 环境要求

- JDK 21
- Maven 3.6+
- MySQL 8.0+
- Redis 6.0+

### 2. 数据库初始化

```sql
-- 执行SQL脚本
source src/main/resources/sql/init.sql
```

### 3. 配置修改

修改 `src/main/resources/application.properties` 中的配置：

```properties
# 数据库配置
spring.datasource.username=your_username
spring.datasource.password=your_password

# Redis配置
spring.data.redis.password=your_redis_password

# 硅基流动AI服务配置
ai.silicon.api-key=your_silicon_api_key
ai.silicon.base-url=https://api.siliconflow.cn/v1
```

**AI模型说明**：

- **聊天模型**：Qwen/Qwen3-30B-A3B-Instruct-2507
- **图片生成**：Qwen/Qwen-Image
- **视频生成**：Wan-AI/Wan2.2-I2V-A14B

### 4. 启动服务

```bash
# 方式1：使用Maven
mvn spring-boot:run

# 方式2：运行主类
java -jar target/back_service-0.0.1-SNAPSHOT.jar
```

### 5. 访问文档

- **Swagger文档**: http://localhost:8080/swagger-ui.html
- **API文档**: http://localhost:8080/v3/api-docs

## API接口

### 认证接口

- `POST /api/auth/register` - 用户注册
- `POST /api/auth/login` - 用户登录
- `POST /api/auth/logout` - 用户登出

### 用户接口

- `GET /api/user/info` - 获取用户信息
- `PUT /api/user/info` - 更新用户信息

### 聊天接口

- `POST /api/chat/send` - 发送聊天消息
- `GET /api/chat/sessions` - 获取会话列表
- `GET /api/chat/session/{sessionId}/messages` - 获取会话消息

### 图片生成接口

- `POST /api/image/generate` - 生成图片
- `GET /api/image/status/{id}` - 查询生成状态
- `GET /api/image/history` - 获取生成历史

### 视频生成接口

- `POST /api/video/generate` - 生成视频（支持文本生成和图片转视频）

## 项目结构

```
src/
├── main/
│   ├── java/com/kmu/edu/back_service/
│   │   ├── common/          # 通用响应和结果码
│   │   ├── config/          # 配置类
│   │   ├── constant/        # 常量定义
│   │   ├── controller/      # 控制器
│   │   ├── dto/            # 数据传输对象
│   │   ├── entity/         # 实体类
│   │   ├── exception/      # 异常处理
│   │   ├── mapper/         # MyBatis映射接口
│   │   ├── service/        # 服务接口
│   │   ├── utils/          # 工具类
│   │   └── BackServiceApplication.java
│   └── resources/
│       ├── sql/            # 数据库脚本
│       └── application.properties
```

## 开发说明

### 数据库设计

- 用户表：存储用户基本信息和会员信息
- 会话表：存储聊天会话信息
- 消息表：存储聊天消息
- 生成记录表：存储各种AI生成任务记录

### 认证机制

- 使用Sa-Token进行用户认证
- Token存储在Redis中
- 支持无感刷新

### 权限控制

- 基于用户类型的功能权限
- 每日使用次数限制
- Token余额控制

## PPT 模板制作指南

AI PPT 生成已经切换为“克隆模板+占位符替换”模式。要获得正确排版，需要在模板中预先标记所有可替换的文字框。

1. **准备模板副本**
   - 在 PowerPoint 中打开原始模板，另存为新的 `.pptx` 文件供 AI 使用。
   - 仅修改需要动态生成的文本框，保留所有背景图形、装饰元素和动画。
2. **替换文本为占位符**
   - 将每个可替换文本框内容替换成 `{{placeholder_name}}`，占位符名称由字母、数字、下划线或短横构成。
   - 为便于理解与调试，名称应描述用途，例如 `{{cover_title}}`、`{{cover_subtitle}}`、`{{section1_intro}}`。
   - 同一个占位符可以在不同文本框中重复出现，服务会使用同一份内容替换全部位置。
3. **为列表内容保留项目符号**
   - 如果某个文本框需要填充多条要点，请保留其原有项目符号格式或编号格式。
   - 占位符命名建议包含 `list`、`bullets`、`points`、`items` 等关键词（例如 `{{outline_bullets}}`、`{{summary_points}}`），服务会识别为数组型输出并自动写入 3-6 条要点。
   - 若需要控制要点数量，可在模板说明或调用参数中额外注明期望条数。
4. **命名规范提示**
   - 封面通常包含 `cover_title`、`cover_subtitle`、`cover_tagline` 等。
   - 章节页可使用 `section1_title`、`section1_bullets`，按 slide 顺序递增编号。
   - 图片或图表说明文字同样可以使用占位符（例如 `{{chart_note}}`），内容会被替换为单行文本。
5. **保存、上传与测试**
   - 完成标注后保存模板，并在管理后台/数据库中更新模板记录。
   - 生成一次测试 PPT，确认所有占位符都成功替换、没有残留 `{{...}}` 文本。如果存在残留，请检查名称是否拼写一致，或模型返回内容是否缺失。
6. **AI 输出格式**
   - AI 只需返回一个 JSON 对象，键名与占位符一致：

```json
{
  "cover_title": "故宫：中华文明的皇家殿堂",
  "cover_subtitle": "走进六百年的古都记忆",
  "outline_bullets": [
     "历史沿革与建造背景",
     "建筑格局与文化价值",
     "典藏文物与艺术成就"
  ]
}
```

7. **兼容模式说明**
   - 若模板中没有 `{{...}}` 占位符，或模型返回的 JSON 无法解析，系统会回退到旧的“布局推断”方案，只能按默认布局输出，排版效果可能不稳定。
   - 建议所有正式模板都按照占位符规则制作，以保证最终排版与设计稿一致。

## 注意事项

1. **AI服务配置**: 需要配置有效的OpenAI API Key
2. **文件存储**: 需要配置文件上传路径
3. **Redis缓存**: 确保Redis服务正常运行
4. **数据库权限**: 确保数据库用户有足够权限

## 后续扩展

- [ ] 视频生成功能完善
- [ ] PPT生成功能实现
- [ ] 支付系统集成
- [ ] 文件存储优化
- [ ] 监控和日志完善
- [ ] Docker容器化部署

## 联系方式

如有问题，请联系开发团队。