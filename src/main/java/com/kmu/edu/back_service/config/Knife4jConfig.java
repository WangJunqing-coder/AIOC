package com.kmu.edu.back_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j API文档配置
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("🤖 AI综合平台 API文档")
                        .description("智能聊天、图片生成、视频生成一站式AI服务平台\n\n" +
                                    "## 功能模块\n" +
                                    "- 🔐 **认证授权**: 用户注册、登录、权限管理\n" +
                                    "- 👤 **用户管理**: 个人信息、账户充值\n" +
                                    "- 💬 **AI聊天**: 智能对话、多轮会话\n" +
                                    "- 🎨 **图片生成**: AI绘画、图像创作\n" +
                                    "- 🎬 **视频生成**: AI视频制作\n" +
                                    "- 🔧 **系统工具**: 健康检查、系统监控\n\n" +
                                    "## 技术栈\n" +
                                    "- **后端框架**: Spring Boot 3.4.10\n" +
                                    "- **认证框架**: Sa-Token 1.38.0\n" +
                                    "- **AI服务**: 硅基流动 (Qwen系列模型)\n" +
                                    "- **数据库**: MySQL + MyBatis\n" +
                                    "- **缓存**: Redis")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("AI综合平台开发团队")
                                .email("support@ai-platform.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .components(new Components()
                        .addSecuritySchemes("Bearer", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("请在此输入您的访问令牌(Token)")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer"));
    }
}