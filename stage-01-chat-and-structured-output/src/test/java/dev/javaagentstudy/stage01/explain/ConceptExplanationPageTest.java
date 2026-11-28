package dev.javaagentstudy.stage01.explain;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.javaagentstudy.common.testing.FakeChatModel;
import dev.javaagentstudy.stage01.Stage01Application;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 验证静态调试页面可随应用正常提供访问，且支持从本地 IDE 预览端口（如 IntelliJ 63342）联调接口。
 */
@SpringBootTest(classes = {Stage01Application.class, ConceptExplanationPageTest.FakeModelConfig.class})
@AutoConfigureMockMvc
class ConceptExplanationPageTest {

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
    void servesVerificationHtmlPageAtIndex() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("/api/explanations")))
                .andExpect(content().string(containsString("/api/explanations/stream")))
                // 验证页面支持自定义后端地址，并在检测到 IDE 预览（如 63342 / _ijt）时回退到 http://localhost:8080
                .andExpect(content().string(containsString("baseUrlInput")))
                .andExpect(content().string(containsString("http://localhost:8080")));
    }

    @Test
    void allowsCorsPreflightFromLocalIdePreviewOrigin() throws Exception {
        // 回归测试：从 IntelliJ 内置预览服务器（http://localhost:63342）跨源调用 SSE 接口时，
        // 浏览器会先发 OPTIONS 预检请求，后端必须返回允许该本机源的 CORS 响应头。
        mockMvc.perform(options("/api/explanations/stream")
                        .header(HttpHeaders.ORIGIN, "http://localhost:63342")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type,accept"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:63342"));
    }

    @Test
    void allowsCorsValidationErrorResponseFromLocalIdePreviewOrigin() throws Exception {
        // 验证跨源触发 400 校验失败时，响应同样带上 CORS 头，浏览器才能读取到 ApiError JSON。
        mockMvc.perform(post("/api/explanations")
                        .header(HttpHeaders.ORIGIN, "http://localhost:63342")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"topic\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:63342"));
    }

    @Test
    void allowsCorsPreflightForExercisesEndpointFromLocalIdePreviewOrigin() throws Exception {
        // 验证统一 LocalCorsConfig 对 /api/exercises 同样生效，避免两个接口的跨源行为不一致。
        mockMvc.perform(options("/api/exercises")
                        .header(HttpHeaders.ORIGIN, "http://localhost:63342")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:63342"));
    }
}
