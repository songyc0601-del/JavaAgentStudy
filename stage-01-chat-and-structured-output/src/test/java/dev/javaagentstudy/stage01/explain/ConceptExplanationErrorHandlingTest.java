package dev.javaagentstudy.stage01.explain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.javaagentstudy.common.testing.FakeChatModel;
import dev.javaagentstudy.stage01.Stage01Application;
import org.junit.jupiter.api.BeforeEach;
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
 * 错误处理的离线测试（接缝 1：HTTP 入口）。
 *
 * <p>覆盖两类错误：
 * 1. 非法入参：返回 400，并且不能调用模型（避免浪费 Token）；
 * 2. 下游模型异常：返回清晰、可诊断的错误响应。
 *
 * <p>这里的假模型被配置为“调用就抛异常”：
 * 如果 400 场景里模型被调用了，测试会因记录数不为 0 而失败。
 */
@SpringBootTest(classes = {Stage01Application.class, ConceptExplanationErrorHandlingTest.FailingModelConfig.class})
@AutoConfigureMockMvc
class ConceptExplanationErrorHandlingTest {

    /** 测试专用配置：用一个必定失败的假模型顶替真实大模型。 */
    @TestConfiguration
    static class FailingModelConfig {

        @Bean
        @Primary
        FakeChatModel fakeChatModel() {
            return FakeChatModel.failingWith(new IllegalStateException("model is down"));
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FakeChatModel fakeChatModel;

    // Spring 会在同一个测试类的多个测试之间复用同一个假模型，
    // 所以每个测试前先清空记录，保证“模型没有被调用”的断言只看当前测试。
    @BeforeEach
    void resetRecordedPrompts() {
        fakeChatModel.clearRecordedPrompts();
    }

    @Test
    void blankTopicReturns400WithoutCallingModel() throws Exception {
        mockMvc.perform(post("/api/explanations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"topic\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details").isNotEmpty());

        assertThat(fakeChatModel.recordedPrompts()).isEmpty();
    }

    @Test
    void missingTopicReturns400WithoutCallingModel() throws Exception {
        mockMvc.perform(post("/api/explanations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"focus\":\"泛型\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));

        assertThat(fakeChatModel.recordedPrompts()).isEmpty();
    }

    @Test
    void modelFailureReturnsDiagnosticErrorResponse() throws Exception {
        mockMvc.perform(post("/api/explanations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"topic\":\"集合框架\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("MODEL_CALL_FAILED"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void malformedJsonReturns400WithoutCallingModel() throws Exception {
        // 请求体不是合法 JSON：这是调用方的错误，应返回 400，而不是被当成“模型故障”返回 502。
        mockMvc.perform(post("/api/explanations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST"));

        assertThat(fakeChatModel.recordedPrompts()).isEmpty();
    }

    @Test
    void modelFailureDoesNotLeakUpstreamMessageToClient() throws Exception {
        // 上游异常文本（这里是 "model is down"）可能包含内部细节，不应该原样返回给调用方。
        String body = mockMvc.perform(post("/api/explanations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"topic\":\"集合框架\"}"))
                .andExpect(status().isBadGateway())
                .andReturn()
                .getResponse()
                .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);

        assertThat(body).doesNotContain("model is down");
    }
}
