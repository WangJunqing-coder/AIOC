package com.kmu.edu.back_service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置类 - 处理静态资源映射
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 保留文件上传的静态资源映射
        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:/data/uploads/");
        
        // 为Knife4j和SpringDoc添加静态资源映射
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
                
        registry.addResourceHandler("/swagger-ui/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/swagger-ui/");
                
        // 明确设置不处理favicon.ico和hybridaction/**
        // 这样这些请求就会被控制器处理而不是静态资源处理器
    }
}