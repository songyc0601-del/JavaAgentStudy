package dev.javaagentstudy.stage01.explain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.javaagentstudy.common.testing.FakeChatModel;
import dev.javaagentstudy.common.testing.SwitchableChatModel;
import dev.javaagentstudy.stage01.Stage01Application;
import dev.javaagentstudy.stage01.Stage01TestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * SSE 流式讲解入参校验的离线测试（接缝 1：HTTP 入口）。
 *
 * <p>非法入参必须在进入 Controller 方法前被拦截，返回 400，
 * 并且不能触发模型的流式调用（避免浪费 Token）。
 */
@SpringBootTest(classes = {Stage01Application.class, Stage01TestConfig.class})
@AutoConfigureMockMvc
class ConceptExplanationStreamValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SwitchableChatModel fakeChatModel;

    @BeforeEach
    void resetRecordedPrompts() {
        fakeChatModel.use(FakeChatModel.streamingChunks("集合", "框架"));
    }

    @Test
    void blankTopicReturns400WithoutCallingModel() throws Exception {
        // 客户端按 SSE 习惯带上 Accept: text/event-stream，错误响应仍应是 JSON 的 400。
        mockMvc.perform(post("/api/explanations/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content("{\"topic\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));

        assertThat(fakeChatModel.recordedPrompts()).isEmpty();
    }

    @Test
    void missingTopicReturns400WithoutCallingModel() throws Exception {
        mockMvc.perform(post("/api/explanations/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content("{\"focus\":\"泛型\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));

        assertThat(fakeChatModel.recordedPrompts()).isEmpty();
    }
}
