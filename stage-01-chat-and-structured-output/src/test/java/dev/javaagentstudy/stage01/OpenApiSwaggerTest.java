package dev.javaagentstudy.stage01;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.javaagentstudy.common.testing.FakeChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 验证阶段一的 OpenAPI 3 描述文档与 Swagger UI 页面可以离线正常访问。
 */
@SpringBootTest(classes = {Stage01Application.class, OpenApiSwaggerTest.FakeModelConfig.class})
@AutoConfigureMockMvc
class OpenApiSwaggerTest {

    @TestConfiguration
    static class FakeModelConfig {

        @Bean
        @Primary
        FakeChatModel fakeChatModel() {
            return FakeChatModel.replyingWith("占位回复");
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void exposesOpenApiJsonWithStage01EndpointsAndSchemas() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.openapi", startsWith("3.")))
                .andExpect(jsonPath("$.info.title").value("JavaAgentStudy - Stage 01 API"))
                .andExpect(jsonPath("$.paths['/api/explanations'].post.summary").value("同步生成概念讲解"))
                .andExpect(jsonPath("$.paths['/api/explanations/stream'].post.summary").value("SSE 流式生成概念讲解"))
                .andExpect(jsonPath("$.paths['/api/exercises'].post.summary").value("生成结构化 Exercise"))
                .andExpect(jsonPath("$.components.schemas.ExplanationRequest").exists())
                .andExpect(jsonPath("$.components.schemas.ExplanationResponse").exists())
                .andExpect(jsonPath("$.components.schemas.ExerciseRequest").exists())
                .andExpect(jsonPath("$.components.schemas.ApiError").exists());
    }

    @Test
    void servesSwaggerUiEntryAndIndexPage() throws Exception {
        // springdoc 默认将 /swagger-ui.html 重定向到 /swagger-ui/index.html。
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("/swagger-ui/index.html")));

        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("Swagger UI")));
    }
}
