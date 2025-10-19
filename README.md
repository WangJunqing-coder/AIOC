语言/Language: [中文](README.zh-CN.md) | [English](README.en.md)

## AIOC 后端服务

本项目是一个基于Spring Boot 3 + Vue3的AI综合服务平台，集成了AI聊天、图片生成、视频生成和AI PPT制作以及会员充值等功能。
* 后端源码：[https://github.com/WangJunqing-coder/AIOC](https://github.com/WangJunqing-coder/AIOC)
* 前端源码：[https://github.com/WangJunqing-coder/AIOC-front](https://github.com/WangJunqing-coder/AIOC-front)

## 技术栈与特性

- Spring Boot 3.4.10（WebMVC + WebClient）
- Java 20+（POM 为 20，建议使用 JDK 20 或 21）
- MySQL 8 + MyBatis（含 PageHelper 分页）
- Redis + Sa-Token（会话/令牌）
- LangChain4j + SiliconFlow（Qwen/Wan 等模型接入）
- SpringDoc OpenAPI + Knife4j（在线 API 文档）
- MinIO（对象存储，选配）
- Hutool、Lombok、Apache POI（PPTX）

## 环境要求

- JDK 20+（推荐 21）
- Maven 3.8+
- MySQL 8.0+
- Redis 6+
- MinIO（可选，用于文件/视频等对象存储）

## 快速开始（Windows cmd）

1) 创建数据库并导入初始化脚本（默认库名 ai_platform）

```bat
REM 打开 MySQL 后执行（或使用可视化工具）
CREATE DATABASE ai_platform DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

REM 在仓库根目录下执行导入（按需修改账号/路径）
mysql -h127.0.0.1 -P3306 -uroot -p ai_platform < back_service\src\main\resources\sql\init.sql
```

2) 配置本地敏感信息（强烈建议使用本地配置覆盖，而不是修改仓库中的 `application.yml`）

- 方式 A：在 `back_service/src/main/resources/` 新增 `application-local.yml`，示例：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ai_platform?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: 你的数据库密码
  data:
    redis:
      host: localhost
      port: 6379
      password: ""

ai:
  silicon:
    api-key: sk-你的SiliconFlowKey
    base-url: https://api.siliconflow.cn/v1

storage:
  minio:
    endpoint: http://localhost:9000
    access-key: admin
    secret-key: admin123456
    bucket: test
```

- 方式 B：使用环境变量（Spring Boot 放宽绑定，`ai.silicon.api-key` 可写成 `AI_SILICON_API_KEY`）：

```bat
set AI_SILICON_API_KEY=sk-xxx_your_key
set SPRING_DATASOURCE_USERNAME=root
set SPRING_DATASOURCE_PASSWORD=你的数据库密码
set SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/ai_platform?useUnicode=true^&characterEncoding=utf8^&useSSL=false^&serverTimezone=Asia/Shanghai^&allowPublicKeyRetrieval=true
```

3) 启动服务（默认端口 8080）

```bat
mvn -q -DskipTests spring-boot:run
```

或构建可执行 JAR 后运行：

```bat
mvn -q -DskipTests package
java -jar target\back_service-0.0.1-SNAPSHOT.jar
```



## 重要配置说明（摘自 `application.yml`）

- 端口：`server.port: 8080`
- 数据库：`spring.datasource.*`（默认本地 `ai_platform`，账号/密码请按需覆盖）
- Redis：`spring.data.redis.*`（默认本地 6379）
- 登录鉴权：`sa-token.*`（请求头使用 `Authorization: Bearer <token>`）
- AI 模型：`ai.silicon.*`（含 api-key、模型名、超时等）
- 对象存储：`storage.minio.*`（endpoint、access/secret、bucket、public-base-url）
- 上传目录：`file.upload.path`（Windows 推荐类似 `D:/uploads/` 或 `C:\\uploads\\`）

强烈建议：不要把真实的 API Key、数据库密码等敏感信息提交到仓库，使用环境变量或 `application-local.yml` 覆盖。

## 常用命令

```bat
REM 运行（跳过测试）
mvn -DskipTests spring-boot:run

REM 打包（跳过测试）
mvn -DskipTests package

REM 运行打包产物
java -jar target\back_service-0.0.1-SNAPSHOT.jar
```

## 常见问题（FAQ）

- 端口占用：修改 `application.yml` 的 `server.port`，或释放占用 8080 的进程。
- 401 未认证：前端需在请求头携带 `Authorization: Bearer <token>`；登录成功后的 token 需正确保存。
- CORS 跨域：本项目已配置 CORS；如仍有问题，检查前端代理 `vite.config.js` 与后端 `CorsConfig`。
- MySQL 时区/SSL：连接串已包含 `serverTimezone=Asia/Shanghai`、`useSSL=false`；如云数据库需按需调整。
- Redis 鉴权：如本地 Redis 有密码，请在本地覆盖 `spring.data.redis.password`。
- Windows 上传路径：`file.upload.path` 推荐使用正斜杠或转义反斜杠，示例 `D:/uploads/` 或 `C:\\uploads\\`。
- 视频生成超时：可按需提高 `ai.silicon.video-timeout-seconds` 或轮询间隔。

## 项目结构（简）

```
back_service/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/com/kmu/edu/back_service/
│   │   │   ├── common/
│   │   │   ├── config/
│   │   │   ├── constant/
│   │   │   ├── dto/
│   │   │   ├── entity/
│   │   │   ├── exception/
│   │   │   ├── mapper/
│   │   │   └── service/
│   │   └── resources/
│   │       ├── application.yml
│   │       └── sql/init.sql
│   └── test/
└── README.md
```

—— 如需 Docker/部署脚本、更多 API 示例或领域说明，可在后续版本补充。
