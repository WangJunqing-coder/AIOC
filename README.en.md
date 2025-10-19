Language: [中文](README.zh-CN.md) | [English](README.en.md)

## AIOC Backend Service

This is an AI platform based on Spring Boot 3 and Vue 3, integrating AI Chat, Image Generation, Video Generation, AI PPT creation, and membership top-up features.

- Backend repo: https://github.com/WangJunqing-coder/AIOC
- Frontend repo: https://github.com/WangJunqing-coder/AIOC-front

## Tech Stack & Features

- Spring Boot 3.4.10 (WebMVC + WebClient)
- Java 20+ (POM is 20; JDK 20 or 21 recommended)
- MySQL 8 + MyBatis (with PageHelper pagination)
- Redis + Sa-Token (session/token)
- LangChain4j + SiliconFlow (Qwen/Wan and other models)
- SpringDoc OpenAPI + Knife4j (online API docs)
- MinIO (object storage, optional)
- Hutool, Lombok, Apache POI (PPTX)

## Prerequisites

- JDK 20+ (21 recommended)
- Maven 3.8+
- MySQL 8.0+
- Redis 6+
- MinIO (optional, for object storage)

## Quick Start (Windows cmd)

1) Create database and import init script (default DB: ai_platform)

```bat
REM Run in MySQL (or use a GUI)
CREATE DATABASE ai_platform DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

REM Run at repo root (adjust credentials/path as needed)
mysql -h127.0.0.1 -P3306 -uroot -p ai_platform < back_service\src\main\resources\sql\init.sql
```

2) Configure local secrets (prefer local overrides rather than editing `application.yml` in VCS)

- Option A: Add `application-local.yml` under `back_service/src/main/resources/`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ai_platform?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: your_db_password
  data:
    redis:
      host: localhost
      port: 6379
      password: ""

ai:
  silicon:
    api-key: sk-yourSiliconFlowKey
    base-url: https://api.siliconflow.cn/v1

storage:
  minio:
    endpoint: http://localhost:9000
    access-key: admin
    secret-key: admin123456
    bucket: test
```

- Option B: Use environment variables (relaxed binding; `ai.silicon.api-key` -> `AI_SILICON_API_KEY`):

```bat
set AI_SILICON_API_KEY=sk-xxx_your_key
set SPRING_DATASOURCE_USERNAME=root
set SPRING_DATASOURCE_PASSWORD=your_db_password
set SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/ai_platform?useUnicode=true^&characterEncoding=utf8^&useSSL=false^&serverTimezone=Asia/Shanghai^&allowPublicKeyRetrieval=true
```

3) Run the service (default port 8080)

```bat
mvn -q -DskipTests spring-boot:run
```

Or build a runnable jar:

```bat
mvn -q -DskipTests package
java -jar target\back_service-0.0.1-SNAPSHOT.jar
```

## Important Config (from `application.yml`)

- Port: `server.port: 8080`
- DB: `spring.datasource.*` (default local `ai_platform`; override user/pass as needed)
- Redis: `spring.data.redis.*` (default 6379)
- Auth: `sa-token.*` (request header `Authorization: Bearer <token>`)
- AI Models: `ai.silicon.*` (api-key, model, timeout, etc.)
- Object Storage: `storage.minio.*` (endpoint, access/secret, bucket, public-base-url)
- Upload dir: `file.upload.path` (Windows example `D:/uploads/` or `C:\\uploads\\`)

Recommendation: never commit real API keys or secrets; use environment variables or `application-local.yml`.

## Common Commands

```bat
REM Run (skip tests)
mvn -DskipTests spring-boot:run

REM Package (skip tests)
mvn -DskipTests package

REM Run packaged artifact
java -jar target\back_service-0.0.1-SNAPSHOT.jar
```

## FAQ

- Port in use: change `server.port` or free 8080.
- 401 Unauthorized: ensure front-end adds `Authorization: Bearer <token>`; persist token after login.
- CORS: CORS is configured; also check `vite.config.js` proxy and backend `CorsConfig`.
- MySQL timezone/SSL: connection string includes `serverTimezone=Asia/Shanghai`, `useSSL=false`; adjust for cloud DB as needed.
- Redis auth: if Redis has a password, override `spring.data.redis.password` locally.
- Windows upload path: prefer forward slashes or escaped backslashes, e.g., `D:/uploads/` or `C:\\uploads\\`.
- Video generation timeout: increase `ai.silicon.video-timeout-seconds` or polling interval as needed.

## Project Structure (brief)

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

— For Docker/deployment scripts and more API examples, future versions may add them.
