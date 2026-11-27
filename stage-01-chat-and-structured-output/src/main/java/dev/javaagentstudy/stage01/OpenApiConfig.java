package dev.javaagentstudy.stage01;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 阶段一的 OpenAPI / Swagger 文档基础元信息配置。
 *
 * <p>具体的接口路径、请求体与响应结构由 springdoc 自动扫描 Controller 与 record 生成，
 * 这里只提供 Swagger UI 顶部展示的标题、说明与版本号。
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI stage01OpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("JavaAgentStudy - Stage 01 API")
                        .description("面向初级 Java Learner 的概念讲解（同步 / SSE 流式）与结构化 Exercise 生成接口。")
                        .version("0.0.1-SNAPSHOT"));
    }
}
