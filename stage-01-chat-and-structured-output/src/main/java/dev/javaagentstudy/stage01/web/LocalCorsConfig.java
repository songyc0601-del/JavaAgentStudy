package dev.javaagentstudy.stage01.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 统一放行本机回环端口跨源访问 /api/**。
 *
 * <p>方便 Learner 直接通过 IntelliJ 内置静态预览服务器（如 localhost:63342）打开调试页联调后端接口，
 * 同时让 /api/explanations 与 /api/exercises 保持一致的 CORS 行为，不向外部主机开放。
 */
@Configuration
public class LocalCorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*")
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("*");
    }
}
