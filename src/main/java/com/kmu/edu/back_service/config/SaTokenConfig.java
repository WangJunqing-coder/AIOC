package com.kmu.edu.back_service.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;

/**
 * Sa-Token配置
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    /**
     * 兼容前端 Authorization: Bearer <token>
     */
    @Configuration
    public static class BearerTokenFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {
            String auth = request.getHeader("Authorization");
            if (StringUtils.hasText(auth) && auth.startsWith("Bearer ")) {
                // Sa-Token 默认也使用 Authorization 头，通常可直接识别 Bearer 模式，这里无需额外处理。
            }
            filterChain.doFilter(request, response);
        }
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册Sa-Token拦截器，打开注解式鉴权功能
        // 包一层，跳过 OPTIONS 预检
        HandlerInterceptor interceptor = new SaInterceptor(handle -> StpUtil.checkLogin()) {
            @Override
            public boolean preHandle(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, Object handler) throws Exception {
                if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
                    return true;
                }
                return super.preHandle(request, response, handler);
            }
        };
        registry.addInterceptor(interceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
            // 预检请求直接放行
            "/**/*.OPTIONS",
                        // 排除认证相关接口
                        "/api/auth/login",
                        "/api/auth/register",
                        "/api/auth/forgot-password",
                        "/api/auth/reset-password",
                        // 允许图片代理无需登录（内部限制了仅可代理到 MinIO 域名）
                        "/api/proxy",
                        // 排除测试接口
                        "/api/test/**",
                        // 排除API文档相关接口
                        // 排除Knife4j文档接口
                        "/webjars/**",
                        "/swagger-resources/**",
                        "/v3/api-docs/**",
                        "/v3/api-docs.yaml",
                        // 排除SpringDoc相关路径
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/openapi/**",
                        "/api-docs/**",
                        "/favicon.ico",
                        "/error",
                        // 排除静态资源
                        "/static/**",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/files/**",
                        // 排除系统路径
                        "/actuator/**",
                        "/hybridaction/**",
                        // 排除根路径
                        "/",
                        "/index.html"
                );
    }
}